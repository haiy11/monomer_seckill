package com.example.seckill.user.client;

import com.example.seckill.common.core.Result;
import com.example.seckill.user.entity.Goods;
import com.example.seckill.user.entity.MallOrder;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 商品/订单服务远程调用客户端（OpenFeign）。
 *
 * <p>管理员审核商品/查看订单、商家管理商品，统一经本客户端调 goods-order-service。</p>
 *
 * @author haiy
 * @date 2026/08/17
 */
@FeignClient(name = "goods-order-service")
public interface GoodsOrderClient {

    @GetMapping("/internal/admin/goods")
    Result<List<Goods>> listGoods();

    @PostMapping("/internal/admin/goods/{id}/approve")
    Result<Void> approveGoods(@PathVariable("id") Long id);

    @PostMapping("/internal/admin/goods/{id}/reject")
    Result<Void> rejectGoods(@PathVariable("id") Long id);

    @GetMapping("/internal/admin/orders")
    Result<List<MallOrder>> listOrders();

    @GetMapping("/internal/merchant/{merchantId}/goods")
    Result<List<Goods>> listMerchantGoods(@PathVariable("merchantId") Long merchantId);

    @PostMapping("/internal/merchant/{merchantId}/goods")
    Result<Goods> addGoods(@PathVariable("merchantId") Long merchantId, @RequestBody Goods goods);

    @PutMapping("/internal/merchant/{merchantId}/goods/{goodsId}")
    Result<Goods> updateGoods(@PathVariable("merchantId") Long merchantId,
                              @PathVariable("goodsId") Long goodsId, @RequestBody Goods goods);

    @PutMapping("/internal/merchant/{merchantId}/goods/{goodsId}/status")
    Result<Void> updateGoodsStatus(@PathVariable("merchantId") Long merchantId,
                                   @PathVariable("goodsId") Long goodsId, @RequestParam("status") Integer status);
}
