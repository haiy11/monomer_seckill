package com.example.seckill.goodsorder.client;

import com.example.seckill.common.core.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 秒杀服务远程调用客户端（OpenFeign）。
 *
 * <p>取消/超时关闭秒杀订单时，反向调用 seckill-service 回滚 Redis 预扣库存。</p>
 *
 * @author haiy
 * @date 2026/08/17
 */
@FeignClient(name = "seckill-service")
public interface SeckillStockClient {

    /**
     * 回滚 Redis 预扣库存（幂等）。
     */
    @PostMapping("/internal/stock/rollback-redis/{seckillGoodsId}")
    Result<Void> rollbackRedis(@PathVariable("seckillGoodsId") Long seckillGoodsId,
                               @RequestParam("userId") Long userId);
}
