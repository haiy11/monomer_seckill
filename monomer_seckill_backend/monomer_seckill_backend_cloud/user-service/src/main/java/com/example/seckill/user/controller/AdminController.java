package com.example.seckill.user.controller;

import com.example.seckill.common.core.Result;
import com.example.seckill.common.entity.Goods;
import com.example.seckill.common.entity.MallOrder;
import com.example.seckill.common.entity.SeckillOrder;
import com.example.seckill.common.vo.SeckillGoodsVO;
import com.example.seckill.user.dto.LoginRequest;
import com.example.seckill.user.service.AdminService;
import com.example.seckill.user.vo.LoginVO;
import com.example.seckill.user.vo.MerchantApplyVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理员接口（除登录外均需管理员角色）。
 *
 * @author haiy
 * @date 2026/08/17
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginRequest request) {
        return Result.ok(adminService.login(request));
    }

    // ==================== 商家申请审核 ====================

    @GetMapping("/merchant-applies")
    public Result<List<MerchantApplyVO>> merchantApplies() {
        return Result.ok(adminService.listMerchantApplies());
    }

    @PostMapping("/merchant-applies/{id}/approve")
    public Result<Void> approveMerchantApply(@PathVariable Long id, @RequestParam(required = false) String remark) {
        adminService.approveMerchantApply(id, remark);
        return Result.ok();
    }

    @PostMapping("/merchant-applies/{id}/reject")
    public Result<Void> rejectMerchantApply(@PathVariable Long id, @RequestParam(required = false) String remark) {
        adminService.rejectMerchantApply(id, remark);
        return Result.ok();
    }

    // ==================== 商品审核 ====================

    @GetMapping("/goods")
    public Result<List<Goods>> goods() {
        return Result.ok(adminService.listGoods());
    }

    @PostMapping("/goods/{id}/approve")
    public Result<Void> approveGoods(@PathVariable Long id) {
        adminService.approveGoods(id);
        return Result.ok();
    }

    @PostMapping("/goods/{id}/reject")
    public Result<Void> rejectGoods(@PathVariable Long id) {
        adminService.rejectGoods(id);
        return Result.ok();
    }

    // ==================== 秒杀商品审核 ====================

    @GetMapping("/seckill-goods")
    public Result<List<SeckillGoodsVO>> seckillGoods() {
        return Result.ok(adminService.listSeckillGoods());
    }

    @PostMapping("/seckill-goods/{id}/approve")
    public Result<Void> approveSeckillGoods(@PathVariable Long id) {
        adminService.approveSeckillGoods(id);
        return Result.ok();
    }

    @PostMapping("/seckill-goods/{id}/reject")
    public Result<Void> rejectSeckillGoods(@PathVariable Long id) {
        adminService.rejectSeckillGoods(id);
        return Result.ok();
    }

    // ==================== 订单与库存 ====================

    @GetMapping("/orders")
    public Result<List<MallOrder>> orders() {
        return Result.ok(adminService.listOrders());
    }

    @GetMapping("/seckill-orders")
    public Result<List<SeckillOrder>> seckillOrders() {
        return Result.ok(adminService.listSeckillOrders());
    }

    @PostMapping("/stock/reset")
    public Result<Void> resetStock() {
        adminService.resetStock();
        return Result.ok();
    }

    @PostMapping("/stock/reset/{seckillGoodsId}")
    public Result<Void> resetStock(@PathVariable Long seckillGoodsId) {
        adminService.resetStock(seckillGoodsId);
        return Result.ok();
    }
}
