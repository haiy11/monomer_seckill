package com.example.seckill.seckill.client;

import com.example.seckill.common.core.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

/**
 * 商品/订单服务远程调用客户端（OpenFeign）。
 *
 * <p>秒杀服务完成 Redis 预扣后，通过本客户端调用 goods-order-service
 * 完成「数据库扣减秒杀库存 + 创建秒杀订单」。</p>
 *
 * @author haiy
 * @date 2026/08/17
 */
@FeignClient(name = "goods-order-service")
public interface GoodsOrderClient {

    /**
     * 创建秒杀订单（DB 扣减秒杀库存 + 插入订单），返回订单号。
     */
    @PostMapping("/internal/seckill-order/create/{seckillGoodsId}")
    Result<String> createSeckillOrder(@PathVariable("seckillGoodsId") Long seckillGoodsId,
                                      @RequestParam("userId") Long userId,
                                      @RequestParam("price") BigDecimal price);
}
