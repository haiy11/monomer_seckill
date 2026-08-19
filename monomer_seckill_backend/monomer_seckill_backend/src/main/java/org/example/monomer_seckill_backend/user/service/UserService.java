package org.example.monomer_seckill_backend.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.monomer_seckill_backend.common.BizException;
import org.example.monomer_seckill_backend.common.Constants;
import org.example.monomer_seckill_backend.common.TokenService;
import org.example.monomer_seckill_backend.user.dto.LoginRequest;
import org.example.monomer_seckill_backend.user.dto.RegisterRequest;
import org.example.monomer_seckill_backend.user.entity.MerchantApply;
import org.example.monomer_seckill_backend.user.entity.User;
import org.example.monomer_seckill_backend.user.mapper.MerchantApplyMapper;
import org.example.monomer_seckill_backend.user.mapper.UserMapper;
import org.example.monomer_seckill_backend.user.vo.LoginVO;
import org.example.monomer_seckill_backend.user.vo.UserVO;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * 用户服务：注册、登录、信息查询、申请成为商家。
 *
 * @author haiy
 * @date 2026/08/17
 */
@Service
public class UserService {

    private final UserMapper userMapper;
    private final MerchantApplyMapper merchantApplyMapper;
    private final TokenService tokenService;

    public UserService(UserMapper userMapper, MerchantApplyMapper merchantApplyMapper, TokenService tokenService) {
        this.userMapper = userMapper;
        this.merchantApplyMapper = merchantApplyMapper;
        this.tokenService = tokenService;
    }

    /**
     * 用户注册（默认普通用户）。
     */
    public UserVO register(RegisterRequest request) {
        if (request == null || request.getUsername() == null || request.getUsername().isBlank()
                || request.getPassword() == null || request.getPassword().isBlank()) {
            throw new BizException("用户名和密码不能为空");
        }
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername().trim()));
        if (count != null && count > 0) {
            throw new BizException("用户名已被占用");
        }
        User user = new User();
        user.setUsername(request.getUsername().trim());
        user.setPassword(md5(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setPhone(request.getPhone());
        user.setRole(Constants.ROLE_USER);
        userMapper.insert(user);
        return toVO(user);
    }

    /**
     * 用户登录（任意角色）。
     */
    public LoginVO login(LoginRequest request) {
        if (request == null || request.getUsername() == null || request.getPassword() == null) {
            throw new BizException("用户名和密码不能为空");
        }
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername().trim()));
        if (user == null || !user.getPassword().equals(md5(request.getPassword()))) {
            throw new BizException("用户名或密码错误");
        }
        String token = tokenService.createToken(user.getId());
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setUser(toVO(user));
        return vo;
    }

    /**
     * 按 ID 查询用户。
     */
    public User getById(Long id) {
        return id == null ? null : userMapper.selectById(id);
    }

    /**
     * 按用户名查询用户。
     */
    public User getByUsername(String username) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    /**
     * 普通用户申请成为商家。
     */
    public MerchantApply applyMerchant(Long userId, String reason) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        if (user.getRole() != Constants.ROLE_USER) {
            throw new BizException("您已是商家或管理员，无需申请");
        }
        Long pending = merchantApplyMapper.selectCount(new LambdaQueryWrapper<MerchantApply>()
                .eq(MerchantApply::getUserId, userId)
                .eq(MerchantApply::getStatus, Constants.APPLY_STATUS_PENDING));
        if (pending != null && pending > 0) {
            throw new BizException("已有待审核的商家申请，请勿重复提交");
        }
        MerchantApply apply = new MerchantApply();
        apply.setUserId(userId);
        apply.setReason(reason);
        apply.setStatus(Constants.APPLY_STATUS_PENDING);
        apply.setApplyTime(LocalDateTime.now());
        merchantApplyMapper.insert(apply);
        return apply;
    }

    /**
     * 查询用户最近的商家申请。
     */
    public MerchantApply getMyApply(Long userId) {
        return merchantApplyMapper.selectOne(new LambdaQueryWrapper<MerchantApply>()
                .eq(MerchantApply::getUserId, userId)
                .orderByDesc(MerchantApply::getId)
                .last("LIMIT 1"));
    }

    /**
     * 实体转视图对象（脱敏密码）。
     */
    public UserVO toVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setPhone(user.getPhone());
        vo.setRole(user.getRole());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }

    private String md5(String raw) {
        return DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
    }
}
