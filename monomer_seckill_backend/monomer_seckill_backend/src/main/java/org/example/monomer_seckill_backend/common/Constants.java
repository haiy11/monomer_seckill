package org.example.monomer_seckill_backend.common;

/**
 * 全局常量定义。
 *
 * <p>集中管理角色、业务状态码、Redis key 前缀、Lua 返回码等魔法值。</p>
 *
 * @author haiy
 * @date 2026/08/17
 */
public final class Constants {

    private Constants() {
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

    /** 订单状态：待支付 */
    public static final int ORDER_STATUS_UNPAID = 0;
    /** 订单状态：已支付 */
    public static final int ORDER_STATUS_PAID = 1;
    /** 订单状态：已取消 */
    public static final int ORDER_STATUS_CANCELED = 2;
    /** 订单状态：超时关闭 */
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

    /** 库存空值缓存占位值：表示秒杀商品不存在（对扣库存脚本而言等价于无库存） */
    public static final String STOCK_NULL_VALUE = "0";

    /** 库存空值缓存有效期（秒）：不存在的商品短时间缓存，避免每次请求穿透到数据库 */
    public static final long STOCK_NULL_TTL_SECONDS = 60;

    /** 登录 token key 前缀：mall:token:{token} */
    public static final String TOKEN_KEY_PREFIX = "mall:token:";

    /** 登录 token 有效期（分钟） */
    public static final long TOKEN_TTL_MINUTES = 30;

    /** Lua 返回码：抢购成功 */
    public static final long LUA_SUCCESS = 0L;
    /** Lua 返回码：库存不足 */
    public static final long LUA_STOCK_EMPTY = -1L;
    /** Lua 返回码：重复抢购 */
    public static final long LUA_DUPLICATE = -2L;
}
