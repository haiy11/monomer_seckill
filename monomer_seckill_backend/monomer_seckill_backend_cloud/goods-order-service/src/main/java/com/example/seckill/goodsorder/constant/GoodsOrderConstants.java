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
}
