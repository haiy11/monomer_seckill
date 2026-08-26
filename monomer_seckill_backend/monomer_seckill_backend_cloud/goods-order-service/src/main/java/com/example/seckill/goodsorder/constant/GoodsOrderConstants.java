package com.example.seckill.goodsorder.constant;

/**
 * 商品/订单服务常量：商品状态、订单状态、种子数据所需角色等本服务所需魔法值。
 *
 * @author haiy
 * @date 2026/08/17
 */
public final class GoodsOrderConstants {

    private GoodsOrderConstants() {
    }

    /** 用户角色：普通用户（种子数据用） */
    public static final int ROLE_USER = 0;
    /** 用户角色：商家（种子数据用） */
    public static final int ROLE_MERCHANT = 1;
    /** 用户角色：管理员（种子数据用） */
    public static final int ROLE_ADMIN = 2;

    /** 商品状态：待审核 */
    public static final int GOODS_STATUS_PENDING = 0;
    /** 商品状态：已上架 */
    public static final int GOODS_STATUS_ON = 1;
    /** 商品状态：已下架 */
    public static final int GOODS_STATUS_OFF = 2;
    /** 商品状态：已拒绝 */
    public static final int GOODS_STATUS_REJECTED = 3;

    /** 订单状态：待支付 */
    public static final int ORDER_STATUS_UNPAID = 0;
    /** 订单状态：已支付 */
    public static final int ORDER_STATUS_PAID = 1;
    /** 订单状态：已取消 */
    public static final int ORDER_STATUS_CANCELED = 2;
    /** 订单状态：超时关闭 */
    public static final int ORDER_STATUS_TIMEOUT = 3;

    /** 商品信息多级缓存 key 前缀，拼 goodsId：goods:cache:{goodsId} */
    public static final String GOODS_KEY_PREFIX = "goods:cache:";

    /** 商品信息缓存有效期（秒） */
    public static final long GOODS_CACHE_TTL_SECONDS = 1800;

    /** 商品信息空值缓存有效期（秒） */
    public static final long GOODS_NULL_TTL_SECONDS = 60;

    /** 商品本地缓存（Caffeine L1）最大条目数 */
    public static final long GOODS_LOCAL_CACHE_MAX_SIZE = 1000;

    /** 商品本地缓存（Caffeine L1）有效期（秒），作为失效广播之外的兜底一致性手段 */
    public static final long GOODS_LOCAL_CACHE_TTL_SECONDS = 300;
}
