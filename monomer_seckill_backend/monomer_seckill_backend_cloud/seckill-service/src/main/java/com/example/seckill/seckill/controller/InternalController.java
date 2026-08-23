package com.example.seckill.seckill.controller;

import com.example.seckill.common.core.Result;
import com.example.seckill.seckill.dto.SeckillApplyRequest;
import com.example.seckill.seckill.entity.SeckillGoods;
import com.example.seckill.seckill.entity.SeckillOrder;
import com.example.seckill.seckill.service.SeckillGoodsManageService;
import com.example.seckill.seckill.service.SeckillOrderService;
import com.example.seckill.seckill.service.StockService;
import com.example.seckill.seckill.vo.SeckillGoodsVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 秒杀内部接口（供 user-service 通过 OpenFeign 调用：管理员审核秒杀商品/查看秒杀订单/重置库存、商家管理秒杀商品）。
 *
 * @author haiy
 * @date 2026/08/17
 */
@RestController
@RequestMapping("/internal")
public class InternalController {

    private final SeckillGoodsManageService seckillGoodsManageService;
    private final SeckillOrderService seckillOrderService;
    private final StockService stockService;

    public InternalController(SeckillGoodsManageService seckillGoodsManageService,
                              SeckillOrderService seckillOrderService, StockService stockService) {
        this.seckillGoodsManageService = seckillGoodsManageService;
        this.seckillOrderService = seckillOrderService;
        this.stockService = stockService;
    }

    // ==================== 管理员：秒杀商品审核 ====================

    @GetMapping("/admin/seckill-goods")
    public Result<List<SeckillGoodsVO>> listSeckillGoods() {
        return Result.ok(seckillGoodsManageService.listAllSeckillGoods());
    }

    @PostMapping("/admin/seckill-goods/{id}/approve")
    public Result<Void> approveSeckillGoods(@PathVariable Long id) {
        seckillGoodsManageService.approveSeckillGoods(id);
        return Result.ok();
    }

    @PostMapping("/admin/seckill-goods/{id}/reject")
    public Result<Void> rejectSeckillGoods(@PathVariable Long id) {
        seckillGoodsManageService.rejectSeckillGoods(id);
        return Result.ok();
    }

    // ==================== 管理员：秒杀订单 / 库存 ====================

    @GetMapping("/admin/seckill-orders")
    public Result<List<SeckillOrder>> listSeckillOrders() {
        return Result.ok(seckillOrderService.listAllSeckillOrders());
    }

    @PostMapping("/admin/stock/reset")
    public Result<Void> resetStock() {
        stockService.preloadAll();
        return Result.ok();
    }

    @PostMapping("/admin/stock/reset/{seckillGoodsId}")
    public Result<Void> resetStockOne(@PathVariable Long seckillGoodsId) {
        stockService.preload(seckillGoodsId);
        return Result.ok();
    }

    // ==================== 商家：秒杀商品管理 ====================

    @GetMapping("/merchant/{merchantId}/seckill-goods")
    public Result<List<SeckillGoods>> listMerchantSeckillGoods(@PathVariable Long merchantId) {
        return Result.ok(seckillGoodsManageService.listMerchantSeckillGoods(merchantId));
    }

    @PostMapping("/merchant/{merchantId}/seckill-goods")
    public Result<SeckillGoods> applySeckill(@PathVariable Long merchantId,
                                             @RequestBody SeckillApplyRequest request) {
        return Result.ok(seckillGoodsManageService.applySeckill(merchantId, request));
    }

    @PutMapping("/merchant/{merchantId}/seckill-goods/{id}")
    public Result<SeckillGoods> updateSeckillGoods(@PathVariable Long merchantId, @PathVariable Long id,
                                                   @RequestBody SeckillApplyRequest request) {
        return Result.ok(seckillGoodsManageService.updateSeckillGoods(merchantId, id, request));
    }
}
