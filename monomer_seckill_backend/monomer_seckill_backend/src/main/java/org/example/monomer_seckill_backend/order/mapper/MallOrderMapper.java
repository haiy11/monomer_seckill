package org.example.monomer_seckill_backend.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.example.monomer_seckill_backend.order.entity.MallOrder;

import java.time.LocalDateTime;

/**
 * 订单主表 Mapper。
 *
 * @author haiy
 * @date 2026/08/17
 */
public interface MallOrderMapper extends BaseMapper<MallOrder> {

    /**
     * 支付：仅待支付可支付。
     */
    @Update("UPDATE mall_order SET status = 1, pay_time = #{payTime}, update_time = #{payTime} " +
            "WHERE order_no = #{orderNo} AND user_id = #{userId} AND status = 0")
    int pay(@Param("orderNo") String orderNo, @Param("userId") Long userId,
            @Param("payTime") LocalDateTime payTime);

    /**
     * 取消：仅待支付可取消。
     */
    @Update("UPDATE mall_order SET status = 2, update_time = #{updateTime} " +
            "WHERE order_no = #{orderNo} AND user_id = #{userId} AND status = 0")
    int cancel(@Param("orderNo") String orderNo, @Param("userId") Long userId,
               @Param("updateTime") LocalDateTime updateTime);

    /**
     * 超时关闭：仅待支付可关闭。
     */
    @Update("UPDATE mall_order SET status = 3, update_time = #{updateTime} WHERE id = #{id} AND status = 0")
    int closeTimeout(@Param("id") Long id, @Param("updateTime") LocalDateTime updateTime);
}
