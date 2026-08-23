package com.example.seckill.user.client;

import com.example.seckill.common.core.Result;
import com.example.seckill.user.dto.SeckillApplyRequest;
import com.example.seckill.user.entity.SeckillGoods;
import com.example.seckill.user.entity.SeckillOrder;
import com.example.seckill.user.vo.SeckillGoodsVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 秒杀服务远程调用客户端（OpenFeign）。
 *
 * <p>管理员审核秒杀商品/查看秒杀订单/重置库存、商家管理秒杀商品，统一经本客户端调 seckill-service。</p>
 *
 * @author haiy
 * @date 2026/08/17
 */
@FeignClient(name = "seckill-service")
public interface SeckillClient {

    @GetMapping("/internal/admin/seckill-goods")
    Result<List<SeckillGoodsVO>> listSeckillGoods();

    @PostMapping("/internal/admin/seckill-goods/{id}/approve")
    Result<Void> approveSeckillGoods(@PathVariable("id") Long id);

    @PostMapping("/internal/admin/seckill-goods/{id}/reject")
    Result<Void> rejectSeckillGoods(@PathVariable("id") Long id);

    @GetMapping("/internal/admin/seckill-orders")
    Result<List<SeckillOrder>> listSeckillOrders();

    @PostMapping("/internal/admin/stock/reset")
    Result<Void> resetStock();

    @PostMapping("/internal/admin/stock/reset/{seckillGoodsId}")
    Result<Void> resetStockOne(@PathVariable("seckillGoodsId") Long seckillGoodsId);

    @GetMapping("/internal/merchant/{merchantId}/seckill-goods")
    Result<List<SeckillGoods>> listMerchantSeckillGoods(@PathVariable("merchantId") Long merchantId);

    @PostMapping("/internal/merchant/{merchantId}/seckill-goods")
    Result<SeckillGoods> applySeckill(@PathVariable("merchantId") Long merchantId,
                                      @RequestBody SeckillApplyRequest request);

    @PutMapping("/internal/merchant/{merchantId}/seckill-goods/{id}")
    Result<SeckillGoods> updateSeckillGoods(@PathVariable("merchantId") Long merchantId,
                                            @PathVariable("id") Long id,
                                            @RequestBody SeckillApplyRequest request);
}
