package com.example.seckill.goodsorder.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.seckill.common.core.BizException;
import com.example.seckill.goodsorder.constant.GoodsOrderConstants;
import com.example.seckill.goodsorder.entity.Goods;
import com.example.seckill.goodsorder.mapper.GoodsMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 商品管理服务（供管理员审核、商家管理调用，经内部接口暴露给 user-service）。
 *
 * @author haiy
 * @date 2026/08/17
 */
@Service
public class GoodsManageService {

    private final GoodsMapper goodsMapper;

    public GoodsManageService(GoodsMapper goodsMapper) {
        this.goodsMapper = goodsMapper;
    }

    // ==================== 管理员：商品审核 ====================

    public List<Goods> listAllGoods() {
        return goodsMapper.selectList(new LambdaQueryWrapper<Goods>().orderByDesc(Goods::getId));
    }

    /**
     * 通过商品审核：待审核 → 已上架。
     */
    public void approveGoods(Long id) {
        Goods goods = requirePendingGoods(id);
        goods.setStatus(GoodsOrderConstants.GOODS_STATUS_ON);
        goodsMapper.updateById(goods);
    }

    /**
     * 拒绝商品审核：待审核 → 已拒绝。
     */
    public void rejectGoods(Long id) {
        Goods goods = requirePendingGoods(id);
        goods.setStatus(GoodsOrderConstants.GOODS_STATUS_REJECTED);
        goodsMapper.updateById(goods);
    }

    private Goods requirePendingGoods(Long id) {
        Goods goods = goodsMapper.selectById(id);
        if (goods == null) {
            throw new BizException("商品不存在");
        }
        if (goods.getStatus() == null || goods.getStatus() != GoodsOrderConstants.GOODS_STATUS_PENDING) {
            throw new BizException("该商品已处理");
        }
        return goods;
    }

    // ==================== 商家：商品管理 ====================

    public List<Goods> listMerchantGoods(Long merchantId) {
        return goodsMapper.selectList(new LambdaQueryWrapper<Goods>()
                .eq(Goods::getMerchantId, merchantId)
                .orderByDesc(Goods::getId));
    }

    /**
     * 新增商品（提交后待管理员审核）。
     */
    public Goods addGoods(Long merchantId, Goods goods) {
        validateGoods(goods);
        goods.setId(null);
        goods.setMerchantId(merchantId);
        goods.setStatus(GoodsOrderConstants.GOODS_STATUS_PENDING);
        goodsMapper.insert(goods);
        return goods;
    }

    /**
     * 修改商品（被拒绝的商品编辑后重新提交审核）。
     */
    public Goods updateGoods(Long merchantId, Long goodsId, Goods goods) {
        Goods db = requireOwnGoods(merchantId, goodsId);
        validateGoods(goods);
        db.setName(goods.getName());
        db.setDescription(goods.getDescription());
        db.setPrice(goods.getPrice());
        db.setStock(goods.getStock());
        db.setImageUrl(goods.getImageUrl());
        if (db.getStatus() != null && db.getStatus() == GoodsOrderConstants.GOODS_STATUS_REJECTED) {
            db.setStatus(GoodsOrderConstants.GOODS_STATUS_PENDING);
        }
        goodsMapper.updateById(db);
        return db;
    }

    /**
     * 上架 / 下架商品（仅已通过审核的商品可操作）。
     */
    public void updateGoodsStatus(Long merchantId, Long goodsId, Integer status) {
        if (status == null || (status != GoodsOrderConstants.GOODS_STATUS_ON && status != GoodsOrderConstants.GOODS_STATUS_OFF)) {
            throw new BizException("非法的商品状态");
        }
        Goods db = requireOwnGoods(merchantId, goodsId);
        if (db.getStatus() == null
                || db.getStatus() == GoodsOrderConstants.GOODS_STATUS_PENDING
                || db.getStatus() == GoodsOrderConstants.GOODS_STATUS_REJECTED) {
            throw new BizException("商品尚未通过审核，不能上下架");
        }
        db.setStatus(status);
        goodsMapper.updateById(db);
    }

    private Goods requireOwnGoods(Long merchantId, Long goodsId) {
        if (goodsId == null) {
            throw new BizException("商品ID不能为空");
        }
        Goods goods = goodsMapper.selectById(goodsId);
        if (goods == null || !goods.getMerchantId().equals(merchantId)) {
            throw new BizException("商品不存在或非本商家商品");
        }
        return goods;
    }

    private void validateGoods(Goods goods) {
        if (goods.getName() == null || goods.getName().isBlank()) {
            throw new BizException("商品名称不能为空");
        }
        if (goods.getPrice() == null || goods.getPrice().doubleValue() < 0) {
            throw new BizException("商品价格不合法");
        }
        if (goods.getStock() == null || goods.getStock() < 0) {
            throw new BizException("库存不合法");
        }
    }
}
