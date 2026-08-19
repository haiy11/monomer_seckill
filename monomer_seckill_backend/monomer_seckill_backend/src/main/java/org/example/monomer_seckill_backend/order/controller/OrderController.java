package org.example.monomer_seckill_backend.order.controller;

import org.example.monomer_seckill_backend.common.Result;
import org.example.monomer_seckill_backend.common.UserContext;
import org.example.monomer_seckill_backend.order.entity.MallOrder;
import org.example.monomer_seckill_backend.order.entity.SeckillOrder;
import org.example.monomer_seckill_backend.order.service.OrderService;
import org.example.monomer_seckill_backend.order.vo.OrderVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 订单接口（需登录）。
 *
 * @author haiy
 * @date 2026/08/17
 */
@RestController
@RequestMapping("/api/order")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 购物车结算下单（多商品一起下单）。
     */
    @PostMapping("/checkout")
    public Result<MallOrder> checkout() {
        return Result.ok(orderService.checkout(UserContext.getUserId()));
    }

    /**
     * 我的正常订单列表。
     */
    @GetMapping("/list")
    public Result<List<MallOrder>> list() {
        return Result.ok(orderService.listNormalByUser(UserContext.getUserId()));
    }

    /**
     * 正常订单详情。
     */
    @GetMapping("/{orderNo}")
    public Result<OrderVO> detail(@PathVariable String orderNo) {
        return Result.ok(orderService.getNormalOrder(orderNo, UserContext.getUserId()));
    }

    @PostMapping("/{orderNo}/pay")
    public Result<Void> pay(@PathVariable String orderNo) {
        orderService.payNormal(orderNo, UserContext.getUserId());
        return Result.ok();
    }

    @PostMapping("/{orderNo}/cancel")
    public Result<Void> cancel(@PathVariable String orderNo) {
        orderService.cancelNormal(orderNo, UserContext.getUserId());
        return Result.ok();
    }

    /**
     * 我的秒杀订单列表。
     */
    @GetMapping("/seckill/list")
    public Result<List<SeckillOrder>> seckillList() {
        return Result.ok(orderService.listSeckillByUser(UserContext.getUserId()));
    }

    @PostMapping("/seckill/{orderNo}/pay")
    public Result<Void> seckillPay(@PathVariable String orderNo) {
        orderService.paySeckill(orderNo, UserContext.getUserId());
        return Result.ok();
    }

    @PostMapping("/seckill/{orderNo}/cancel")
    public Result<Void> seckillCancel(@PathVariable String orderNo) {
        orderService.cancelSeckill(orderNo, UserContext.getUserId());
        return Result.ok();
    }
}
