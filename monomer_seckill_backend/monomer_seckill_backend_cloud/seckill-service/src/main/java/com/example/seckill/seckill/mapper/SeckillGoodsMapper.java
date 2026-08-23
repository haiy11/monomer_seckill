package com.example.seckill.seckill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.seckill.seckill.entity.SeckillGoods;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 秒杀商品 Mapper（秒杀域专属）。
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
