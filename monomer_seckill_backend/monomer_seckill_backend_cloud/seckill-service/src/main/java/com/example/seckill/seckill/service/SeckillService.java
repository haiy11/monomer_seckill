package com.example.seckill.seckill.service;

import com.example.seckill.common.core.Constants;
import com.example.seckill.common.core.Result;
import com.example.seckill.common.entity.SeckillGoods;
import com.example.seckill.seckill.client.GoodsOrderClient;
import org.springframework.stereotype.Service;

/**
 * 秒杀核心服务（编排层）。
 *
 * <p>本身不直接访问数据库/库存，通过 {@link GoodsOrderClient} 远程调用完成：
 * ① 校验可购买 → ② Redis 预扣库存 → ③ DB 扣减 + 创建订单；失败时回滚 Redis。</p>
 *
 * @author haiy
 * @date 2026/08/17
 */
@Service
public class SeckillService {

    private final GoodsOrderClient goodsOrderClient;

    public SeckillService(GoodsOrderClient goodsOrderClient) {
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

        // 1. 校验秒杀商品存在、已上架且在时间窗口内
        Result<SeckillGoods> purchasable = goodsOrderClient.getPurchasable(seckillGoodsId);
        if (isFail(purchasable)) {
            return Result.fail(purchasable == null ? "商品服务异常" : purchasable.getMsg());
        }

        // 2. Redis 原子预扣库存
        Result<Integer> deduct = goodsOrderClient.deductStock(seckillGoodsId, userId);
        if (isFail(deduct)) {
            return Result.fail(deduct == null ? "库存服务异常" : deduct.getMsg());
        }
        long code = deduct.getData();
        if (code == Constants.LUA_STOCK_EMPTY) {
            return Result.fail("手慢了，库存不足");
        }
        if (code == Constants.LUA_DUPLICATE) {
            return Result.fail("您已抢购过该秒杀商品");
        }

        // 3. 创建秒杀订单（DB 扣库存 + 插订单）
        Result<String> created = goodsOrderClient.createSeckillOrder(seckillGoodsId, userId);
        if (isFail(created)) {
            // DB 写入失败，回滚 Redis 预扣
            goodsOrderClient.rollbackRedis(seckillGoodsId, userId);
            return Result.fail(created == null ? "下单失败" : created.getMsg());
        }
        return Result.ok(created.getData());
    }

    private boolean isFail(Result<?> result) {
        return result == null || result.getCode() == null || result.getCode() != 200;
    }
}
