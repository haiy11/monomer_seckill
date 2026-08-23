package com.example.seckill.goodsorder.controller;

import com.example.seckill.common.core.Result;
import com.example.seckill.goodsorder.entity.Goods;
import com.example.seckill.goodsorder.entity.MallOrder;
import com.example.seckill.goodsorder.service.GoodsManageService;
import com.example.seckill.goodsorder.service.OrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商品/订单内部接口（供 user-service 通过 OpenFeign 调用：管理员审核商品/查看订单、商家管理商品）。
 *
 * @author haiy
 * @date 2026/08/17
 */
@RestController
@RequestMapping("/internal")
public class InternalController {

    private final GoodsManageService goodsManageService;
    private final OrderService orderService;

    public InternalController(GoodsManageService goodsManageService, OrderService orderService) {
        this.goodsManageService = goodsManageService;
        this.orderService = orderService;
    }

    // ==================== 管理员：商品审核 ====================

    @GetMapping("/admin/goods")
    public Result<List<Goods>> listGoods() {
        return Result.ok(goodsManageService.listAllGoods());
    }

    @PostMapping("/admin/goods/{id}/approve")
    public Result<Void> approveGoods(@PathVariable Long id) {
        goodsManageService.approveGoods(id);
        return Result.ok();
    }

    @PostMapping("/admin/goods/{id}/reject")
    public Result<Void> rejectGoods(@PathVariable Long id) {
        goodsManageService.rejectGoods(id);
        return Result.ok();
    }

    // ==================== 管理员：订单 ====================

    @GetMapping("/admin/orders")
    public Result<List<MallOrder>> listOrders() {
        return Result.ok(orderService.listAllNormal());
    }

    // ==================== 商家：商品管理 ====================

    @GetMapping("/merchant/{merchantId}/goods")
    public Result<List<Goods>> listMerchantGoods(@PathVariable Long merchantId) {
        return Result.ok(goodsManageService.listMerchantGoods(merchantId));
    }

    @PostMapping("/merchant/{merchantId}/goods")
    public Result<Goods> addGoods(@PathVariable Long merchantId, @RequestBody Goods goods) {
        return Result.ok(goodsManageService.addGoods(merchantId, goods));
    }

    @PutMapping("/merchant/{merchantId}/goods/{goodsId}")
    public Result<Goods> updateGoods(@PathVariable Long merchantId, @PathVariable Long goodsId,
                                     @RequestBody Goods goods) {
        return Result.ok(goodsManageService.updateGoods(merchantId, goodsId, goods));
    }

    @PutMapping("/merchant/{merchantId}/goods/{goodsId}/status")
    public Result<Void> updateGoodsStatus(@PathVariable Long merchantId, @PathVariable Long goodsId,
                                          @RequestParam Integer status) {
        goodsManageService.updateGoodsStatus(merchantId, goodsId, status);
        return Result.ok();
    }
}
