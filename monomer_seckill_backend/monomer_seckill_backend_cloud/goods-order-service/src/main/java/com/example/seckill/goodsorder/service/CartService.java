package com.example.seckill.goodsorder.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.seckill.common.core.BizException;
import com.example.seckill.goodsorder.constant.GoodsOrderConstants;
import com.example.seckill.goodsorder.dto.CartItemRequest;
import com.example.seckill.goodsorder.entity.CartItem;
import com.example.seckill.goodsorder.entity.Goods;
import com.example.seckill.goodsorder.mapper.CartItemMapper;
import com.example.seckill.goodsorder.mapper.GoodsMapper;
import com.example.seckill.goodsorder.vo.CartItemVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 购物车服务：加购、列表、改数量、删除。
 *
 * @author haiy
 * @date 2026/08/17
 */
@Service
public class CartService {

    private final CartItemMapper cartItemMapper;
    private final GoodsMapper goodsMapper;

    public CartService(CartItemMapper cartItemMapper, GoodsMapper goodsMapper) {
        this.cartItemMapper = cartItemMapper;
        this.goodsMapper = goodsMapper;
    }

    /**
     * 我的购物车列表（含商品信息）。
     */
    public List<CartItemVO> list(Long userId) {
        List<CartItem> items = cartItemMapper.selectList(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .orderByAsc(CartItem::getId));
        return items.stream().map(this::toVO).toList();
    }

    /**
     * 加入购物车（已存在则累加数量）。
     */
    public void add(Long userId, CartItemRequest request) {
        Goods goods = requireOnSaleGoods(request.getGoodsId());
        int quantity = request.getQuantity() == null ? 1 : request.getQuantity();
        if (quantity <= 0) {
            throw new BizException("购买数量不合法");
        }
        CartItem existing = cartItemMapper.selectOne(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getGoodsId, goods.getId()));
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + quantity);
            cartItemMapper.updateById(existing);
            return;
        }
        CartItem item = new CartItem();
        item.setUserId(userId);
        item.setGoodsId(goods.getId());
        item.setQuantity(quantity);
        cartItemMapper.insert(item);
    }

    /**
     * 修改购物车数量。
     */
    public void updateQuantity(Long userId, Long goodsId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new BizException("购买数量不合法");
        }
        CartItem item = cartItemMapper.selectOne(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getGoodsId, goodsId));
        if (item == null) {
            throw new BizException("购物车中无此商品");
        }
        item.setQuantity(quantity);
        cartItemMapper.updateById(item);
    }

    /**
     * 从购物车移除。
     */
    public void remove(Long userId, Long goodsId) {
        cartItemMapper.delete(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getGoodsId, goodsId));
    }

    /**
     * 清空购物车（下单成功后调用）。
     */
    public void clear(Long userId) {
        cartItemMapper.delete(new LambdaQueryWrapper<CartItem>().eq(CartItem::getUserId, userId));
    }

    /**
     * 校验商品存在且已上架。
     */
    private Goods requireOnSaleGoods(Long goodsId) {
        if (goodsId == null) {
            throw new BizException("商品ID不能为空");
        }
        Goods goods = goodsMapper.selectById(goodsId);
        if (goods == null || goods.getStatus() == null
                || goods.getStatus() != GoodsOrderConstants.GOODS_STATUS_ON) {
            throw new BizException("商品不存在或未上架");
        }
        return goods;
    }

    private CartItemVO toVO(CartItem item) {
        Goods goods = item.getGoodsId() == null ? null : goodsMapper.selectById(item.getGoodsId());
        CartItemVO vo = new CartItemVO();
        vo.setId(item.getId());
        vo.setGoodsId(item.getGoodsId());
        vo.setGoodsName(goods == null ? null : goods.getName());
        vo.setPrice(goods == null ? null : goods.getPrice());
        vo.setStock(goods == null ? null : goods.getStock());
        vo.setQuantity(item.getQuantity());
        if (goods != null && goods.getPrice() != null) {
            vo.setAmount(goods.getPrice().multiply(BigDecimal.valueOf(item.getQuantity() == null ? 1 : item.getQuantity())));
        }
        return vo;
    }
}
