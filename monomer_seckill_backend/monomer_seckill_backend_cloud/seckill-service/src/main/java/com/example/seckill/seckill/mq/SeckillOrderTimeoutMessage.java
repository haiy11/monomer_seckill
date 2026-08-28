package com.example.seckill.seckill.mq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 秒杀订单超时消息（延迟队列 TTL 过期 → 死信队列）。
 *
 * <p>建单成功后由 {@link SeckillMqProducer} 发送到延迟队列并设置 TTL；过期后经死信交换机
 * 投递到超时队列，由 {@link SeckillOrderTimeoutListener} 消费：订单仍为「待支付」则关闭并回补库存。</p>
 *
 * @author haiy
 * @date 2026/08/27
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeckillOrderTimeoutMessage {

    /** 订单号 */
    private String orderNo;

    /** 秒杀商品ID（回补库存用） */
    private Long seckillGoodsId;

    /** 用户ID（回滚 Redis 已购集合用） */
    private Long userId;
}
