package com.example.seckill.user.service;

import com.example.seckill.common.core.BizException;
import com.example.seckill.common.core.Result;
import com.example.seckill.user.client.GoodsOrderClient;
import com.example.seckill.user.client.SeckillClient;
import com.example.seckill.user.dto.SeckillApplyRequest;
import com.example.seckill.user.entity.Goods;
import com.example.seckill.user.entity.SeckillGoods;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 商家服务：我的商品管理、秒杀商品申请与管理（经 Feign 调对应服务）。
 *
 * @author haiy
 * @date 2026/08/17
 */
@Service
public class MerchantService {

    private final GoodsOrderClient goodsOrderClient;
    private final SeckillClient seckillClient;

    public MerchantService(GoodsOrderClient goodsOrderClient, SeckillClient seckillClient) {
        this.goodsOrderClient = goodsOrderClient;
        this.seckillClient = seckillClient;
    }

    // ==================== 商品管理（Feign → goods-order-service） ====================

    public List<Goods> listMyGoods(Long merchantId) {
        return unwrap(goodsOrderClient.listMerchantGoods(merchantId));
    }

    public Goods addGoods(Long merchantId, Goods goods) {
        return unwrap(goodsOrderClient.addGoods(merchantId, goods));
    }

    /**
     * 修改我的商品（被拒绝的商品编辑后重新提交审核）。
     */
    public Goods updateGoods(Long merchantId, Long goodsId, Goods goods) {
        return unwrap(goodsOrderClient.updateGoods(merchantId, goodsId, goods));
    }

    /**
     * 上架 / 下架我的商品（仅已通过审核的商品可操作）。
     */
    public void updateGoodsStatus(Long merchantId, Long goodsId, Integer status) {
        unwrap(goodsOrderClient.updateGoodsStatus(merchantId, goodsId, status));
    }

    // ==================== 秒杀商品管理（Feign → seckill-service） ====================

    public SeckillGoods applySeckill(Long merchantId, SeckillApplyRequest request) {
        return unwrap(seckillClient.applySeckill(merchantId, request));
    }

    /**
     * 编辑秒杀商品（被拒绝的编辑后重新提交审核）。
     */
    public SeckillGoods updateSeckillGoods(Long merchantId, Long seckillGoodsId, SeckillApplyRequest request) {
        return unwrap(seckillClient.updateSeckillGoods(merchantId, seckillGoodsId, request));
    }

    /**
     * 我的秒杀商品列表。
     */
    public List<SeckillGoods> listMySeckill(Long merchantId) {
        return unwrap(seckillClient.listMerchantSeckillGoods(merchantId));
    }

    /**
     * 解析 Feign 返回的统一响应体，失败时转为业务异常。
     */
    private <T> T unwrap(Result<T> result) {
        if (result == null || result.getCode() == null || result.getCode() != 200) {
            throw new BizException(result == null ? "远程调用失败" : result.getMsg());
        }
        return result.getData();
    }
}
