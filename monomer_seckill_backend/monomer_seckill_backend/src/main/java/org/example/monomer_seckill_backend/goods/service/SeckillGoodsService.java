package org.example.monomer_seckill_backend.goods.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.monomer_seckill_backend.common.BizException;
import org.example.monomer_seckill_backend.common.Constants;
import org.example.monomer_seckill_backend.goods.entity.SeckillGoods;
import org.example.monomer_seckill_backend.goods.mapper.SeckillGoodsMapper;
import org.example.monomer_seckill_backend.goods.vo.SeckillGoodsVO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 秒杀商品服务：查询、状态维护。
 *
 * @author haiy
 * @date 2026/08/17
 */
@Service
public class SeckillGoodsService {

    private final SeckillGoodsMapper seckillGoodsMapper;

    public SeckillGoodsService(SeckillGoodsMapper seckillGoodsMapper) {
        this.seckillGoodsMapper = seckillGoodsMapper;
    }

    /**
     * 查询已上架秒杀商品（用户端秒杀商品页）。
     */
    public List<SeckillGoodsVO> listOnSale() {
        List<SeckillGoods> list = seckillGoodsMapper.selectList(new LambdaQueryWrapper<SeckillGoods>()
                .eq(SeckillGoods::getStatus, Constants.SECKILL_STATUS_ON)
                .orderByAsc(SeckillGoods::getId));
        return list.stream().map(this::toVO).toList();
    }

    /**
     * 查询单个秒杀商品（含商品信息）。
     */
    public SeckillGoodsVO getVO(Long id) {
        SeckillGoods sg = seckillGoodsMapper.selectById(id);
        return sg == null ? null : toVO(sg);
    }

    /**
     * 按 ID 查询秒杀商品实体。
     */
    public SeckillGoods getById(Long id) {
        return id == null ? null : seckillGoodsMapper.selectById(id);
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
     * 校验秒杀商品是否处于「已上架且可购买」状态（供秒杀下单调用）。
     */
    public SeckillGoods requirePurchasable(Long id) {
        SeckillGoods sg = seckillGoodsMapper.selectById(id);
        if (sg == null) {
            throw new BizException("秒杀商品不存在");
        }
        if (sg.getStatus() == null || sg.getStatus() != Constants.SECKILL_STATUS_ON) {
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
