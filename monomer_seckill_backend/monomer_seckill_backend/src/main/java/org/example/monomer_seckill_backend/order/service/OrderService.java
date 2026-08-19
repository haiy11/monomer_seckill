package org.example.monomer_seckill_backend.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.monomer_seckill_backend.cart.entity.CartItem;
import org.example.monomer_seckill_backend.cart.mapper.CartItemMapper;
import org.example.monomer_seckill_backend.common.BizException;
import org.example.monomer_seckill_backend.common.Constants;
import org.example.monomer_seckill_backend.goods.entity.Goods;
import org.example.monomer_seckill_backend.goods.mapper.GoodsMapper;
import org.example.monomer_seckill_backend.goods.service.StockService;
import org.example.monomer_seckill_backend.order.entity.MallOrder;
import org.example.monomer_seckill_backend.order.entity.OrderItem;
import org.example.monomer_seckill_backend.order.entity.SeckillOrder;
import org.example.monomer_seckill_backend.order.mapper.MallOrderMapper;
import org.example.monomer_seckill_backend.order.mapper.OrderItemMapper;
import org.example.monomer_seckill_backend.order.mapper.SeckillOrderMapper;
import org.example.monomer_seckill_backend.order.vo.OrderVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 订单服务：正常商品结算、支付、取消，以及秒杀订单的创建与生命周期。
 *
 * @author haiy
 * @date 2026/08/17
 */
@Service
public class OrderService {

    private final MallOrderMapper mallOrderMapper;
    private final OrderItemMapper orderItemMapper;
    private final SeckillOrderMapper seckillOrderMapper;
    private final CartItemMapper cartItemMapper;
    private final GoodsMapper goodsMapper;
    private final StockService stockService;

    public OrderService(MallOrderMapper mallOrderMapper, OrderItemMapper orderItemMapper,
                        SeckillOrderMapper seckillOrderMapper, CartItemMapper cartItemMapper,
                        GoodsMapper goodsMapper, StockService stockService) {
        this.mallOrderMapper = mallOrderMapper;
        this.orderItemMapper = orderItemMapper;
        this.seckillOrderMapper = seckillOrderMapper;
        this.cartItemMapper = cartItemMapper;
        this.goodsMapper = goodsMapper;
        this.stockService = stockService;
    }

    // ==================== 正常商品下单 ====================

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
            if (goods == null || goods.getStatus() == null || goods.getStatus() != Constants.GOODS_STATUS_ON) {
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
        order.setStatus(Constants.ORDER_STATUS_UNPAID);
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
        }
    }

    // ==================== 秒杀订单 ====================

    /**
     * 创建秒杀订单（Redis 预扣成功之后调用）：扣减 DB 秒杀库存 + 插订单。
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
        order.setStatus(Constants.ORDER_STATUS_UNPAID);
        seckillOrderMapper.insert(order);
        return order;
    }

    /**
     * 我的秒杀订单列表。
     */
    public List<SeckillOrder> listSeckillByUser(Long userId) {
        return seckillOrderMapper.selectList(new LambdaQueryWrapper<SeckillOrder>()
                .eq(SeckillOrder::getUserId, userId)
                .orderByDesc(SeckillOrder::getId));
    }

    /**
     * 支付秒杀订单。
     */
    @Transactional
    public void paySeckill(String orderNo, Long userId) {
        int rows = seckillOrderMapper.pay(orderNo, userId, LocalDateTime.now());
        if (rows == 0) {
            throw new BizException("订单不存在或状态不允许支付");
        }
    }

    /**
     * 取消秒杀订单（回补秒杀库存）。
     */
    @Transactional
    public void cancelSeckill(String orderNo, Long userId) {
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
        stockService.rollback(order.getSeckillGoodsId(), userId);
    }

    // ==================== 管理端 ====================

    public List<MallOrder> listAllNormal() {
        return mallOrderMapper.selectList(new LambdaQueryWrapper<MallOrder>().orderByDesc(MallOrder::getId));
    }

    public List<SeckillOrder> listAllSeckill() {
        return seckillOrderMapper.selectList(new LambdaQueryWrapper<SeckillOrder>().orderByDesc(SeckillOrder::getId));
    }

    // ==================== 超时回滚 ====================

    public List<MallOrder> listNormalTimeoutUnpaid(LocalDateTime deadline) {
        return mallOrderMapper.selectList(new LambdaQueryWrapper<MallOrder>()
                .eq(MallOrder::getStatus, Constants.ORDER_STATUS_UNPAID)
                .lt(MallOrder::getCreateTime, deadline));
    }

    public List<SeckillOrder> listSeckillTimeoutUnpaid(LocalDateTime deadline) {
        return seckillOrderMapper.selectList(new LambdaQueryWrapper<SeckillOrder>()
                .eq(SeckillOrder::getStatus, Constants.ORDER_STATUS_UNPAID)
                .lt(SeckillOrder::getCreateTime, deadline));
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

    @Transactional
    public void closeSeckillTimeout(Long orderId) {
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
