package com.example.seckill.common.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀订单实体，对应表 seckill_order。
 *
 * <p>表上建有 (seckill_goods_id, user_id) 唯一索引，作为「同一用户对同一秒杀活动只下一单」的最终兜底。</p>
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
