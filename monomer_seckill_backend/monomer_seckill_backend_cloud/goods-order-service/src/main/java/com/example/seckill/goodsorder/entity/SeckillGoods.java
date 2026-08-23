package com.example.seckill.goodsorder.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀商品实体（订单域的库存持久化视图），对应表 seckill_goods。
 *
 * <p>本服务建秒杀订单时对 seckill_goods 做数据库扣减/回补，仅用到该表的库存字段；
 * 秒杀商品的完整业务逻辑（缓存/校验/Redis 预扣）由 seckill-service 负责。</p>
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
