package com.example.seckill.seckill.constant;

/**
 * 秒杀服务常量：秒杀状态、Redis 库存/用户/商品缓存 key 前缀、缓存有效期、Lua 返回码。
 *
 * @author haiy
 * @date 2026/08/17
 */
public final class SeckillConstants {

    private SeckillConstants() {
    }

    /** 秒杀商品状态：待审核 */
    public static final int SECKILL_STATUS_PENDING = 0;
    /** 秒杀商品状态：已上架 */
    public static final int SECKILL_STATUS_ON = 1;
    /** 秒杀商品状态：已拒绝 */
    public static final int SECKILL_STATUS_REJECTED = 2;
    /** 秒杀商品状态：已下架/已结束 */
    public static final int SECKILL_STATUS_OFF = 3;

    /** 秒杀订单状态：待支付 */
    public static final int ORDER_STATUS_UNPAID = 0;
    /** 秒杀订单状态：已支付 */
    public static final int ORDER_STATUS_PAID = 1;
    /** 秒杀订单状态：已取消 */
    public static final int ORDER_STATUS_CANCELED = 2;
    /** 秒杀订单状态：超时关闭 */
    public static final int ORDER_STATUS_TIMEOUT = 3;

    /** Redis 秒杀库存 key 前缀，拼 seckillGoodsId：seckill:stock:{seckillGoodsId} */
    public static final String STOCK_KEY_PREFIX = "seckill:stock:";

    /** Redis 已购用户集合 key 前缀：seckill:users:{seckillGoodsId} */
    public static final String USERS_KEY_PREFIX = "seckill:users:";

    /** Redis 秒杀商品信息缓存 key 前缀，拼 seckillGoodsId：seckill:goods:{seckillGoodsId} */
    public static final String GOODS_KEY_PREFIX = "seckill:goods:";

    /** 秒杀商品信息缓存有效期（秒） */
    public static final long GOODS_CACHE_TTL_SECONDS = 1800;

    /** 秒杀商品信息缓存空值占位符：表示商品不存在，防止缓存穿透 */
    public static final String GOODS_CACHE_NULL = "__NULL__";

    /** 秒杀商品信息空值缓存有效期（秒） */
    public static final long GOODS_NULL_TTL_SECONDS = 60;

    /** 秒杀商品本地缓存（Caffeine L1）最大条目数 */
    public static final long GOODS_LOCAL_CACHE_MAX_SIZE = 1000;

    /** 秒杀商品本地缓存（Caffeine L1）有效期（秒），作为失效广播之外的兜底一致性手段 */
    public static final long GOODS_LOCAL_CACHE_TTL_SECONDS = 300;

    /** 库存空值缓存占位值：表示秒杀商品不存在（对扣库存脚本而言等价于无库存） */
    public static final String STOCK_NULL_VALUE = "0";

    /** 库存空值缓存有效期（秒） */
    public static final long STOCK_NULL_TTL_SECONDS = 60;

    /** Lua 返回码：抢购成功 */
    public static final long LUA_SUCCESS = 0L;
    /** Lua 返回码：库存不足 */
    public static final long LUA_STOCK_EMPTY = -1L;
    /** Lua 返回码：重复抢购 */
    public static final long LUA_DUPLICATE = -2L;
}
