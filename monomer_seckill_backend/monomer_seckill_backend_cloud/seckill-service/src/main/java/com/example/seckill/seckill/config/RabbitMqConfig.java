package com.example.seckill.seckill.config;

import com.example.seckill.seckill.constant.SeckillMqConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 秒杀服务 RabbitMQ 配置（P8）：声明交换机 / 队列 / 绑定关系 + JSON 消息序列化。
 *
 * <p>拓扑说明：</p>
 * <ul>
 *   <li><b>下单队列</b> {@code seckill.order.queue}：接收建单消息；</li>
 *   <li><b>延迟队列</b> {@code seckill.order.delay.queue}：消息带 15 分钟 TTL，过期后作为
 *       「死信」投递到 {@link SeckillMqConstants#ORDER_TIMEOUT_KEY}；</li>
 *   <li><b>死信（超时）队列</b> {@code seckill.order.timeout.queue}：消费过期消息执行超时关闭。</li>
 * </ul>
 *
 * <p>消息体统一用 JSON 序列化（{@link Jackson2JsonMessageConverter}），替代默认的 Java 原生
 * 序列化：可读、跨语言、无反序列化安全风险，便于在 RabbitMQ 管理台直接查看消息内容。</p>
 *
 * @author haiy
 * @date 2026/08/27
 */
@Configuration
public class RabbitMqConfig {

    /**
     * 秒杀订单交换机（direct、持久化、非自动删除）。
     */
    @Bean
    public DirectExchange seckillOrderExchange() {
        return new DirectExchange(SeckillMqConstants.ORDER_EXCHANGE, true, false);
    }

    /**
     * 下单队列（持久化）：接收「Redis 预扣成功后」的建单消息。
     */
    @Bean
    public Queue seckillOrderQueue() {
        return QueueBuilder.durable(SeckillMqConstants.ORDER_QUEUE).build();
    }

    /**
     * 延迟队列（持久化）：消息带 TTL，过期后作为死信投递到超时路由。
     *
     * <p>关键参数：{@code x-dead-letter-exchange} 指向主交换机、{@code x-dead-letter-routing-key}
     * 指定超时路由键——这是「延迟队列 + 死信队列」实现延时任务的核心。</p>
     */
    @Bean
    public Queue seckillOrderDelayQueue() {
        return QueueBuilder.durable(SeckillMqConstants.ORDER_DELAY_QUEUE)
                .deadLetterExchange(SeckillMqConstants.ORDER_EXCHANGE)
                .deadLetterRoutingKey(SeckillMqConstants.ORDER_TIMEOUT_KEY)
                .build();
    }

    /**
     * 死信（超时）队列（持久化）：接收延迟队列过期的消息。
     */
    @Bean
    public Queue seckillOrderTimeoutQueue() {
        return QueueBuilder.durable(SeckillMqConstants.ORDER_TIMEOUT_QUEUE).build();
    }

    @Bean
    public Binding seckillOrderCreateBinding(Queue seckillOrderQueue, DirectExchange seckillOrderExchange) {
        return BindingBuilder.bind(seckillOrderQueue).to(seckillOrderExchange)
                .with(SeckillMqConstants.ORDER_CREATE_KEY);
    }

    @Bean
    public Binding seckillOrderDelayBinding(Queue seckillOrderDelayQueue, DirectExchange seckillOrderExchange) {
        return BindingBuilder.bind(seckillOrderDelayQueue).to(seckillOrderExchange)
                .with(SeckillMqConstants.ORDER_DELAY_KEY);
    }

    @Bean
    public Binding seckillOrderTimeoutBinding(Queue seckillOrderTimeoutQueue, DirectExchange seckillOrderExchange) {
        return BindingBuilder.bind(seckillOrderTimeoutQueue).to(seckillOrderExchange)
                .with(SeckillMqConstants.ORDER_TIMEOUT_KEY);
    }

    /**
     * JSON 消息转换器：消息体序列化为 JSON，并携带 {@code __TypeId__} 头供反序列化还原类型。
     *
     * <p>使用 Spring Boot 自动配置的 {@link ObjectMapper}（含 JavaTime 模块），保证 BigDecimal、
     * LocalDateTime 等类型可正确序列化。Spring Boot 会自动把该转换器装配到 {@code RabbitTemplate}
     * 与 {@code SimpleRabbitListenerContainerFactory}，生产消费两侧序列化一致。</p>
     */
    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
