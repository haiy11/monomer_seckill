package com.example.seckill.seckill.controller;

import com.example.seckill.common.core.Result;
import com.example.seckill.seckill.service.StockService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 秒杀库存内部接口（供 goods-order-service 在取消/超时关闭秒杀订单时回滚 Redis 预扣库存）。
 *
 * @author haiy
 * @date 2026/08/17
 */
@RestController
@RequestMapping("/internal/stock")
public class InternalStockController {

    private final StockService stockService;

    public InternalStockController(StockService stockService) {
        this.stockService = stockService;
    }

    /**
     * 回滚 Redis 预扣库存（幂等）。
     */
    @PostMapping("/rollback-redis/{seckillGoodsId}")
    public Result<Void> rollbackRedis(@PathVariable Long seckillGoodsId, @RequestParam Long userId) {
        stockService.rollbackRedis(seckillGoodsId, userId);
        return Result.ok();
    }
}
