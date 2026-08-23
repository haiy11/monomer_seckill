package com.example.seckill.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.seckill.common.core.BizException;
import com.example.seckill.common.redis.RedisUtil;
import com.example.seckill.user.constant.UserConstants;
import com.example.seckill.user.dto.LoginRequest;
import com.example.seckill.user.entity.Goods;
import com.example.seckill.user.entity.MallOrder;
import com.example.seckill.user.entity.MerchantApply;
import com.example.seckill.user.entity.SeckillGoods;
import com.example.seckill.user.entity.SeckillOrder;
import com.example.seckill.user.entity.User;
import com.example.seckill.user.mapper.GoodsMapper;
import com.example.seckill.user.mapper.MallOrderMapper;
import com.example.seckill.user.mapper.MerchantApplyMapper;
import com.example.seckill.user.mapper.SeckillGoodsMapper;
import com.example.seckill.user.mapper.SeckillOrderMapper;
import com.example.seckill.user.mapper.UserMapper;
import com.example.seckill.user.vo.LoginVO;
import com.example.seckill.user.vo.MerchantApplyVO;
import com.example.seckill.user.vo.SeckillGoodsVO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理员服务：登录、审核商家申请、审核商品上架、审核秒杀商品上架、订单与库存管理。
 *
 * <p>本服务直连共享库进行审核/管理所需的读与状态变更；秒杀库存预载与缓存失效在此本地实现。</p>
 *
 * @author haiy
 * @date 2026/08/17
 */
@Service
public class AdminService {

    private final UserService userService;
    private final UserMapper userMapper;
    private final MerchantApplyMapper merchantApplyMapper;
    private final GoodsMapper goodsMapper;
    private final SeckillGoodsMapper seckillGoodsMapper;
    private final MallOrderMapper mallOrderMapper;
    private final SeckillOrderMapper seckillOrderMapper;
    private final RedisUtil redisUtil;

    public AdminService(UserService userService, UserMapper userMapper, MerchantApplyMapper merchantApplyMapper,
                        GoodsMapper goodsMapper, SeckillGoodsMapper seckillGoodsMapper,
                        MallOrderMapper mallOrderMapper, SeckillOrderMapper seckillOrderMapper,
                        RedisUtil redisUtil) {
        this.userService = userService;
        this.userMapper = userMapper;
        this.merchantApplyMapper = merchantApplyMapper;
        this.goodsMapper = goodsMapper;
        this.seckillGoodsMapper = seckillGoodsMapper;
        this.mallOrderMapper = mallOrderMapper;
        this.seckillOrderMapper = seckillOrderMapper;
        this.redisUtil = redisUtil;
    }

    /**
     * 管理员登录（要求角色为管理员）。
     */
    public LoginVO login(LoginRequest request) {
        LoginVO vo = userService.login(request);
        if (vo.getUser().getRole() == null || vo.getUser().getRole() != UserConstants.ROLE_ADMIN) {
            throw new BizException(403, "无管理员权限");
        }
        return vo;
    }

    // ==================== 商家申请审核 ====================

    public List<MerchantApplyVO> listMerchantApplies() {
        List<MerchantApply> list = merchantApplyMapper.selectList(new LambdaQueryWrapper<MerchantApply>()
                .orderByDesc(MerchantApply::getId));
        return list.stream().map(this::toVO).toList();
    }

    /**
     * 通过商家申请：用户角色变为商家。
     */
    public void approveMerchantApply(Long id, String remark) {
        MerchantApply apply = requirePendingApply(id);
        User user = userMapper.selectById(apply.getUserId());
        if (user == null) {
            throw new BizException("申请人不存在");
        }
        apply.setStatus(UserConstants.APPLY_STATUS_APPROVED);
        apply.setReviewTime(LocalDateTime.now());
        apply.setReviewRemark(remark);
        merchantApplyMapper.updateById(apply);

        user.setRole(UserConstants.ROLE_MERCHANT);
        userMapper.updateById(user);
    }

    /**
     * 拒绝商家申请。
     */
    public void rejectMerchantApply(Long id, String remark) {
        MerchantApply apply = requirePendingApply(id);
        apply.setStatus(UserConstants.APPLY_STATUS_REJECTED);
        apply.setReviewTime(LocalDateTime.now());
        apply.setReviewRemark(remark);
        merchantApplyMapper.updateById(apply);
    }

    private MerchantApply requirePendingApply(Long id) {
        MerchantApply apply = merchantApplyMapper.selectById(id);
        if (apply == null) {
            throw new BizException("申请不存在");
        }
        if (apply.getStatus() == null || apply.getStatus() != UserConstants.APPLY_STATUS_PENDING) {
            throw new BizException("该申请已处理");
        }
        return apply;
    }

    // ==================== 商品审核 ====================

    public List<Goods> listGoods() {
        return goodsMapper.selectList(new LambdaQueryWrapper<Goods>().orderByDesc(Goods::getId));
    }

