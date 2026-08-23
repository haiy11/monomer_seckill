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
 * 商品实体（管理员审核/商家管理所用视图），对应表 goods。
 *
 * @author haiy
 * @date 2026/08/17
 */
@Data
@TableName("goods")
public class Goods {

    /** 主键，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商家用户ID */
    private Long merchantId;

    /** 商品名称 */
    private String name;

    /** 商品描述 */
    private String description;

    /** 价格 */
    private BigDecimal price;

    /** 库存 */
    private Integer stock;

    /** 商品图片地址 */
    private String imageUrl;

    /** 状态：0-待审核 1-已上架 2-已下架 3-已拒绝 */
    private Integer status;

    /** 创建时间（应用自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间（应用自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
