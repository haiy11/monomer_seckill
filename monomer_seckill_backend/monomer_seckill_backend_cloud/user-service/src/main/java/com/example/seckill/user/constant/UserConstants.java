package com.example.seckill.user.constant;

/**
 * 用户服务常量：角色、商家申请、商品/秒杀商品审核、Redis 缓存 key 等本服务所需魔法值。
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

    /** 商品状态：待审核 */
    public static final int GOODS_STATUS_PENDING = 0;
    /** 商品状态：已上架 */
    public static final int GOODS_STATUS_ON = 1;
    /** 商品状态：已下架 */
    public static final int GOODS_STATUS_OFF = 2;
    /** 商品状态：已拒绝 */
    public static final int GOODS_STATUS_REJECTED = 3;

    /** 秒杀商品状态：待审核 */
    public static final int SECKILL_STATUS_PENDING = 0;
    /** 秒杀商品状态：已上架 */
    public static final int SECKILL_STATUS_ON = 1;
    /** 秒杀商品状态：已拒绝 */
    public static final int SECKILL_STATUS_REJECTED = 2;
    /** 秒杀商品状态：已下架/已结束 */
    public static final int SECKILL_STATUS_OFF = 3;

    /** Redis 秒杀库存 key 前缀（管理员审核通过/重置库存时预载用） */
    public static final String STOCK_KEY_PREFIX = "seckill:stock:";

    /** Redis 已购用户集合 key 前缀（预载时清空用） */
    public static final String USERS_KEY_PREFIX = "seckill:users:";

    /** Redis 秒杀商品信息缓存 key 前缀（审核后失效缓存用） */
    public static final String GOODS_KEY_PREFIX = "seckill:goods:";
}
