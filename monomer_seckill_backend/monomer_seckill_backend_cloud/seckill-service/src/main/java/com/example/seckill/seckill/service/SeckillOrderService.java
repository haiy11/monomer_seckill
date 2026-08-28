package com.example.seckill.seckill.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.seckill.common.core.BizException;
import com.example.seckill.seckill.constant.SeckillConstants;
import com.example.seckill.seckill.entity.SeckillOrder;
import com.example.seckill.seckill.mapper.SeckillOrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 秒杀订单服务：秒杀订单的落库与生命周期（创建/支付/取消/超时关闭）。
 *
 * <p>P8 起「创建订单」改由消息消费者异步调用（削峰解耦），订单号由生产端预生成后随消息传入；
 * 超时关闭改由死信队列（DLX）触发，替代原先的定时轮询扫描。</p>
 *
 * @author haiy
 * @date 2026/08/17
 */
@Service
public class SeckillOrderService {

    private final SeckillOrderMapper seckillOrderMapper;
    private final StockService stockService;

    public SeckillOrderService(SeckillOrderMapper seckillOrderMapper, StockService stockService) {
        this.seckillOrderMapper = seckillOrderMapper;
        this.stockService = stockService;
    }

    /**
     * 创建秒杀订单（消息消费者调用）：DB 扣减秒杀库存 + 插订单（同一事务）。
     *
     * <p>订单号由生产端预生成（用于向用户即时返回），本方法直接落库，保证「Redis 预扣 →
     * 消息 → 建单」全链路可幂等。</p>
     */
    @Transactional
    public SeckillOrder createSeckillOrder(String orderNo, Long seckillGoodsId, Long userId, BigDecimal seckillPrice) {
        int rows = stockService.deductDb(seckillGoodsId);
        if (rows == 0) {
            throw new BizException("手慢了，库存不足");
        }
        SeckillOrder order = new SeckillOrder();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setSeckillGoodsId(seckillGoodsId);
        order.setSeckillPrice(seckillPrice);
        order.setStatus(SeckillConstants.ORDER_STATUS_UNPAID);
        seckillOrderMapper.insert(order);
        return order;
    }

    /**
     * 订单是否已存在（按订单号，消费者幂等判断用）。
     */
    public boolean existsByOrderNo(String orderNo) {
        return seckillOrderMapper.selectCount(new LambdaQueryWrapper<SeckillOrder>()
                .eq(SeckillOrder::getOrderNo, orderNo)) > 0;
    }

    /**
     * 我的秒杀订单列表。
     */
    public List<SeckillOrder> listByUser(Long userId) {
        return seckillOrderMapper.selectList(new LambdaQueryWrapper<SeckillOrder>()
                .eq(SeckillOrder::getUserId, userId)
                .orderByDesc(SeckillOrder::getId));
    }

    /**
     * 全部秒杀订单（管理端查看）。
     */
    public List<SeckillOrder> listAllSeckillOrders() {
        return seckillOrderMapper.selectList(new LambdaQueryWrapper<SeckillOrder>().orderByDesc(SeckillOrder::getId));
    }

    /**
     * 支付秒杀订单。
     */
    @Transactional
    public void pay(String orderNo, Long userId) {
        int rows = seckillOrderMapper.pay(orderNo, userId, LocalDateTime.now());
        if (rows == 0) {
            throw new BizException("订单不存在或状态不允许支付");
        }
    }

    /**
     * 取消秒杀订单：回补 DB 与 Redis 库存。
     */
    @Transactional
    public void cancel(String orderNo, Long userId) {
        SeckillOrder order = seckillOrderMapper.selectOne(new LambdaQueryWrapper<SeckillOrder>()
                .eq(SeckillOrder::getOrderNo, orderNo)
                .eq(SeckillOrder::getUserId, userId));
        if (order == null) {
            throw new BizException("订单不存在");
        }
        int rows = seckillOrderMapper.cancel(orderNo, userId, LocalDateTime.now());
        if (rows == 0) {
            throw new BizException("订单状态不允许取消");
        }
        stockService.rollback(order.getSeckillGoodsId(), order.getUserId());
    }

    /**
     * 超时关闭秒杀订单（死信队列消费者调用）：仅待支付可关闭，关闭后回补 DB 与 Redis 库存。
     *
     * <p>幂等：订单不存在或已非待支付则直接返回，可安全应对消息重复投递。</p>
     */
    @Transactional
    public void closeTimeoutByOrderNo(String orderNo) {
        SeckillOrder order = seckillOrderMapper.selectOne(new LambdaQueryWrapper<SeckillOrder>()
                .eq(SeckillOrder::getOrderNo, orderNo));
        if (order == null) {
            return;
        }
        int rows = seckillOrderMapper.closeTimeout(order.getId(), LocalDateTime.now());
        if (rows == 0) {
            return;
        }
        stockService.rollback(order.getSeckillGoodsId(), order.getUserId());
    }

    /**
     * 生成订单号（生产端预生成，随消息传入消费者）。
     */
    public String generateOrderNo(String prefix) {
        return prefix + System.currentTimeMillis() + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }
}
