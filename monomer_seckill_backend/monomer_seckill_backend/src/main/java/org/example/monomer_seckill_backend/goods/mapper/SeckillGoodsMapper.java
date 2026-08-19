package org.example.monomer_seckill_backend.goods.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.example.monomer_seckill_backend.goods.entity.SeckillGoods;

/**
 * 秒杀商品 Mapper。
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
