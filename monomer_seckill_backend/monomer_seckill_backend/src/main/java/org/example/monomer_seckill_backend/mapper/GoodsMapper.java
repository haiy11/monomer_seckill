package org.example.monomer_seckill_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.example.monomer_seckill_backend.entity.Goods;

/**
 * 秒杀商品 Mapper。
 *
 * <p>继承 MyBatis-Plus 的 BaseMapper，获得单表 CRUD 能力；自定义方法见 {@link #deductStock(Long)}。</p>
 *
 * @author haiy
 * @date 2026/08/17
 */
public interface GoodsMapper extends BaseMapper<Goods> {

    /**
     * 原子扣减库存：仅当库存大于 0 时才执行扣减，防止超卖。
     *
     * <p>「判断库存 &gt; 0」与「扣减」在同一条 UPDATE 中完成，天然具备原子性；
     * 返回受影响行数，0 表示库存不足、扣减失败。</p>
     *
     * @param goodsId 商品ID
     * @return 受影响行数：1 表示扣减成功，0 表示库存不足
     */
    @Update("UPDATE seckill_goods SET stock = stock - 1 WHERE id = #{goodsId} AND stock > 0")
    int deductStock(@Param("goodsId") Long goodsId);
}
