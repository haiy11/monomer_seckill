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
     * 创建秒杀订单（Redis 预扣成功之后调用）：DB 扣减秒杀库存 + 插订单。
     */
    @Transactional
    public SeckillOrder createSeckillOrder(Long seckillGoodsId, Long userId, BigDecimal seckillPrice) {
        int rows = stockService.deductDb(seckillGoodsId);
        if (rows == 0) {
            throw new BizException("手慢了，库存不足");
        }
        SeckillOrder order = new SeckillOrder();
        order.setOrderNo(generateOrderNo("SO"));
        order.setUserId(userId);
        order.setSeckillGoodsId(seckillGoodsId);
        order.setSeckillPrice(seckillPrice);
        order.setStatus(SeckillConstants.ORDER_STATUS_UNPAID);
        seckillOrderMapper.insert(order);
        return order;
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
     * 查询超时未支付秒杀订单。
     */
    public List<SeckillOrder> listTimeoutUnpaid(LocalDateTime deadline) {
        return seckillOrderMapper.selectList(new LambdaQueryWrapper<SeckillOrder>()
                .eq(SeckillOrder::getStatus, SeckillConstants.ORDER_STATUS_UNPAID)
                .lt(SeckillOrder::getCreateTime, deadline));
    }

    /**
     * 超时关闭秒杀订单：回补 DB 与 Redis 库存。
     */
    @Transactional
    public void closeTimeout(Long orderId) {
        SeckillOrder order = seckillOrderMapper.selectById(orderId);
        if (order == null) {
            return;
        }
        int rows = seckillOrderMapper.closeTimeout(orderId, LocalDateTime.now());
        if (rows == 0) {
            return;
        }
        stockService.rollback(order.getSeckillGoodsId(), order.getUserId());
    }

    private String generateOrderNo(String prefix) {
        return prefix + System.currentTimeMillis() + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }
}
