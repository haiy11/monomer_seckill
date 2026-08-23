package com.example.seckill.seckill.service;

import com.example.seckill.common.core.BizException;
import com.example.seckill.seckill.constant.SeckillConstants;
import com.example.seckill.seckill.dto.SeckillApplyRequest;
import com.example.seckill.seckill.entity.SeckillGoods;
import com.example.seckill.seckill.mapper.SeckillGoodsMapper;
import com.example.seckill.seckill.vo.SeckillGoodsVO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 秒杀商品管理服务（供管理员审核、商家申请/管理调用，经内部接口暴露给 user-service）。
 *
 * @author haiy
 * @date 2026/08/17
 */
@Service
public class SeckillGoodsManageService {

    private final SeckillGoodsMapper seckillGoodsMapper;
    private final SeckillGoodsService seckillGoodsService;
    private final StockService stockService;

    public SeckillGoodsManageService(SeckillGoodsMapper seckillGoodsMapper,
                                     SeckillGoodsService seckillGoodsService, StockService stockService) {
        this.seckillGoodsMapper = seckillGoodsMapper;
        this.seckillGoodsService = seckillGoodsService;
        this.stockService = stockService;
    }

    // ==================== 管理员：秒杀商品审核 ====================

    public List<SeckillGoodsVO> listAllSeckillGoods() {
        return seckillGoodsService.listAll().stream().map(seckillGoodsService::toVO).toList();
    }

    /**
     * 通过秒杀商品审核：待审核 → 已上架，并预载秒杀库存到 Redis。
     */
    public void approveSeckillGoods(Long id) {
        SeckillGoods sg = requirePendingSeckillGoods(id);
        sg.setStatus(SeckillConstants.SECKILL_STATUS_ON);
        seckillGoodsMapper.updateById(sg);
        seckillGoodsService.evictCache(id);
        stockService.preload(id);
    }

    /**
     * 拒绝秒杀商品审核。
     */
    public void rejectSeckillGoods(Long id) {
        SeckillGoods sg = requirePendingSeckillGoods(id);
        sg.setStatus(SeckillConstants.SECKILL_STATUS_REJECTED);
        seckillGoodsMapper.updateById(sg);
        seckillGoodsService.evictCache(id);
    }

    /**
     * 查找秒杀商品
     */
    private SeckillGoods requirePendingSeckillGoods(Long id) {
        SeckillGoods sg = seckillGoodsMapper.selectById(id);
        if (sg == null) {
            throw new BizException("秒杀商品不存在");
        }
        if (sg.getStatus() == null || sg.getStatus() != SeckillConstants.SECKILL_STATUS_PENDING) {
            throw new BizException("该秒杀商品已处理");
        }
        return sg;
    }

    // ==================== 商家：秒杀商品管理 ====================

    /**
     * 查找商家秒杀货物
     */
    public List<SeckillGoods> listMerchantSeckillGoods(Long merchantId) {
        return seckillGoodsService.listByMerchant(merchantId);
    }

    /**
     * 申请新建秒杀商品（待管理员审核）。
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
        sg.setStatus(SeckillConstants.SECKILL_STATUS_PENDING);
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
        if (sg.getStatus() != null && sg.getStatus() == SeckillConstants.SECKILL_STATUS_REJECTED) {
            sg.setStatus(SeckillConstants.SECKILL_STATUS_PENDING);
        }
        seckillGoodsMapper.updateById(sg);
        seckillGoodsService.evictCache(seckillGoodsId);
        return sg;
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
