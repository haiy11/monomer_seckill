package com.example.seckill.goodsorder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.seckill.goodsorder.entity.SeckillOrder;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 秒杀订单 Mapper。
 *
 * @author haiy
 * @date 2026/08/17
 */
public interface SeckillOrderMapper extends BaseMapper<SeckillOrder> {

    /**
     * 支付：仅待支付可支付。
     */
    @Update("UPDATE seckill_order SET status = 1, pay_time = #{payTime}, update_time = #{payTime} " +
            "WHERE order_no = #{orderNo} AND user_id = #{userId} AND status = 0")
    int pay(@Param("orderNo") String orderNo, @Param("userId") Long userId,
            @Param("payTime") LocalDateTime payTime);

    /**
     * 取消：仅待支付可取消。
     */
    @Update("UPDATE seckill_order SET status = 2, update_time = #{updateTime} " +
            "WHERE order_no = #{orderNo} AND user_id = #{userId} AND status = 0")
    int cancel(@Param("orderNo") String orderNo, @Param("userId") Long userId,
               @Param("updateTime") LocalDateTime updateTime);

    /**
     * 超时关闭：仅待支付可关闭。
     */
    @Update("UPDATE seckill_order SET status = 3, update_time = #{updateTime} WHERE id = #{id} AND status = 0")
    int closeTimeout(@Param("id") Long id, @Param("updateTime") LocalDateTime updateTime);
}
