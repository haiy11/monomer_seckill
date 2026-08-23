package com.example.seckill.user.controller;

import com.example.seckill.common.core.Result;
import com.example.seckill.common.core.UserContext;
import com.example.seckill.user.entity.Goods;
import com.example.seckill.user.entity.SeckillGoods;
import com.example.seckill.user.dto.SeckillApplyRequest;
import com.example.seckill.user.service.MerchantService;
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
 * 商家接口（需商家角色）。
 *
 * @author haiy
 * @date 2026/08/17
 */
@RestController
@RequestMapping("/api/merchant")
public class MerchantController {

    private final MerchantService merchantService;

    public MerchantController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    /**
     * 我的商品。
     */
    @GetMapping("/goods")
    public Result<List<Goods>> myGoods() {
        return Result.ok(merchantService.listMyGoods(UserContext.getUserId()));
    }

    /**
     * 新增商品（待管理员审核）。
     */
    @PostMapping("/goods")
    public Result<Goods> addGoods(@RequestBody Goods goods) {
        return Result.ok(merchantService.addGoods(UserContext.getUserId(), goods));
    }

    /**
     * 修改商品。
     */
    @PutMapping("/goods/{id}")
    public Result<Goods> updateGoods(@PathVariable Long id, @RequestBody Goods goods) {
        return Result.ok(merchantService.updateGoods(UserContext.getUserId(), id, goods));
    }

    /**
     * 上架 / 下架我的商品。
     */
    @PutMapping("/goods/{id}/status")
    public Result<Void> updateGoodsStatus(@PathVariable Long id, @RequestParam Integer status) {
        merchantService.updateGoodsStatus(UserContext.getUserId(), id, status);
        return Result.ok();
    }

    /**
     * 申请新建秒杀商品（与正常商品独立）。
     */
    @PostMapping("/seckill-goods")
    public Result<SeckillGoods> applySeckill(@RequestBody SeckillApplyRequest request) {
        return Result.ok(merchantService.applySeckill(UserContext.getUserId(), request));
    }

    /**
     * 编辑秒杀商品（被拒绝的编辑后重新提交审核）。
     */
    @PutMapping("/seckill-goods/{id}")
    public Result<SeckillGoods> updateSeckill(@PathVariable Long id, @RequestBody SeckillApplyRequest request) {
        return Result.ok(merchantService.updateSeckillGoods(UserContext.getUserId(), id, request));
    }

    /**
     * 我的秒杀申请列表。
     */
    @GetMapping("/seckill-goods")
    public Result<List<SeckillGoods>> mySeckill() {
        return Result.ok(merchantService.listMySeckill(UserContext.getUserId()));
    }
}
