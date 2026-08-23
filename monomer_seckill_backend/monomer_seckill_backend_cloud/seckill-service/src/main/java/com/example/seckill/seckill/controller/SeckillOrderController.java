package com.example.seckill.seckill.controller;

import com.example.seckill.common.core.Result;
import com.example.seckill.common.core.UserContext;
import com.example.seckill.seckill.entity.SeckillOrder;
import com.example.seckill.seckill.service.SeckillOrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 秒杀订单接口（需登录）。
 *
 * @author haiy
 * @date 2026/08/17
 */
@RestController
@RequestMapping("/api/seckill/order")
public class SeckillOrderController {

    private final SeckillOrderService seckillOrderService;

    public SeckillOrderController(SeckillOrderService seckillOrderService) {
        this.seckillOrderService = seckillOrderService;
    }

    /**
     * 我的秒杀订单列表。
     */
    @GetMapping("/list")
    public Result<List<SeckillOrder>> list() {
        return Result.ok(seckillOrderService.listByUser(UserContext.getUserId()));
    }

    /**
     * 支付秒杀订单。
     */
    @PostMapping("/{orderNo}/pay")
    public Result<Void> pay(@PathVariable String orderNo) {
        seckillOrderService.pay(orderNo, UserContext.getUserId());
        return Result.ok();
    }

    /**
     * 取消秒杀订单。
     */
    @PostMapping("/{orderNo}/cancel")
    public Result<Void> cancel(@PathVariable String orderNo) {
        seckillOrderService.cancel(orderNo, UserContext.getUserId());
        return Result.ok();
    }
}
