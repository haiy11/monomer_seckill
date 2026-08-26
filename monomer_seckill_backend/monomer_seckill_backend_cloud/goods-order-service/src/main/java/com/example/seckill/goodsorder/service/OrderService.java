package com.example.seckill.goodsorder.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.seckill.common.core.BizException;
import com.example.seckill.goodsorder.constant.GoodsOrderConstants;
import com.example.seckill.goodsorder.entity.CartItem;
import com.example.seckill.goodsorder.entity.Goods;
import com.example.seckill.goodsorder.entity.MallOrder;
import com.example.seckill.goodsorder.entity.OrderItem;
import com.example.seckill.goodsorder.mapper.CartItemMapper;
import com.example.seckill.goodsorder.mapper.GoodsMapper;
import com.example.seckill.goodsorder.mapper.MallOrderMapper;
import com.example.seckill.goodsorder.mapper.OrderItemMapper;
import com.example.seckill.goodsorder.vo.OrderVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 订单服务：正常商品结算、支付、取消。
 *
 * <p>秒杀订单的创建与生命周期已下沉到 seckill-service，本服务只负责正常商品订单。</p>
 *
 * @author haiy
 * @date 2026/08/17
 */
@Service
public class OrderService {

    private final MallOrderMapper mallOrderMapper;
    private final OrderItemMapper orderItemMapper;
    private final CartItemMapper cartItemMapper;
    private final GoodsMapper goodsMapper;
    private final GoodsService goodsService;

    public OrderService(MallOrderMapper mallOrderMapper, OrderItemMapper orderItemMapper,
                        CartItemMapper cartItemMapper, GoodsMapper goodsMapper, GoodsService goodsService) {
        this.mallOrderMapper = mallOrderMapper;
        this.orderItemMapper = orderItemMapper;
        this.cartItemMapper = cartItemMapper;
        this.goodsMapper = goodsMapper;
        this.goodsService = goodsService;
    }

    /**
     * 购物车结算：把购物车内多件商品一次性下单，扣库存、生成订单与明细、清空购物车。
     */
    @Transactional
    public MallOrder checkout(Long userId) {
        List<CartItem> cartItems = cartItemMapper.selectList(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId));
        if (cartItems.isEmpty()) {
            throw new BizException("购物车为空");
        }

