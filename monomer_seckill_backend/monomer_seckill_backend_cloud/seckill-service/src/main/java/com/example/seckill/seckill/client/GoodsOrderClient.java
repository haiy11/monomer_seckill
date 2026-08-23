package com.example.seckill.seckill.client;

import com.example.seckill.common.core.Result;
import com.example.seckill.common.entity.SeckillGoods;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 商品/订单服务远程调用客户端（OpenFeign）。
 *
 * <p>通过服务名 goods-order-service 经 Nacos 服务发现 + 负载均衡，
 * 调用其内部接口完成秒杀下单链路的库存扣减与订单创建。</p>
 *
 * @author haiy
 * @date 2026/08/17
 */
@FeignClient(name = "goods-order-service")
public interface GoodsOrderClient {

    /**
     * 校验秒杀商品可购买（存在、已上架、时间窗口内）。
     */
    @GetMapping("/internal/seckill-goods/{id}/purchasable")
    Result<SeckillGoods> getPurchasable(@PathVariable("id") Long id);

    /**
     * Redis 原子预扣库存（Lua），返回 Lua 结果码。
     */
    @PostMapping("/internal/stock/deduct/{seckillGoodsId}")
    Result<Integer> deductStock(@PathVariable("seckillGoodsId") Long seckillGoodsId,
                                @RequestParam("userId") Long userId);

    /**
     * 创建秒杀订单（DB 扣减秒杀库存 + 插入订单），返回订单号。
     */
    @PostMapping("/internal/seckill-order/create/{seckillGoodsId}")
    Result<String> createSeckillOrder(@PathVariable("seckillGoodsId") Long seckillGoodsId,
                                      @RequestParam("userId") Long userId);

    /**
     * 仅回滚 Redis 预扣库存（下单失败时调用）。
     */
    @PostMapping("/internal/stock/rollback-redis/{seckillGoodsId}")
    Result<Void> rollbackRedis(@PathVariable("seckillGoodsId") Long seckillGoodsId,
                               @RequestParam("userId") Long userId);
}
