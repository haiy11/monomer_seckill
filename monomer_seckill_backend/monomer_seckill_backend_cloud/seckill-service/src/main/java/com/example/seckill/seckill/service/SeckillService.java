package com.example.seckill.seckill.service;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowException;
import com.example.seckill.common.core.Result;
import com.example.seckill.seckill.constant.SeckillConstants;
import com.example.seckill.seckill.entity.SeckillGoods;
import com.example.seckill.seckill.mq.SeckillMqProducer;
import com.example.seckill.seckill.mq.SeckillOrderMessage;
import org.springframework.stereotype.Service;

/**
 * 秒杀核心服务（P8 异步削峰）：校验可购买 → Redis 预扣库存 → 发送 MQ 消息 → 立即返回。
 *
 * <p>P8 起，请求线程不再同步执行「DB 扣库存 + 落订单」，改为 Redis Lua 预扣成功后发送消息、
 * 立即返回订单号；DB 落库由 {@code SeckillOrderCreateListener} 异步完成。这样把瞬时高并发
 * 请求「削」成 MQ 消费者可承受的匀速写入，同时解耦了请求链路与数据库写链路。</p>
 *
 * @author haiy
 * @date 2026/08/17
 */
@Service
public class SeckillService {

    private final SeckillGoodsService seckillGoodsService;
    private final StockService stockService;
    private final SeckillOrderService seckillOrderService;
    private final SeckillMqProducer seckillMqProducer;

    public SeckillService(SeckillGoodsService seckillGoodsService, StockService stockService,
                          SeckillOrderService seckillOrderService, SeckillMqProducer seckillMqProducer) {
        this.seckillGoodsService = seckillGoodsService;
        this.stockService = stockService;
        this.seckillOrderService = seckillOrderService;
        this.seckillMqProducer = seckillMqProducer;
    }

    /**
     * 秒杀下单（需登录，userId 来自 token）。
     *
     * @param seckillGoodsId 秒杀商品ID
     * @param userId         用户ID
     * @return 成功携带订单号（订单由消息消费者异步落库）
     */
    @SentinelResource(value = "seckill", blockHandler = "seckillBlockHandler")
    public Result<String> seckill(Long seckillGoodsId, Long userId) {
        if (seckillGoodsId == null || userId == null) {
            return Result.fail("参数错误");
        }

        // 1. 校验秒杀商品存在、已上架且在时间窗口内（不满足抛业务异常）
        SeckillGoods sg = seckillGoodsService.requirePurchasable(seckillGoodsId);

        // 2. Redis 原子预扣库存（判库存 + 判重复 + 扣库存 + 记用户）
        long code = stockService.deduct(seckillGoodsId, userId);
        if (code == SeckillConstants.LUA_STOCK_EMPTY) {
            return Result.fail("手慢了，库存不足");
        }
        if (code == SeckillConstants.LUA_DUPLICATE) {
            return Result.fail("您已抢购过该秒杀商品");
        }

        // 3. 预生成订单号 + 发送下单消息（发布确认），失败则回滚 Redis 预扣
        String orderNo = seckillOrderService.generateOrderNo("SO");
        SeckillOrderMessage message = SeckillOrderMessage.of(orderNo, seckillGoodsId, userId, sg.getSeckillPrice());
        try {
            seckillMqProducer.sendCreateOrder(message);
        } catch (Exception e) {
            stockService.rollbackRedis(seckillGoodsId, userId);
            throw e;
        }
        return Result.ok(orderNo);
    }

    /**
     * 秒杀资源被限流/熔断时的兜底处理（Sentinel 框架回调，仅处理 BlockException）。
     *
     * <p>签名约定：与原方法参数一致，末尾追加 {@link BlockException}。业务异常不受影响，
     * 仍按原逻辑（内部捕获或 {@code GlobalExceptionHandler}）处理。</p>
     *
     * <ul>
     *   <li>熔断（DegradeException）→ 503，服务熔断中；</li>
     *   <li>热点参数限流（ParamFlowException）→ 429，该商品太火爆；</li>
     *   <li>其它限流（FlowException 等）→ 429，请求过于频繁。</li>
     * </ul>
     */
    public Result<String> seckillBlockHandler(Long seckillGoodsId, Long userId, BlockException ex) {
        if (ex instanceof DegradeException) {
            return Result.fail(503, "秒杀服务熔断中，请稍后再试");
        }
        if (ex instanceof ParamFlowException) {
            return Result.fail(429, "该商品太火爆，请稍后再试");
        }
        return Result.fail(429, "请求过于频繁，请稍后再试");
    }
}
