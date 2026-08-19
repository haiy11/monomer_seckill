package org.example.monomer_seckill_backend.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.monomer_seckill_backend.common.BizException;
import org.example.monomer_seckill_backend.common.Constants;
import org.example.monomer_seckill_backend.goods.entity.Goods;
import org.example.monomer_seckill_backend.goods.entity.SeckillGoods;
import org.example.monomer_seckill_backend.goods.mapper.GoodsMapper;
import org.example.monomer_seckill_backend.goods.mapper.SeckillGoodsMapper;
import org.example.monomer_seckill_backend.goods.service.SeckillGoodsService;
import org.example.monomer_seckill_backend.goods.service.StockService;
import org.example.monomer_seckill_backend.goods.vo.SeckillGoodsVO;
import org.example.monomer_seckill_backend.order.entity.MallOrder;
import org.example.monomer_seckill_backend.order.entity.SeckillOrder;
import org.example.monomer_seckill_backend.order.service.OrderService;
import org.example.monomer_seckill_backend.user.dto.LoginRequest;
import org.example.monomer_seckill_backend.user.entity.MerchantApply;
import org.example.monomer_seckill_backend.user.entity.User;
import org.example.monomer_seckill_backend.user.mapper.MerchantApplyMapper;
import org.example.monomer_seckill_backend.user.mapper.UserMapper;
import org.example.monomer_seckill_backend.user.service.UserService;
import org.example.monomer_seckill_backend.user.vo.LoginVO;
import org.example.monomer_seckill_backend.user.vo.MerchantApplyVO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理员服务：登录、审核商家申请、审核商品上架、审核秒杀商品上架、订单与库存管理。
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
    private final SeckillGoodsService seckillGoodsService;
    private final OrderService orderService;
    private final StockService stockService;

    public AdminService(UserService userService, UserMapper userMapper, MerchantApplyMapper merchantApplyMapper,
                        GoodsMapper goodsMapper, SeckillGoodsMapper seckillGoodsMapper,
                        SeckillGoodsService seckillGoodsService, OrderService orderService, StockService stockService) {
        this.userService = userService;
        this.userMapper = userMapper;
        this.merchantApplyMapper = merchantApplyMapper;
        this.goodsMapper = goodsMapper;
        this.seckillGoodsMapper = seckillGoodsMapper;
        this.seckillGoodsService = seckillGoodsService;
        this.orderService = orderService;
        this.stockService = stockService;
    }

    /**
     * 管理员登录（要求角色为管理员）。
     */
    public LoginVO login(LoginRequest request) {
        LoginVO vo = userService.login(request);
        if (vo.getUser().getRole() == null || vo.getUser().getRole() != Constants.ROLE_ADMIN) {
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
        apply.setStatus(Constants.APPLY_STATUS_APPROVED);
        apply.setReviewTime(LocalDateTime.now());
        apply.setReviewRemark(remark);
        merchantApplyMapper.updateById(apply);

        user.setRole(Constants.ROLE_MERCHANT);
        userMapper.updateById(user);
    }

    /**
     * 拒绝商家申请。
     */
    public void rejectMerchantApply(Long id, String remark) {
        MerchantApply apply = requirePendingApply(id);
        apply.setStatus(Constants.APPLY_STATUS_REJECTED);
        apply.setReviewTime(LocalDateTime.now());
        apply.setReviewRemark(remark);
        merchantApplyMapper.updateById(apply);
    }

    private MerchantApply requirePendingApply(Long id) {
        MerchantApply apply = merchantApplyMapper.selectById(id);
        if (apply == null) {
            throw new BizException("申请不存在");
        }
        if (apply.getStatus() == null || apply.getStatus() != Constants.APPLY_STATUS_PENDING) {
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
        goods.setStatus(Constants.GOODS_STATUS_ON);
        goodsMapper.updateById(goods);
    }

    /**
     * 拒绝商品审核：待审核 → 已拒绝。
     */
    public void rejectGoods(Long id) {
        Goods goods = requirePendingGoods(id);
        goods.setStatus(Constants.GOODS_STATUS_REJECTED);
        goodsMapper.updateById(goods);
    }

    private Goods requirePendingGoods(Long id) {
        Goods goods = goodsMapper.selectById(id);
        if (goods == null) {
            throw new BizException("商品不存在");
        }
        if (goods.getStatus() == null || goods.getStatus() != Constants.GOODS_STATUS_PENDING) {
            throw new BizException("该商品已处理");
        }
        return goods;
    }

    // ==================== 秒杀商品审核 ====================

    public List<SeckillGoodsVO> listSeckillGoods() {
        return seckillGoodsService.listAll().stream().map(seckillGoodsService::toVO).toList();
    }

    /**
     * 通过秒杀商品审核：待审核 → 已上架，并预载秒杀库存到 Redis。
     */
    public void approveSeckillGoods(Long id) {
        SeckillGoods sg = requirePendingSeckillGoods(id);
        sg.setStatus(Constants.SECKILL_STATUS_ON);
        seckillGoodsMapper.updateById(sg);
        stockService.preload(id);
    }

    /**
     * 拒绝秒杀商品审核。
     */
    public void rejectSeckillGoods(Long id) {
        SeckillGoods sg = requirePendingSeckillGoods(id);
        sg.setStatus(Constants.SECKILL_STATUS_REJECTED);
        seckillGoodsMapper.updateById(sg);
    }

    private SeckillGoods requirePendingSeckillGoods(Long id) {
        SeckillGoods sg = seckillGoodsMapper.selectById(id);
        if (sg == null) {
            throw new BizException("秒杀商品不存在");
        }
        if (sg.getStatus() == null || sg.getStatus() != Constants.SECKILL_STATUS_PENDING) {
            throw new BizException("该秒杀商品已处理");
        }
        return sg;
    }

    // ==================== 订单与库存 ====================

    public List<MallOrder> listOrders() {
        return orderService.listAllNormal();
    }

    public List<SeckillOrder> listSeckillOrders() {
        return orderService.listAllSeckill();
    }

    public void resetStock() {
        stockService.preloadAll();
    }

    public void resetStock(Long seckillGoodsId) {
        stockService.preload(seckillGoodsId);
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
}
