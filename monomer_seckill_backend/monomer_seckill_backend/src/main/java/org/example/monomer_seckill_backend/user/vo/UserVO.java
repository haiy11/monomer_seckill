package org.example.monomer_seckill_backend.user.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息视图对象（对外输出，脱敏密码等敏感字段）。
 *
 * @author haiy
 * @date 2026/08/17
 */
@Data
public class UserVO {

    /** 用户ID */
    private Long id;

    /** 用户名 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 手机号 */
    private String phone;

    /** 角色：0-普通用户 1-管理员 */
    private Integer role;

    /** 创建时间 */
    private LocalDateTime createTime;
}
