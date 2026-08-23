package com.example.seckill.seckill.service;

import com.example.seckill.common.core.Result;
import com.example.seckill.seckill.client.GoodsOrderClient;
import com.example.seckill.seckill.constant.SeckillConstants;
import com.example.seckill.seckill.entity.SeckillGoods;
import org.springframework.stereotype.Service;

/**
 * 秒杀核心服务（编排层）。
 *
 * <p>① 校验可购买 → ② 本地 Redis 原子预扣库存 → ③ Feign 调用 goods-order-service
 * 完成 DB 扣减 + 建单；建单失败时本地回滚 Redis。</p>
 *
 * @author haiy
 * @date 2026/08/17
 */
@Service
public class SeckillService {

    private final SeckillGoodsService seckillGoodsService;
    private final StockService stockService;
    private final GoodsOrderClient goodsOrderClient;

    public SeckillService(SeckillGoodsService seckillGoodsService, StockService stockService,
                          GoodsOrderClient goodsOrderClient) {
        this.seckillGoodsService = seckillGoodsService;
        this.stockService = stockService;
        this.goodsOrderClient = goodsOrderClient;
    }

    /**
     * 秒杀下单（需登录，userId 来自 token）。
     *
     * @param seckillGoodsId 秒杀商品ID
     * @param userId         用户ID
     * @return 成功携带订单号
     */
    public Result<String> seckill(Long seckillGoodsId, Long userId) {
        if (seckillGoodsId == null || userId == null) {
            return Result.fail("参数错误");
        }

        // 1. 校验秒杀商品存在、已上架且在时间窗口内（不满足抛业务异常）
        SeckillGoods sg = seckillGoodsService.requirePurchasable(seckillGoodsId);

        // 2. 本地 Redis 原子预扣库存
        long code = stockService.deduct(seckillGoodsId, userId);
        if (code == SeckillConstants.LUA_STOCK_EMPTY) {
            return Result.fail("手慢了，库存不足");
        }
        if (code == SeckillConstants.LUA_DUPLICATE) {
            return Result.fail("您已抢购过该秒杀商品");
        }

        // 3. 远程创建秒杀订单（DB 扣库存 + 插订单）
        Result<String> created = goodsOrderClient.createSeckillOrder(seckillGoodsId, userId, sg.getSeckillPrice());
        if (isFail(created)) {
            stockService.rollbackRedis(seckillGoodsId, userId);
            return Result.fail(created == null ? "下单失败" : created.getMsg());
        }
        return Result.ok(created.getData());
    }

    private boolean isFail(Result<?> result) {
        return result == null || result.getCode() == null || result.getCode() != 200;
    }
}
