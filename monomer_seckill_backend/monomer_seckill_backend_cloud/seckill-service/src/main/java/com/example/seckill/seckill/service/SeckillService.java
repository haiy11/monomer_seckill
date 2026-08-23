package com.example.seckill.seckill.service;

import com.example.seckill.common.core.BizException;
import com.example.seckill.common.core.Result;
import com.example.seckill.seckill.constant.SeckillConstants;
import com.example.seckill.seckill.entity.SeckillGoods;
import com.example.seckill.seckill.entity.SeckillOrder;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/**
 * 秒杀核心服务：校验可购买 → Redis 预扣库存 → 创建秒杀订单。
 *
 * <p>秒杀下单链路（商品校验、Redis 预扣、DB 扣减、订单落库）完全在秒杀域内闭环，
 * 高并发流量与其它服务隔离。</p>
 *
 * @author haiy
 * @date 2026/08/17
 */
@Service
public class SeckillService {

    private final SeckillGoodsService seckillGoodsService;
    private final StockService stockService;
    private final SeckillOrderService seckillOrderService;

    public SeckillService(SeckillGoodsService seckillGoodsService, StockService stockService,
                          SeckillOrderService seckillOrderService) {
        this.seckillGoodsService = seckillGoodsService;
        this.stockService = stockService;
        this.seckillOrderService = seckillOrderService;
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

        // 2. Redis 原子预扣库存
        long code = stockService.deduct(seckillGoodsId, userId);
        if (code == SeckillConstants.LUA_STOCK_EMPTY) {
            return Result.fail("手慢了，库存不足");
        }
        if (code == SeckillConstants.LUA_DUPLICATE) {
            return Result.fail("您已抢购过该秒杀商品");
        }

        // 3. 创建秒杀订单（DB 扣库存 + 插订单），失败时回滚 Redis
        try {
            SeckillOrder order = seckillOrderService.createSeckillOrder(seckillGoodsId, userId, sg.getSeckillPrice());
            return Result.ok(order.getOrderNo());
        } catch (DuplicateKeyException e) {
            stockService.rollbackRedis(seckillGoodsId, userId);
            return Result.fail("您已抢购过该秒杀商品");
        } catch (BizException e) {
            stockService.rollbackRedis(seckillGoodsId, userId);
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            stockService.rollbackRedis(seckillGoodsId, userId);
            throw e;
        }
    }
}
