package com.example.seckill.goodsorder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.seckill.goodsorder.entity.SeckillGoods;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 秒杀商品库存 Mapper（订单域仅用于 DB 扣减/回补）。
 *
 * @author haiy
 * @date 2026/08/17
 */
public interface SeckillGoodsMapper extends BaseMapper<SeckillGoods> {

    /**
     * 原子扣减秒杀库存：仅当库存大于 0 时扣减。
     */
    @Update("UPDATE seckill_goods SET seckill_stock = seckill_stock - 1 WHERE id = #{id} AND seckill_stock > 0")
    int deductStock(@Param("id") Long id);

    /**
     * 回补秒杀库存。
     */
    @Update("UPDATE seckill_goods SET seckill_stock = seckill_stock + 1 WHERE id = #{id}")
    int restoreStock(@Param("id") Long id);
}
