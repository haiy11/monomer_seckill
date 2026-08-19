package org.example.monomer_seckill_backend.user.vo;

import lombok.Data;

/**
 * 登录结果视图对象：token + 用户信息。
 *
 * @author haiy
 * @date 2026/08/17
 */
@Data
public class LoginVO {

    /** 登录 token */
    private String token;

    /** 用户信息（已脱敏） */
    private UserVO user;
}
