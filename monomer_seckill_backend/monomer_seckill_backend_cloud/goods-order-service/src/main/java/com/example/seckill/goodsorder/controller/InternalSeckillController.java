package com.example.seckill.goodsorder.controller;

import com.example.seckill.common.core.BizException;
import com.example.seckill.common.core.Result;
import com.example.seckill.common.entity.SeckillGoods;
import com.example.seckill.common.entity.SeckillOrder;
import com.example.seckill.common.service.SeckillGoodsService;
import com.example.seckill.common.service.StockService;
import com.example.seckill.goodsorder.service.OrderService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 秒杀内部接口（供 seckill-service 通过 OpenFeign 调用）。
 *
 * <p>返回统一 {@link Result}，避免把业务失败映射成 HTTP 5xx 后
 * 在 Feign 侧无法区分「库存不足/重复抢购」等语义。</p>
 *
 * @author haiy
 * @date 2026/08/17
 */
@RestController
@RequestMapping("/internal")
public class InternalSeckillController {

    private final SeckillGoodsService seckillGoodsService;
    private final StockService stockService;
    private final OrderService orderService;

    public InternalSeckillController(SeckillGoodsService seckillGoodsService,
                                     StockService stockService, OrderService orderService) {
        this.seckillGoodsService = seckillGoodsService;
        this.stockService = stockService;
        this.orderService = orderService;
    }

    /**
     * 校验秒杀商品可购买（存在、已上架、时间窗口内）。
     */
    @GetMapping("/seckill-goods/{id}/purchasable")
    public Result<SeckillGoods> purchasable(@PathVariable Long id) {
        try {
            return Result.ok(seckillGoodsService.requirePurchasable(id));
        } catch (BizException e) {
            return Result.fail(e.getCode(), e.getMessage());
        }
    }

    /**
     * Redis 原子预扣库存（Lua），返回 Lua 结果码。
     */
    @PostMapping("/stock/deduct/{seckillGoodsId}")
    public Result<Integer> deduct(@PathVariable Long seckillGoodsId, @RequestParam Long userId) {
        long code = stockService.deduct(seckillGoodsId, userId);
        return Result.ok((int) code);
    }

    /**
     * 创建秒杀订单（DB 扣减秒杀库存 + 插入订单），返回订单号。
     */
    @PostMapping("/seckill-order/create/{seckillGoodsId}")
    public Result<String> create(@PathVariable Long seckillGoodsId, @RequestParam Long userId) {
        try {
            SeckillGoods sg = seckillGoodsService.getByIdCached(seckillGoodsId);
            if (sg == null) {
                return Result.fail("秒杀商品不存在");
            }
            SeckillOrder order = orderService.createSeckillOrder(seckillGoodsId, userId, sg.getSeckillPrice());
            return Result.ok(order.getOrderNo());
        } catch (DuplicateKeyException e) {
            return Result.fail("您已抢购过该秒杀商品");
        } catch (BizException e) {
            return Result.fail(e.getCode(), e.getMessage());
        }
    }

    /**
     * 仅回滚 Redis 预扣库存（下单 DB 写入失败时由 seckill-service 调用）。
     */
    @PostMapping("/stock/rollback-redis/{seckillGoodsId}")
    public Result<Void> rollbackRedis(@PathVariable Long seckillGoodsId, @RequestParam Long userId) {
        stockService.rollbackRedis(seckillGoodsId, userId);
        return Result.ok();
    }
}
