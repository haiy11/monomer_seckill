package org.example.monomer_seckill_backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.monomer_seckill_backend.common.Result;
import org.example.monomer_seckill_backend.entity.Goods;
import org.example.monomer_seckill_backend.entity.SeckillOrder;
import org.example.monomer_seckill_backend.mapper.GoodsMapper;
import org.example.monomer_seckill_backend.mapper.SeckillOrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 秒杀核心服务（最简版本，无优化，先把业务流程跑通）
 *
 * 流程：校验商品 -> 校验重复抢购 -> 原子扣库存 -> 创建订单
 * 后续 P2 阶段再引入 Redis 预扣库存 + Lua 脚本等优化。
 */
@Service
public class SeckillService {

    private final GoodsMapper goodsMapper;
    private final SeckillOrderMapper orderMapper;

    public SeckillService(GoodsMapper goodsMapper, SeckillOrderMapper orderMapper) {
        this.goodsMapper = goodsMapper;
        this.orderMapper = orderMapper;
    }

    @Transactional
    public Result<Long> seckill(Long goodsId, Long userId) {
        if (goodsId == null || userId == null) {
            return Result.fail("参数错误：goodsId 和 userId 不能为空");
        }

        // 1. 校验商品是否存在
        Goods goods = goodsMapper.selectById(goodsId);
        if (goods == null) {
            return Result.fail("商品不存在");
        }

        // 2. 校验是否重复抢购（数据库唯一索引 uk_goods_user 作为兜底）
        Long count = orderMapper.selectCount(new LambdaQueryWrapper<SeckillOrder>()
                .eq(SeckillOrder::getGoodsId, goodsId)
                .eq(SeckillOrder::getUserId, userId));
        if (count != null && count > 0) {
            return Result.fail("您已抢购过该商品，请勿重复抢购");
        }

        // 3. 原子扣减库存，返回 0 表示库存不足
        int rows = goodsMapper.deductStock(goodsId);
        if (rows == 0) {
            return Result.fail("手慢了，库存不足");
        }

        // 4. 创建订单
        SeckillOrder order = new SeckillOrder();
        order.setGoodsId(goodsId);
        order.setUserId(userId);
        order.setStatus(0);
        orderMapper.insert(order);

        return Result.ok(order.getId());
    }
}
