package com.example.seckill.seckill.mq;

import com.example.seckill.seckill.constant.SeckillMqConstants;
import com.example.seckill.seckill.service.SeckillOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 秒杀订单超时消费者（P8 死信队列）：延迟队列消息 TTL 过期后投递到这里，执行超时关闭。
 *
 * <p>「下单后 15 分钟未支付自动取消」由死信队列触发，无需定时任务扫描数据库：下单时发送一条
 * 带 TTL 的延时消息，过期后作为死信进入本队列，此时校验订单若仍为「待支付」则关闭并回补库存，
 * 否则（已支付/已取消）幂等跳过。</p>
 *
 * @author haiy
 * @date 2026/08/27
 */
@Slf4j
@Component
public class SeckillOrderTimeoutListener {

    private final SeckillOrderService seckillOrderService;

    public SeckillOrderTimeoutListener(SeckillOrderService seckillOrderService) {
        this.seckillOrderService = seckillOrderService;
    }

    /**
     * 消费超时消息：关闭超时未支付订单并回补库存（幂等）。
     */
    @RabbitListener(queues = SeckillMqConstants.ORDER_TIMEOUT_QUEUE)
    public void onTimeout(SeckillOrderTimeoutMessage message) {
        seckillOrderService.closeTimeoutByOrderNo(message.getOrderNo());
        log.info("[秒杀超时] 订单 {} 超时处理完成（若仍待支付则已关闭并回补库存）", message.getOrderNo());
    }
}
