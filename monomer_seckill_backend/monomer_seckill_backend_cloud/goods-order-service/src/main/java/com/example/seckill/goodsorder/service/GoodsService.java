package com.example.seckill.goodsorder.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.seckill.common.cache.MultiLevelCache;
import com.example.seckill.common.core.BizException;
import com.example.seckill.goodsorder.constant.GoodsOrderConstants;
import com.example.seckill.goodsorder.entity.Goods;
import com.example.seckill.goodsorder.mapper.GoodsMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 商品服务（公开查询，商品详情走多级缓存）。
 *
 * <p>商品详情是高频读路径，走「Caffeine → Redis → DB」多级缓存；缓存更新采用
 * Cache-Aside：商品审核/上下架/库存变化后删除缓存。</p>
 *
 * @author haiy
 * @date 2026/08/17
 */
@Service
public class GoodsService {

    private final GoodsMapper goodsMapper;
    private final MultiLevelCache<Long, Goods> goodsCache;

    public GoodsService(GoodsMapper goodsMapper, MultiLevelCache<Long, Goods> goodsCache) {
        this.goodsMapper = goodsMapper;
        this.goodsCache = goodsCache;
    }

    /**
     * 查询已上架商品列表（用户端正常商品页）。
     */
    public List<Goods> listOnSale() {
        return goodsMapper.selectList(new LambdaQueryWrapper<Goods>()
                .eq(Goods::getStatus, GoodsOrderConstants.GOODS_STATUS_ON)
                .orderByAsc(Goods::getId));
    }

    /**
     * 按 ID 查询商品（走 Caffeine → Redis → DB 多级缓存）。
     */
    public Goods getById(Long id) {
        return goodsCache.get(id);
    }

    /**
     * 删除商品缓存（商品审核/上下架/库存变化后调用：删本地 + 删分布式 + 广播其它实例删本地）。
     */
    public void evictCache(Long id) {
        goodsCache.evict(id);
    }

    /**
     * 查询商品，不存在时抛异常。
     */
    public Goods requireGoods(Long id) {
        Goods goods = getById(id);
        if (goods == null) {
            throw new BizException("商品不存在");
        }
        return goods;
    }
}
