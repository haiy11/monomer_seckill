package com.example.seckill.seckill.mq;

import com.example.seckill.common.core.BizException;
import com.example.seckill.seckill.constant.SeckillMqConstants;
import com.example.seckill.seckill.service.SeckillOrderService;
import com.example.seckill.seckill.service.StockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

/**
 * 秒杀下单消费者（P8）：异步执行「DB 扣库存 + 落订单」，实现削峰解耦。
 *
 * <p>秒杀请求在 Redis Lua 预扣成功后只发消息、立即返回；真正的数据库写入由本消费者以受控
 * 并发异步完成，把瞬时高并发「削」成后端可承受的匀速流量，同时解耦请求线程与落库过程。</p>
 *
 * <p>消费保证：</p>
 * <ul>
 *   <li><b>幂等</b>：以订单号判重，重复投递不重复建单；</li>
 *   <li><b>业务失败不重试</b>：库存不足/重复下单 → 回滚 Redis 预扣后确认消息；</li>
 *   <li><b>异常重试</b>：抛出运行时异常交由 RabbitMQ 重新投递（默认 requeue）。</li>
 * </ul>
 *
 * @author haiy
 * @date 2026/08/27
 */
@Slf4j
@Component
public class SeckillOrderCreateListener {

    private final SeckillOrderService seckillOrderService;
    private final StockService stockService;
    private final SeckillMqProducer seckillMqProducer;

    public SeckillOrderCreateListener(SeckillOrderService seckillOrderService,
                                      StockService stockService, SeckillMqProducer seckillMqProducer) {
        this.seckillOrderService = seckillOrderService;
        this.stockService = stockService;
        this.seckillMqProducer = seckillMqProducer;
    }

    /**
     * 消费下单消息：建单成功后投递「超时消息」，15 分钟后未支付自动关闭。
     */
    @RabbitListener(queues = SeckillMqConstants.ORDER_QUEUE)
    public void onCreate(SeckillOrderMessage message) {
        // 1. 幂等：订单已存在（消息重复投递），补发超时消息兜底后确认
        if (seckillOrderService.existsByOrderNo(message.getOrderNo())) {
            log.warn("[秒杀下单] 重复消费，订单已存在，跳过建单: orderNo={}", message.getOrderNo());
            seckillMqProducer.sendTimeoutMessage(toTimeoutMessage(message));
            return;
        }

        // 2. 异步建单（DB 扣库存 + 落订单）
        try {
            seckillOrderService.createSeckillOrder(message.getOrderNo(), message.getSeckillGoodsId(),
                    message.getUserId(), message.getSeckillPrice());
        } catch (DuplicateKeyException e) {
            // 同一用户已对该商品存在订单（唯一索引兜底），释放本次 Redis 预扣后确认
            stockService.rollbackRedis(message.getSeckillGoodsId(), message.getUserId());
            log.warn("[秒杀下单] 用户重复下单，已回滚 Redis 预扣: orderNo={}, userId={}, goodsId={}",
                    message.getOrderNo(), message.getUserId(), message.getSeckillGoodsId());
            return;
        } catch (BizException e) {
            // 业务失败（如 DB 库存不足）：释放 Redis 预扣，不再重试
            stockService.rollbackRedis(message.getSeckillGoodsId(), message.getUserId());
            log.warn("[秒杀下单] 建单失败，已回滚 Redis 预扣: orderNo={}, 原因={}", message.getOrderNo(), e.getMessage());
            return;
        }

        // 3. 建单成功 → 投递超时消息（TTL=订单超时分钟数），到期后由死信队列触发自动取消
        seckillMqProducer.sendTimeoutMessage(toTimeoutMessage(message));
        log.info("[秒杀下单] 订单异步创建成功: orderNo={}, userId={}, goodsId={}",
                message.getOrderNo(), message.getUserId(), message.getSeckillGoodsId());
    }

    private SeckillOrderTimeoutMessage toTimeoutMessage(SeckillOrderMessage message) {
        return new SeckillOrderTimeoutMessage(message.getOrderNo(), message.getSeckillGoodsId(), message.getUserId());
    }
}
