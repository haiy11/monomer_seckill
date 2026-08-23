package com.example.seckill.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.seckill.common.core.BizException;
import com.example.seckill.common.core.Result;
import com.example.seckill.user.client.GoodsOrderClient;
import com.example.seckill.user.client.SeckillClient;
import com.example.seckill.user.constant.UserConstants;
import com.example.seckill.user.dto.LoginRequest;
import com.example.seckill.user.entity.Goods;
import com.example.seckill.user.entity.MallOrder;
import com.example.seckill.user.entity.MerchantApply;
import com.example.seckill.user.entity.SeckillOrder;
import com.example.seckill.user.entity.User;
import com.example.seckill.user.mapper.MerchantApplyMapper;
import com.example.seckill.user.mapper.UserMapper;
import com.example.seckill.user.vo.LoginVO;
import com.example.seckill.user.vo.MerchantApplyVO;
import com.example.seckill.user.vo.SeckillGoodsVO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理员服务：登录、审核商家申请（本服务），以及审核商品/订单/秒杀商品/重置库存（经 Feign 调对应服务）。
 *
 * @author haiy
 * @date 2026/08/17
 */
@Service
public class AdminService {

    private final UserService userService;
    private final UserMapper userMapper;
    private final MerchantApplyMapper merchantApplyMapper;
    private final GoodsOrderClient goodsOrderClient;
    private final SeckillClient seckillClient;

    public AdminService(UserService userService, UserMapper userMapper, MerchantApplyMapper merchantApplyMapper,
                        GoodsOrderClient goodsOrderClient, SeckillClient seckillClient) {
        this.userService = userService;
        this.userMapper = userMapper;
        this.merchantApplyMapper = merchantApplyMapper;
        this.goodsOrderClient = goodsOrderClient;
        this.seckillClient = seckillClient;
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

    // ==================== 商家申请审核（本服务） ====================

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

    // ==================== 商品审核（Feign → goods-order-service） ====================

    public List<Goods> listGoods() {
        return unwrap(goodsOrderClient.listGoods());
    }

    /**
     * 通过商品审核：待审核 → 已上架。
     */
    public void approveGoods(Long id) {
        unwrap(goodsOrderClient.approveGoods(id));
    }

    /**
     * 拒绝商品审核：待审核 → 已拒绝。
     */
    public void rejectGoods(Long id) {
        unwrap(goodsOrderClient.rejectGoods(id));
    }

    // ==================== 秒杀商品审核 / 秒杀订单 / 库存（Feign → seckill-service） ====================

    public List<SeckillGoodsVO> listSeckillGoods() {
        return unwrap(seckillClient.listSeckillGoods());
    }

    /**
     * 通过秒杀商品审核：待审核 → 已上架，并预载秒杀库存到 Redis。
     */
    public void approveSeckillGoods(Long id) {
        unwrap(seckillClient.approveSeckillGoods(id));
    }

    /**
     * 拒绝秒杀商品审核。
     */
    public void rejectSeckillGoods(Long id) {
        unwrap(seckillClient.rejectSeckillGoods(id));
    }

    public List<MallOrder> listOrders() {
        return unwrap(goodsOrderClient.listOrders());
    }

    public List<SeckillOrder> listSeckillOrders() {
        return unwrap(seckillClient.listSeckillOrders());
    }

    public void resetStock() {
        unwrap(seckillClient.resetStock());
    }

    public void resetStock(Long seckillGoodsId) {
        unwrap(seckillClient.resetStockOne(seckillGoodsId));
    }

    // ==================== 工具 ====================

    /**
     * 解析 Feign 返回的统一响应体，失败时转为业务异常。
     */
    private <T> T unwrap(Result<T> result) {
        if (result == null || result.getCode() == null || result.getCode() != 200) {
            throw new BizException(result == null ? "远程调用失败" : result.getMsg());
        }
        return result.getData();
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
