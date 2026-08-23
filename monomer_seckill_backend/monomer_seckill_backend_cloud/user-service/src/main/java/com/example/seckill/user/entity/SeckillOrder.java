package com.example.seckill.user.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀订单实体（管理员查看秒杀订单所用视图），对应表 seckill_order。
 *
 * @author haiy
 * @date 2026/08/17
 */
@Data
@TableName("seckill_order")
public class SeckillOrder {

    /** 主键，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单号（全局唯一） */
    private String orderNo;

    /** 用户ID */
    private Long userId;

    /** 秒杀商品ID */
    private Long seckillGoodsId;

    /** 成交价（秒杀价快照） */
    private BigDecimal seckillPrice;

    /** 状态：0-待支付 1-已支付 2-已取消 3-超时关闭 */
    private Integer status;

    /** 下单时间（应用自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 支付时间 */
    private LocalDateTime payTime;

    /** 更新时间（应用自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
