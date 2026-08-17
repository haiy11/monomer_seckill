package org.example.monomer_seckill_backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀商品实体，对应表 seckill_goods。
 *
 * @author haiy
 * @date 2026/08/17
 */
@Data
@TableName("seckill_goods")
public class Goods {

    /** 主键，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商品名称 */
    private String name;

    /** 商品描述 */
    private String description;

    /** 价格 */
    private BigDecimal price;

    /** 库存数量 */
    private Integer stock;

    /** 秒杀开始时间 */
    private LocalDateTime startTime;

    /** 秒杀结束时间 */
    private LocalDateTime endTime;

    /** 创建时间 */
    private LocalDateTime createTime;
}
