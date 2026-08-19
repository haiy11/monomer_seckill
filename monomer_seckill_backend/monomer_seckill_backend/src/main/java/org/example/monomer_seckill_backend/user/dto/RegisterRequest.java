package org.example.monomer_seckill_backend.user.dto;

import lombok.Data;

/**
 * 用户注册请求。
 *
 * @author haiy
 * @date 2026/08/17
 */
@Data
public class RegisterRequest {

    /** 用户名（登录账号） */
    private String username;

    /** 密码（明文，服务端负责 MD5） */
    private String password;

    /** 昵称（可选） */
    private String nickname;

    /** 手机号（可选） */
    private String phone;
}
