package org.example.monomer_seckill_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.example.monomer_seckill_backend.entity.Goods;

public interface GoodsMapper extends BaseMapper<Goods> {

    /**
     * 原子扣减库存：只有库存 > 0 时才扣减，返回受影响行数。
     * 返回 0 说明库存不足（防止超卖的核心 SQL）。
     */
    @Update("UPDATE seckill_goods SET stock = stock - 1 WHERE id = #{goodsId} AND stock > 0")
    int deductStock(@Param("goodsId") Long goodsId);
}
