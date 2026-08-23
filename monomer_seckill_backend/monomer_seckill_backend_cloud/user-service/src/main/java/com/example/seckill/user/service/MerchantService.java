package com.example.seckill.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.seckill.common.core.BizException;
import com.example.seckill.common.redis.RedisUtil;
import com.example.seckill.user.constant.UserConstants;
import com.example.seckill.user.dto.LoginRequest;
import com.example.seckill.user.dto.SeckillApplyRequest;
import com.example.seckill.user.entity.Goods;
import com.example.seckill.user.entity.SeckillGoods;
import com.example.seckill.user.mapper.GoodsMapper;
import com.example.seckill.user.mapper.SeckillGoodsMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 商家服务：我的商品管理、秒杀商品申请与管理。
 *
 * <p>秒杀商品与正常商品相互独立，商家直接填写秒杀商品信息，无需关联正常商品。</p>
 *
 * @author haiy
 * @date 2026/08/17
 */
@Service
public class MerchantService {

    private final GoodsMapper goodsMapper;
    private final SeckillGoodsMapper seckillGoodsMapper;
    private final RedisUtil redisUtil;

    public MerchantService(GoodsMapper goodsMapper, SeckillGoodsMapper seckillGoodsMapper, RedisUtil redisUtil) {
        this.goodsMapper = goodsMapper;
        this.seckillGoodsMapper = seckillGoodsMapper;
        this.redisUtil = redisUtil;
    }

    /**
     * 我的商品列表。
     */
    public List<Goods> listMyGoods(Long merchantId) {
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
        goods.setStatus(UserConstants.GOODS_STATUS_PENDING);
        goodsMapper.insert(goods);
        return goods;
    }

    /**
     * 修改我的商品（被拒绝的商品编辑后重新提交审核）。
     */
    public Goods updateGoods(Long merchantId, Long goodsId, Goods goods) {
        Goods db = requireOwnGoods(merchantId, goodsId);
        validateGoods(goods);
        db.setName(goods.getName());
        db.setDescription(goods.getDescription());
        db.setPrice(goods.getPrice());
        db.setStock(goods.getStock());
        db.setImageUrl(goods.getImageUrl());
        if (db.getStatus() != null && db.getStatus() == UserConstants.GOODS_STATUS_REJECTED) {
            db.setStatus(UserConstants.GOODS_STATUS_PENDING);
        }
        goodsMapper.updateById(db);
        return db;
    }

    /**
     * 上架 / 下架我的商品（仅已通过审核的商品可操作）。
     */
    public void updateGoodsStatus(Long merchantId, Long goodsId, Integer status) {
        if (status == null || (status != UserConstants.GOODS_STATUS_ON && status != UserConstants.GOODS_STATUS_OFF)) {
            throw new BizException("非法的商品状态");
        }
        Goods db = requireOwnGoods(merchantId, goodsId);
        if (db.getStatus() == null
                || db.getStatus() == UserConstants.GOODS_STATUS_PENDING
                || db.getStatus() == UserConstants.GOODS_STATUS_REJECTED) {
            throw new BizException("商品尚未通过审核，不能上下架");
        }
        db.setStatus(status);
        goodsMapper.updateById(db);
    }

    /**
     * 申请新建秒杀商品（与正常商品独立，待管理员审核）。
     */
    public SeckillGoods applySeckill(Long merchantId, SeckillApplyRequest request) {
        validateSeckill(request);
        SeckillGoods sg = new SeckillGoods();
        sg.setMerchantId(merchantId);
        sg.setName(request.getName().trim());
        sg.setSeckillPrice(request.getSeckillPrice());
        sg.setSeckillStock(request.getSeckillStock());
        sg.setStartTime(request.getStartTime());
        sg.setEndTime(request.getEndTime());
        sg.setStatus(UserConstants.SECKILL_STATUS_PENDING);
        seckillGoodsMapper.insert(sg);
        return sg;
    }

    /**
     * 编辑秒杀商品（被拒绝的编辑后重新提交审核）。
     */
    public SeckillGoods updateSeckillGoods(Long merchantId, Long seckillGoodsId, SeckillApplyRequest request) {
        SeckillGoods sg = requireOwnSeckillGoods(merchantId, seckillGoodsId);
        validateSeckill(request);
        sg.setName(request.getName().trim());
        sg.setSeckillPrice(request.getSeckillPrice());
        sg.setSeckillStock(request.getSeckillStock());
        sg.setStartTime(request.getStartTime());
        sg.setEndTime(request.getEndTime());
        if (sg.getStatus() != null && sg.getStatus() == UserConstants.SECKILL_STATUS_REJECTED) {
            sg.setStatus(UserConstants.SECKILL_STATUS_PENDING);
        }
        seckillGoodsMapper.updateById(sg);
        evictCache(seckillGoodsId);
        return sg;
    }

    /**
     * 我的秒杀商品列表。
     */
    public List<SeckillGoods> listMySeckill(Long merchantId) {
        return seckillGoodsMapper.selectList(new LambdaQueryWrapper<SeckillGoods>()
                .eq(SeckillGoods::getMerchantId, merchantId)
                .orderByDesc(SeckillGoods::getId));
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

    private SeckillGoods requireOwnSeckillGoods(Long merchantId, Long seckillGoodsId) {
        if (seckillGoodsId == null) {
            throw new BizException("秒杀商品ID不能为空");
        }
        SeckillGoods sg = seckillGoodsMapper.selectById(seckillGoodsId);
        if (sg == null || !sg.getMerchantId().equals(merchantId)) {
            throw new BizException("秒杀商品不存在或非本商家");
        }
        return sg;
    }

    /**
     * 删除秒杀商品缓存（商品信息变更后调用，保证缓存与数据库一致）。
     */
    private void evictCache(Long id) {
        if (id != null) {
            redisUtil.delete(UserConstants.GOODS_KEY_PREFIX + id);
        }
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

    private void validateSeckill(SeckillApplyRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BizException("秒杀商品名称不能为空");
        }
        if (request.getSeckillPrice() == null || request.getSeckillPrice().doubleValue() < 0) {
            throw new BizException("秒杀价不合法");
        }
        if (request.getSeckillStock() == null || request.getSeckillStock() <= 0) {
            throw new BizException("秒杀库存必须大于 0");
        }
        if (request.getStartTime() == null || request.getEndTime() == null
                || !request.getEndTime().isAfter(request.getStartTime())) {
            throw new BizException("秒杀时间段不合法");
        }
        if (request.getEndTime().isBefore(LocalDateTime.now())) {
            throw new BizException("秒杀结束时间不能早于当前时间");
        }
    }
}