    /**
     * 通过商品审核：待审核 → 已上架。
     */
    public void approveGoods(Long id) {
        Goods goods = requirePendingGoods(id);
        goods.setStatus(UserConstants.GOODS_STATUS_ON);
        goodsMapper.updateById(goods);
    }

    /**
     * 拒绝商品审核：待审核 → 已拒绝。
     */
    public void rejectGoods(Long id) {
        Goods goods = requirePendingGoods(id);
        goods.setStatus(UserConstants.GOODS_STATUS_REJECTED);
        goodsMapper.updateById(goods);
    }

    private Goods requirePendingGoods(Long id) {
        Goods goods = goodsMapper.selectById(id);
        if (goods == null) {
            throw new BizException("商品不存在");
        }
        if (goods.getStatus() == null || goods.getStatus() != UserConstants.GOODS_STATUS_PENDING) {
            throw new BizException("该商品已处理");
        }
        return goods;
    }

    // ==================== 秒杀商品审核 ====================

    public List<SeckillGoodsVO> listSeckillGoods() {
        List<SeckillGoods> list = seckillGoodsMapper.selectList(new LambdaQueryWrapper<SeckillGoods>()
                .orderByDesc(SeckillGoods::getId));
        return list.stream().map(this::toVO).toList();
    }

    /**
     * 通过秒杀商品审核：待审核 → 已上架，并预载秒杀库存到 Redis。
     */
    public void approveSeckillGoods(Long id) {
        SeckillGoods sg = requirePendingSeckillGoods(id);
        sg.setStatus(UserConstants.SECKILL_STATUS_ON);
        seckillGoodsMapper.updateById(sg);
        evictCache(id);
        preload(id);
    }

    /**
     * 拒绝秒杀商品审核。
     */
    public void rejectSeckillGoods(Long id) {
        SeckillGoods sg = requirePendingSeckillGoods(id);
        sg.setStatus(UserConstants.SECKILL_STATUS_REJECTED);
        seckillGoodsMapper.updateById(sg);
        evictCache(id);
    }

    private SeckillGoods requirePendingSeckillGoods(Long id) {
        SeckillGoods sg = seckillGoodsMapper.selectById(id);
        if (sg == null) {
            throw new BizException("秒杀商品不存在");
        }
        if (sg.getStatus() == null || sg.getStatus() != UserConstants.SECKILL_STATUS_PENDING) {
            throw new BizException("该秒杀商品已处理");
        }
        return sg;
    }

    // ==================== 订单与库存 ====================

    public List<MallOrder> listOrders() {
        return mallOrderMapper.selectList(new LambdaQueryWrapper<MallOrder>().orderByDesc(MallOrder::getId));
    }

    public List<SeckillOrder> listSeckillOrders() {
        return seckillOrderMapper.selectList(new LambdaQueryWrapper<SeckillOrder>().orderByDesc(SeckillOrder::getId));
    }

    public void resetStock() {
        preloadAll();
    }

    public void resetStock(Long seckillGoodsId) {
        preload(seckillGoodsId);
    }

    // ==================== 本地缓存/预载 ====================

    private void preload(Long seckillGoodsId) {
        SeckillGoods sg = seckillGoodsMapper.selectById(seckillGoodsId);
        if (sg == null) {
            return;
        }
        redisUtil.set(UserConstants.STOCK_KEY_PREFIX + seckillGoodsId, String.valueOf(sg.getSeckillStock()));
        redisUtil.delete(UserConstants.USERS_KEY_PREFIX + seckillGoodsId);
    }

    private void preloadAll() {
        List<SeckillGoods> list = seckillGoodsMapper.selectList(null);
        for (SeckillGoods sg : list) {
            redisUtil.set(UserConstants.STOCK_KEY_PREFIX + sg.getId(), String.valueOf(sg.getSeckillStock()));
            redisUtil.delete(UserConstants.USERS_KEY_PREFIX + sg.getId());
        }
    }

    private void evictCache(Long id) {
        if (id != null) {
            redisUtil.delete(UserConstants.GOODS_KEY_PREFIX + id);
        }
    }

    private MerchantApplyVO toVO(MerchantApply apply) {
        User user = userMapper.selectById(apply.getUserId());
        MerchantApplyVO vo = new MerchantApplyVO();
        vo.setId(apply.getId());
        vo.setUserId(apply.getUserId());
        vo.setUsername(user == null ? null : user.getUsername());
        vo.setNickname(user == null ? null : user.getNickname());
        vo.setReason(apply.getReason());
        vo.setStatus(apply.getStatus());
        vo.setApplyTime(apply.getApplyTime());
        vo.setReviewTime(apply.getReviewTime());
        vo.setReviewRemark(apply.getReviewRemark());
        return vo;
    }

    private SeckillGoodsVO toVO(SeckillGoods sg) {
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
}
