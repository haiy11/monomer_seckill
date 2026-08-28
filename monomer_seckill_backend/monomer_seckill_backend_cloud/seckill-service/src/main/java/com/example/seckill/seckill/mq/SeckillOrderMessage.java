package com.example.seckill.seckill.mq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 秒杀下单消息（Redis 预扣成功 → 异步建单）。
 *
 * <p>由 {@link SeckillMqProducer} 在 Redis Lua 预扣成功后发送，由
 * {@link SeckillOrderCreateListener} 消费完成「DB 扣库存 + 落订单」。</p>
 *
 * <p>字段全部为不可变业务要素 + 一条幂等标识 {@code messageId}；消费者以 {@code orderNo}
 * 做幂等判断，保证 RabbitMQ「至少一次投递」语义下重复消费不会重复建单。</p>
 *
 * @author haiy
 * @date 2026/08/27
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeckillOrderMessage {

    /** 消息唯一标识（UUID），用于发布确认关联与日志排查 */
    private String messageId;

    /** 订单号（生产端预生成，用于幂等与对用户即时返回） */
    private String orderNo;

    /** 秒杀商品ID */
    private Long seckillGoodsId;

    /** 用户ID */
    private Long userId;

    /** 成交价（秒杀价快照） */
    private BigDecimal seckillPrice;

    /**
     * 构造下单消息（自动生成 messageId）。
     */
    public static SeckillOrderMessage of(String orderNo, Long seckillGoodsId, Long userId, BigDecimal seckillPrice) {
        return new SeckillOrderMessage(UUID.randomUUID().toString(), orderNo, seckillGoodsId, userId, seckillPrice);
    }
}
