package com.example.seckill.goodsorder.controller;

import com.example.seckill.common.core.BizException;
import com.example.seckill.common.core.Result;
import com.example.seckill.goodsorder.entity.SeckillOrder;
import com.example.seckill.goodsorder.service.OrderService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * 秒杀订单内部接口（供 seckill-service 通过 OpenFeign 调用）。
 *
 * <p>返回统一 {@link Result}，避免把业务失败映射成 HTTP 5xx 后在 Feign 侧无法区分语义。</p>
 *
 * @author haiy
 * @date 2026/08/17
 */
@RestController
@RequestMapping("/internal")
public class InternalOrderController {

    private final OrderService orderService;

    public InternalOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 创建秒杀订单（DB 扣减秒杀库存 + 插入订单），返回订单号。
     */
    @PostMapping("/seckill-order/create/{seckillGoodsId}")
    public Result<String> create(@PathVariable Long seckillGoodsId,
                                 @RequestParam Long userId,
                                 @RequestParam BigDecimal price) {
        try {
            SeckillOrder order = orderService.createSeckillOrder(seckillGoodsId, userId, price);
            return Result.ok(order.getOrderNo());
        } catch (DuplicateKeyException e) {
            return Result.fail("您已抢购过该秒杀商品");
        } catch (BizException e) {
            return Result.fail(e.getCode(), e.getMessage());
        }
    }
}
