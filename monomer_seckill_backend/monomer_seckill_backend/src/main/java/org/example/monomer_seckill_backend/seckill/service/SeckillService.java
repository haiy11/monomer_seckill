package org.example.monomer_seckill_backend.seckill.service;

import org.example.monomer_seckill_backend.common.BizException;
import org.example.monomer_seckill_backend.common.Constants;
import org.example.monomer_seckill_backend.common.Result;
import org.example.monomer_seckill_backend.goods.entity.SeckillGoods;
import org.example.monomer_seckill_backend.goods.service.SeckillGoodsService;
import org.example.monomer_seckill_backend.goods.service.StockService;
import org.example.monomer_seckill_backend.order.entity.SeckillOrder;
import org.example.monomer_seckill_backend.order.service.OrderService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/**
 * 秒杀核心服务：Redis 预扣库存 + 限时秒杀下单。
 *
 * @author haiy
 * @date 2026/08/17
 */
@Service
public class SeckillService {

    private final SeckillGoodsService seckillGoodsService;
    private final StockService stockService;
    private final OrderService orderService;

    public SeckillService(SeckillGoodsService seckillGoodsService, StockService stockService, OrderService orderService) {
        this.seckillGoodsService = seckillGoodsService;
        this.stockService = stockService;
        this.orderService = orderService;
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
        // 校验秒杀商品存在、已上架且在时间窗口内（不满足时抛业务异常，由全局异常处理返回）
        SeckillGoods sg = seckillGoodsService.requirePurchasable(seckillGoodsId);

        // Redis 原子预扣库存
        long code = stockService.deduct(seckillGoodsId, userId);
        if (code == Constants.LUA_STOCK_EMPTY) {
            return Result.fail("手慢了，库存不足");
        }
        if (code == Constants.LUA_DUPLICATE) {
            return Result.fail("您已抢购过该秒杀商品");
        }

        // 创建秒杀订单（DB 扣库存 + 插订单）
        try {
            SeckillOrder order = orderService.createSeckillOrder(seckillGoodsId, userId, sg.getSeckillPrice());
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
