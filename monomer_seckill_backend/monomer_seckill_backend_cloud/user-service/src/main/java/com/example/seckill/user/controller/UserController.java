package com.example.seckill.user.controller;

import com.example.seckill.common.core.Result;
import com.example.seckill.common.core.UserContext;
import com.example.seckill.user.entity.MerchantApply;
import com.example.seckill.user.dto.LoginRequest;
import com.example.seckill.user.dto.MerchantApplyRequest;
import com.example.seckill.user.dto.RegisterRequest;
import com.example.seckill.user.service.UserService;
import com.example.seckill.user.vo.LoginVO;
import com.example.seckill.user.vo.UserVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户接口：注册、登录、个人信息、申请成为商家。
 *
 * @author haiy
 * @date 2026/08/17
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public Result<UserVO> register(@RequestBody RegisterRequest request) {
        return Result.ok(userService.register(request));
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginRequest request) {
        return Result.ok(userService.login(request));
    }

    @GetMapping("/info")
    public Result<UserVO> info() {
        return Result.ok(userService.toVO(userService.getById(UserContext.getUserId())));
    }

    /**
     * 申请成为商家。
     */
    @PostMapping("/apply-merchant")
    public Result<MerchantApply> applyMerchant(@RequestBody MerchantApplyRequest request) {
        return Result.ok(userService.applyMerchant(UserContext.getUserId(), request.getReason()));
    }

    /**
     * 查询我最近的商家申请。
     */
    @GetMapping("/apply-merchant")
    public Result<MerchantApply> myApply() {
        return Result.ok(userService.getMyApply(UserContext.getUserId()));
    }
}
