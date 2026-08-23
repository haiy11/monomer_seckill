package com.example.seckill.seckill.task;

import com.example.seckill.seckill.entity.SeckillOrder;
import com.example.seckill.seckill.service.SeckillOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 秒杀订单超时回滚定时任务。
 *
 * @author haiy
 * @date 2026/08/17
 */
@Slf4j
@Component
public class SeckillOrderTimeoutTask {

    private final SeckillOrderService seckillOrderService;

    /** 订单超时时间（分钟） */
    @Value("${seckill.order-timeout-minutes:15}")
    private long timeoutMinutes;

    public SeckillOrderTimeoutTask(SeckillOrderService seckillOrderService) {
        this.seckillOrderService = seckillOrderService;
    }

    /**
     * 每 30 秒扫描一次超时未支付秒杀订单，自动关闭并回补库存。
     */
    @Scheduled(fixedDelayString = "${seckill.order-timeout-scan-ms:30000}")
    public void closeTimeoutOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(timeoutMinutes);
        for (SeckillOrder order : seckillOrderService.listTimeoutUnpaid(deadline)) {
            try {
                seckillOrderService.closeTimeout(order.getId());
                log.info("秒杀订单 {} 超时关闭，已回补库存", order.getOrderNo());
            } catch (Exception e) {
                log.error("关闭超时秒杀订单 {} 失败", order.getOrderNo(), e);
            }
        }
    }
}
