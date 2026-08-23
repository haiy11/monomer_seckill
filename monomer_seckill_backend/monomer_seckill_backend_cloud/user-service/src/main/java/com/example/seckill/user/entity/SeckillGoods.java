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
 * 秒杀商品实体（管理员审核/商家管理所用视图），对应表 seckill_goods。
 *
 * @author haiy
 * @date 2026/08/17
 */
@Data
@TableName("seckill_goods")
public class SeckillGoods {

    /** 主键，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商家用户ID */
    private Long merchantId;

    /** 秒杀商品名称 */
    private String name;

    /** 秒杀价 */
    private BigDecimal seckillPrice;

    /** 秒杀库存 */
    private Integer seckillStock;

    /** 秒杀开始时间 */
    private LocalDateTime startTime;

    /** 秒杀结束时间 */
    private LocalDateTime endTime;

    /** 状态：0-待审核 1-已上架 2-已拒绝 3-已下架/已结束 */
    private Integer status;

    /** 创建时间（应用自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间（应用自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
