package com.example.seckill.seckill.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.seckill.common.cache.MultiLevelCache;
import com.example.seckill.common.core.BizException;
import com.example.seckill.seckill.constant.SeckillConstants;
import com.example.seckill.seckill.entity.SeckillGoods;
import com.example.seckill.seckill.mapper.SeckillGoodsMapper;
import com.example.seckill.seckill.vo.SeckillGoodsVO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 秒杀商品服务：查询、状态维护（含多级缓存），秒杀域专属。
 *
 * <p>热点秒杀商品信息走「Caffeine 本地 → Redis 分布式 → DB」多级缓存，大幅降低高并发
 * 读场景下的 Redis/DB 压力。缓存更新采用 Cache-Aside：更新 DB 后删除缓存。</p>
 *
 * @author haiy
 * @date 2026/08/17
 */
@Service
public class SeckillGoodsService {

    private final SeckillGoodsMapper seckillGoodsMapper;
    private final MultiLevelCache<Long, SeckillGoods> seckillGoodsCache;

    public SeckillGoodsService(SeckillGoodsMapper seckillGoodsMapper,
                               MultiLevelCache<Long, SeckillGoods> seckillGoodsCache) {
        this.seckillGoodsMapper = seckillGoodsMapper;
        this.seckillGoodsCache = seckillGoodsCache;
    }

    /**
     * 查询已上架秒杀商品（用户端秒杀商品页）。
     */
    public List<SeckillGoodsVO> listOnSale() {
        List<SeckillGoods> list = seckillGoodsMapper.selectList(new LambdaQueryWrapper<SeckillGoods>()
                .eq(SeckillGoods::getStatus, SeckillConstants.SECKILL_STATUS_ON)
                .orderByAsc(SeckillGoods::getId));
        return list.stream().map(this::toVO).toList();
    }

    /**
     * 查询单个秒杀商品（含商品信息，走多级缓存）。
     */
    public SeckillGoodsVO getVO(Long id) {
        SeckillGoods sg = getByIdCached(id);
        return sg == null ? null : toVO(sg);
    }

    /**
     * 按 ID 查询秒杀商品实体（直查 DB，管理端/内部流程需最新数据时使用）。
     */
    public SeckillGoods getById(Long id) {
        return id == null ? null : seckillGoodsMapper.selectById(id);
    }

    /**
     * 按 ID 查询秒杀商品实体（走 Caffeine → Redis → DB 多级缓存）。
     */
    public SeckillGoods getByIdCached(Long id) {
        return seckillGoodsCache.get(id);
    }

    /**
     * 删除秒杀商品缓存（更新 DB 后调用：删本地 + 删分布式 + 广播其它实例删本地）。
     */
    public void evictCache(Long id) {
        seckillGoodsCache.evict(id);
    }

    /**
     * 查询某商家的秒杀商品。
     */
    public List<SeckillGoods> listByMerchant(Long merchantId) {
        return seckillGoodsMapper.selectList(new LambdaQueryWrapper<SeckillGoods>()
                .eq(SeckillGoods::getMerchantId, merchantId)
                .orderByDesc(SeckillGoods::getId));
    }

    /**
     * 查询全部秒杀商品（管理端）。
     */
    public List<SeckillGoods> listAll() {
        return seckillGoodsMapper.selectList(new LambdaQueryWrapper<SeckillGoods>()
                .orderByDesc(SeckillGoods::getId));
    }

    /**
     * 实体转视图对象（计算秒杀状态）。
     */
    public SeckillGoodsVO toVO(SeckillGoods sg) {
        SeckillGoodsVO vo = new SeckillGoodsVO();
        vo.setId(sg.getId());
        vo.setMerchantId(sg.getMerchantId());
        vo.setName(sg.getName());
        vo.setSeckillPrice(sg.getSeckillPrice());
        vo.setSeckillStock(sg.getSeckillStock());
        vo.setStartTime(sg.getStartTime());
        vo.setEndTime(sg.getEndTime());
        vo.setStatus(sg.getStatus());
        vo.setSeckillState(calcState(sg));
        return vo;
    }

    /**
     * 计算秒杀状态：0-未开始 1-进行中 2-已结束。
     */
    private int calcState(SeckillGoods sg) {
        if (sg.getStartTime() == null || sg.getEndTime() == null) {
            return 2;
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(sg.getStartTime())) {
            return 0;
        }
        if (now.isAfter(sg.getEndTime())) {
            return 2;
        }
        return 1;
    }

    /**
     * 校验秒杀商品是否处于「已上架且可购买」状态。
     */
    public SeckillGoods requirePurchasable(Long id) {
        SeckillGoods sg = getByIdCached(id);
        if (sg == null) {
            throw new BizException("秒杀商品不存在");
        }
        if (sg.getStatus() == null || sg.getStatus() != SeckillConstants.SECKILL_STATUS_ON) {
            throw new BizException("秒杀商品未上架");
        }
        LocalDateTime now = LocalDateTime.now();
        if (sg.getStartTime() != null && now.isBefore(sg.getStartTime())) {
            throw new BizException("秒杀尚未开始");
        }
        if (sg.getEndTime() != null && now.isAfter(sg.getEndTime())) {
            throw new BizException("秒杀已结束");
        }
        return sg;
    }
}
