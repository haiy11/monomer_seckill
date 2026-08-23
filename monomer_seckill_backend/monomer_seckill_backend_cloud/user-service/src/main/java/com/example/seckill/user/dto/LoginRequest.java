package com.example.seckill.user.dto;

import lombok.Data;

/**
 * 用户登录请求。
 *
 * @author haiy
 * @date 2026/08/17
 */
@Data
public class LoginRequest {

    /** 用户名 */
    private String username;

    /** 密码（明文） */
    private String password;
}
