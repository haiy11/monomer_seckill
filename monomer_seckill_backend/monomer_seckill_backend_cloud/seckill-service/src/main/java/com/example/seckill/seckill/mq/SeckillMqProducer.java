package com.example.seckill.seckill.mq;

import com.example.seckill.common.core.BizException;
import com.example.seckill.seckill.constant.SeckillMqConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 秒杀消息生产者（P8）：发送「下单」消息与「延迟超时」消息。
 *
 * <p>削峰解耦的核心：秒杀请求在 Redis 预扣成功后只做「发消息 + 等确认」，DB 落库交给消费者
 * 异步处理，请求线程随即返回，不再同步等待数据库写。</p>
 *
 * @author haiy
 * @date 2026/08/27
 */
@Slf4j
@Component
public class SeckillMqProducer {

    private final RabbitTemplate rabbitTemplate;

    /** 订单超时时间（分钟），用于设置延迟消息 TTL */
    @Value("${seckill.order-timeout-minutes:15}")
    private long timeoutMinutes;

    /** 发布确认超时时间（秒） */
    private static final long CONFIRM_TIMEOUT_SECONDS = 3;

    public SeckillMqProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * 发送下单消息（发布确认：等 Broker 确认落盘后才返回，保证「Redis 已预扣」不丢消息）。
     *
     * <p>若投递失败/未确认则抛出异常，由调用方回滚 Redis 预扣，保证最终一致。</p>
     */
    public void sendCreateOrder(SeckillOrderMessage message) {
        CorrelationData correlationData = new CorrelationData(message.getMessageId());
        rabbitTemplate.convertAndSend(SeckillMqConstants.ORDER_EXCHANGE,
                SeckillMqConstants.ORDER_CREATE_KEY, message, correlationData);
        awaitConfirm(correlationData, message.getMessageId());
        log.info("已投递秒杀下单消息：orderNo={}, goodsId={}, userId={}",
                message.getOrderNo(), message.getSeckillGoodsId(), message.getUserId());
    }

    /**
     * 发送超时消息到延迟队列，并设置 TTL（订单超时分钟数）。
     *
     * <p>消息在延迟队列中滞留 TTL 时长后过期，作为死信投递到超时队列触发自动取消。</p>
     */
    public void sendTimeoutMessage(SeckillOrderTimeoutMessage message) {
        long ttlMillis = TimeUnit.MINUTES.toMillis(timeoutMinutes);
        rabbitTemplate.convertAndSend(SeckillMqConstants.ORDER_EXCHANGE,
                SeckillMqConstants.ORDER_DELAY_KEY, message, m -> {
                    m.getMessageProperties().setExpiration(String.valueOf(ttlMillis));
                    return m;
                });
        log.info("已投递秒杀订单超时消息：orderNo={}, TTL={}ms", message.getOrderNo(), ttlMillis);
    }

    /**
     * 等待 Broker 对消息的发布确认（publisher confirm）。
     */
    private void awaitConfirm(CorrelationData correlationData, String messageId) {
        try {
            CorrelationData.Confirm confirm = correlationData.getFuture().get(CONFIRM_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (confirm == null || !confirm.isAck()) {
                throw new BizException("秒杀下单消息投递未确认");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException("等待秒杀下单消息确认被中断");
        } catch (ExecutionException | TimeoutException e) {
            throw new BizException("等待秒杀下单消息确认异常: " + e.getMessage());
        }
    }
}
