package com.example.seckill.seckill.constant;

/**
 * 秒杀服务 RabbitMQ 交换机 / 队列 / 路由键常量（P8 异步削峰 + 死信队列）。
 *
 * <p>拓扑设计（单个直连交换机承载三种路由）：</p>
 *
 * <pre>
 * seckill.order.exchange (direct)
 *   ├─ seckill.order.create   → seckill.order.queue        下单消息：Redis 预扣成功后异步建单
 *   ├─ seckill.order.delay    → seckill.order.delay.queue  延迟队列：消息带 15 分钟 TTL
 *   │                              │（过期后成为死信，按 x-dead-letter-routing-key 重新投递）
 *   │                              ▼
 *   └─ seckill.order.timeout  → seckill.order.timeout.queue 死信队列：超时未支付自动取消 + 回补库存
 * </pre>
 *
 * <p>「延迟队列 + 死信交换机」是 RabbitMQ 实现延时任务的经典做法：消息发到延迟队列并设置
 * TTL，过期后 RabbitMQ 把它当「死信」投递到 {@code x-dead-letter-routing-key} 对应的队列，
 * 由超时消费者处理。相比定时轮询扫描数据库，消息过期即可触发，无需周期性全表扫描。</p>
 *
 * @author haiy
 * @date 2026/08/27
 */
public final class SeckillMqConstants {

    private SeckillMqConstants() {
    }

    /** 秒杀订单交换机（direct，持久化） */
    public static final String ORDER_EXCHANGE = "seckill.order.exchange";

    /** 下单队列：接收「Redis 预扣成功后」的建单消息 */
    public static final String ORDER_QUEUE = "seckill.order.queue";

    /** 延迟队列：承载带 TTL 的超时消息，过期后投递到死信路由 */
    public static final String ORDER_DELAY_QUEUE = "seckill.order.delay.queue";

    /** 死信队列（超时队列）：接收过期消息，执行超时关闭 + 回补库存 */
    public static final String ORDER_TIMEOUT_QUEUE = "seckill.order.timeout.queue";

    /** 路由键：下单建单 */
    public static final String ORDER_CREATE_KEY = "seckill.order.create";

    /** 路由键：延迟（超时）消息入口 */
    public static final String ORDER_DELAY_KEY = "seckill.order.delay";

    /** 路由键：超时消息出口（延迟队列的死信路由键） */
    public static final String ORDER_TIMEOUT_KEY = "seckill.order.timeout";
}
