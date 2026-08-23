package com.example.seckill.goodsorder.task;

import com.example.seckill.goodsorder.entity.MallOrder;
import com.example.seckill.goodsorder.entity.SeckillOrder;
import com.example.seckill.goodsorder.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 订单超时回滚定时任务（正常订单 + 秒杀订单）。
 *
 * @author haiy
 * @date 2026/08/17
 */
@Slf4j
@Component
public class OrderTimeoutTask {

    private final OrderService orderService;

    /** 订单超时时间（分钟） */
    @Value("${seckill.order-timeout-minutes:15}")
    private long timeoutMinutes;

    public OrderTimeoutTask(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 每 30 秒扫描一次超时未支付订单，自动关闭并回补库存。
     */
    @Scheduled(fixedDelayString = "${seckill.order-timeout-scan-ms:30000}")
    public void closeTimeoutOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(timeoutMinutes);

        for (MallOrder order : orderService.listNormalTimeoutUnpaid(deadline)) {
            try {
                orderService.closeNormalTimeout(order.getId());
                log.info("正常订单 {} 超时关闭，已回补库存", order.getOrderNo());
            } catch (Exception e) {
                log.error("关闭超时正常订单 {} 失败", order.getOrderNo(), e);
            }
        }

        for (SeckillOrder order : orderService.listSeckillTimeoutUnpaid(deadline)) {
            try {
                orderService.closeSeckillTimeout(order.getId());
                log.info("秒杀订单 {} 超时关闭，已回补库存", order.getOrderNo());
            } catch (Exception e) {
                log.error("关闭超时秒杀订单 {} 失败", order.getOrderNo(), e);
            }
        }
    }
}
