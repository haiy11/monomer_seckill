package org.example.monomer_seckill_backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 秒杀订单实体，对应表 seckill_order。
 *
 * <p>表上建有 (goods_id, user_id) 唯一索引，用于兜底防止同一用户对同一商品重复下单。</p>
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

    /** 商品ID */
    private Long goodsId;

    /** 用户ID */
    private Long userId;

    /** 订单状态：0-已下单 1-已支付 */
    private Integer status;

    /** 下单时间 */
    private LocalDateTime createTime;
}
