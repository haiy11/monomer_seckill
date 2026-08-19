package org.example.monomer_seckill_backend.user.controller;

import org.example.monomer_seckill_backend.common.Result;
import org.example.monomer_seckill_backend.common.UserContext;
import org.example.monomer_seckill_backend.user.dto.LoginRequest;
import org.example.monomer_seckill_backend.user.dto.MerchantApplyRequest;
import org.example.monomer_seckill_backend.user.dto.RegisterRequest;
import org.example.monomer_seckill_backend.user.entity.MerchantApply;
import org.example.monomer_seckill_backend.user.service.UserService;
import org.example.monomer_seckill_backend.user.vo.LoginVO;
import org.example.monomer_seckill_backend.user.vo.UserVO;
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