        BigDecimal total = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem ci : cartItems) {
            Goods goods = goodsMapper.selectById(ci.getGoodsId());
            if (goods == null || goods.getStatus() == null
                    || goods.getStatus() != GoodsOrderConstants.GOODS_STATUS_ON) {
                throw new BizException("商品已下架：" + (goods != null ? goods.getName() : ci.getGoodsId()));
            }
            int quantity = ci.getQuantity() == null ? 1 : ci.getQuantity();
            if (quantity <= 0) {
                throw new BizException("购买数量不合法");
            }
            int rows = goodsMapper.deductStock(goods.getId(), quantity);
            if (rows == 0) {
                throw new BizException("库存不足：" + goods.getName());
            }
            evictGoodsCacheAfterCommit(goods.getId());
            BigDecimal amount = goods.getPrice().multiply(BigDecimal.valueOf(quantity));
            total = total.add(amount);

            OrderItem oi = new OrderItem();
            oi.setGoodsId(goods.getId());
            oi.setGoodsName(goods.getName());
            oi.setPrice(goods.getPrice());
            oi.setQuantity(quantity);
            oi.setAmount(amount);
            orderItems.add(oi);
        }

        MallOrder order = new MallOrder();
        order.setOrderNo(generateOrderNo("NO"));
        order.setUserId(userId);
        order.setTotalAmount(total);
        order.setStatus(GoodsOrderConstants.ORDER_STATUS_UNPAID);
        mallOrderMapper.insert(order);
        for (OrderItem oi : orderItems) {
            oi.setOrderId(order.getId());
            orderItemMapper.insert(oi);
        }
        cartItemMapper.delete(new LambdaQueryWrapper<CartItem>().eq(CartItem::getUserId, userId));
        return order;
    }

    /**
     * 我的正常订单列表。
     */
    public List<MallOrder> listNormalByUser(Long userId) {
        return mallOrderMapper.selectList(new LambdaQueryWrapper<MallOrder>()
                .eq(MallOrder::getUserId, userId)
                .orderByDesc(MallOrder::getId));
    }

    /**
     * 全部正常订单（管理端查看）。
     */
    public List<MallOrder> listAllNormal() {
        return mallOrderMapper.selectList(new LambdaQueryWrapper<MallOrder>().orderByDesc(MallOrder::getId));
    }

    /**
     * 正常订单详情（含明细）。
     */
    public OrderVO getNormalOrder(String orderNo, Long userId) {
        MallOrder order = mallOrderMapper.selectOne(new LambdaQueryWrapper<MallOrder>()
                .eq(MallOrder::getOrderNo, orderNo)
                .eq(MallOrder::getUserId, userId));
        if (order == null) {
            throw new BizException("订单不存在");
        }
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setUserId(order.getUserId());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setStatus(order.getStatus());
        vo.setCreateTime(order.getCreateTime());
        vo.setPayTime(order.getPayTime());
        vo.setItems(orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId())));
        return vo;
    }

    /**
     * 支付正常订单。
     */
    @Transactional
    public void payNormal(String orderNo, Long userId) {
        int rows = mallOrderMapper.pay(orderNo, userId, LocalDateTime.now());
        if (rows == 0) {
            throw new BizException("订单不存在或状态不允许支付");
        }
    }

    /**
     * 取消正常订单（回补各明细商品库存）。
     */
    @Transactional
    public void cancelNormal(String orderNo, Long userId) {
        MallOrder order = mallOrderMapper.selectOne(new LambdaQueryWrapper<MallOrder>()
                .eq(MallOrder::getOrderNo, orderNo)
                .eq(MallOrder::getUserId, userId));
        if (order == null) {
            throw new BizException("订单不存在");
        }
        int rows = mallOrderMapper.cancel(orderNo, userId, LocalDateTime.now());
        if (rows == 0) {
            throw new BizException("订单状态不允许取消");
        }
        restoreNormalStock(order.getId());
    }

    private void restoreNormalStock(Long orderId) {
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId));
        for (OrderItem oi : items) {
            goodsMapper.restoreStock(oi.getGoodsId(), oi.getQuantity());
            evictGoodsCacheAfterCommit(oi.getGoodsId());
        }
    }

    /**
     * 事务提交后再删除商品缓存（Cache-Aside 严格时序）。
     *
     * <p>扣减/回补库存属于事务内的写操作，若在事务提交前删除缓存，期间其它请求可能
     * 回源读到未提交的旧数据并重新写入缓存，导致缓存与 DB 不一致。因此把缓存删除
     * 推迟到事务提交后执行；无活动事务时（独立调用）直接删除。</p>
     */
    private void evictGoodsCacheAfterCommit(Long goodsId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    goodsService.evictCache(goodsId);
                }
            });
        } else {
            goodsService.evictCache(goodsId);
        }
    }

    // ==================== 超时回滚 ====================

    public List<MallOrder> listNormalTimeoutUnpaid(LocalDateTime deadline) {
        return mallOrderMapper.selectList(new LambdaQueryWrapper<MallOrder>()
                .eq(MallOrder::getStatus, GoodsOrderConstants.ORDER_STATUS_UNPAID)
                .lt(MallOrder::getCreateTime, deadline));
    }

    @Transactional
    public void closeNormalTimeout(Long orderId) {
        MallOrder order = mallOrderMapper.selectById(orderId);
        if (order == null) {
            return;
        }
        int rows = mallOrderMapper.closeTimeout(orderId, LocalDateTime.now());
        if (rows == 0) {
            return;
        }
        restoreNormalStock(orderId);
    }

    private String generateOrderNo(String prefix) {
        return prefix + System.currentTimeMillis() + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }
}
