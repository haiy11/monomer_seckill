package com.example.seckill.user.constant;

/**
 * 用户服务常量：角色、商家申请状态等本服务所需魔法值。
 *
 * @author haiy
 * @date 2026/08/17
 */
public final class UserConstants {

    private UserConstants() {
    }

    /** 用户角色：普通用户 */
    public static final int ROLE_USER = 0;
    /** 用户角色：商家 */
    public static final int ROLE_MERCHANT = 1;
    /** 用户角色：管理员 */
    public static final int ROLE_ADMIN = 2;

    /** 商家申请状态：待审核 */
    public static final int APPLY_STATUS_PENDING = 0;
    /** 商家申请状态：通过 */
    public static final int APPLY_STATUS_APPROVED = 1;
    /** 商家申请状态：拒绝 */
    public static final int APPLY_STATUS_REJECTED = 2;
}
