package com.example.seckill.seckill.service;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowException;
import com.example.seckill.common.core.BizException;
import com.example.seckill.common.core.Result;
import com.example.seckill.seckill.constant.SeckillConstants;
import com.example.seckill.seckill.entity.SeckillGoods;
import com.example.seckill.seckill.entity.SeckillOrder;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/**
 * 秒杀核心服务：校验可购买 → Redis 预扣库存 → 创建秒杀订单。
 *
 * <p>秒杀下单链路（商品校验、Redis 预扣、DB 扣减、订单落库）完全在秒杀域内闭环，
 * 高并发流量与其它服务隔离。</p>
 *
 * @author haiy
 * @date 2026/08/17
 */
@Service
public class SeckillService {

    private final SeckillGoodsService seckillGoodsService;
    private final StockService stockService;
    private final SeckillOrderService seckillOrderService;

    public SeckillService(SeckillGoodsService seckillGoodsService, StockService stockService,
                          SeckillOrderService seckillOrderService) {
        this.seckillGoodsService = seckillGoodsService;
        this.stockService = stockService;
        this.seckillOrderService = seckillOrderService;
    }

    /**
     * 秒杀下单（需登录，userId 来自 token）。
     *
     * @param seckillGoodsId 秒杀商品ID
     * @param userId         用户ID
     * @return 成功携带订单号
     */
    @SentinelResource(value = "seckill", blockHandler = "seckillBlockHandler")
    public Result<String> seckill(Long seckillGoodsId, Long userId) {
        if (seckillGoodsId == null || userId == null) {
            return Result.fail("参数错误");
        }

        // 1. 校验秒杀商品存在、已上架且在时间窗口内（不满足抛业务异常）
        SeckillGoods sg = seckillGoodsService.requirePurchasable(seckillGoodsId);

        // 2. Redis 原子预扣库存
        long code = stockService.deduct(seckillGoodsId, userId);
        if (code == SeckillConstants.LUA_STOCK_EMPTY) {
            return Result.fail("手慢了，库存不足");
        }
        if (code == SeckillConstants.LUA_DUPLICATE) {
            return Result.fail("您已抢购过该秒杀商品");
        }

        // 3. 创建秒杀订单（DB 扣库存 + 插订单），失败时回滚 Redis
        try {
            SeckillOrder order = seckillOrderService.createSeckillOrder(seckillGoodsId, userId, sg.getSeckillPrice());
            return Result.ok(order.getOrderNo());
        } catch (DuplicateKeyException e) {
            stockService.rollbackRedis(seckillGoodsId, userId);
            return Result.fail("您已抢购过该秒杀商品");
        } catch (BizException e) {
            stockService.rollbackRedis(seckillGoodsId, userId);
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            stockService.rollbackRedis(seckillGoodsId, userId);
            throw e;
        }
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
