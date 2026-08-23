package com.example.seckill.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.seckill.common.entity.Goods;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 商品 Mapper。
 *
 * @author haiy
 * @date 2026/08/17
 */
public interface GoodsMapper extends BaseMapper<Goods> {

    /**
     * 原子扣减库存（按数量）：仅当库存充足时才扣减，防止超卖。
     *
     * @return 受影响行数：1 成功 / 0 库存不足
     */
    @Update("UPDATE goods SET stock = stock - #{quantity} WHERE id = #{goodsId} AND stock >= #{quantity}")
    int deductStock(@Param("goodsId") Long goodsId, @Param("quantity") int quantity);

    /**
     * 回补库存（取消/超时回滚时调用）。
     */
    @Update("UPDATE goods SET stock = stock + #{quantity} WHERE id = #{goodsId}")
    int restoreStock(@Param("goodsId") Long goodsId, @Param("quantity") int quantity);
}
