package com.example.seckill.common.cache;

/**
 * 多级缓存公共常量。
 *
 * @author haiy
 * @date 2026/08/17
 */
public final class CacheConstants {

    private CacheConstants() {
    }

    /**
     * 本地缓存失效广播通道前缀（Redis Pub/Sub）。
     *
     * <p>每个缓存实例的广播通道为「前缀 + 缓存名」，例如 {@code cache:evict:seckill-goods}。
     * 订阅端使用模式 {@code cache:evict:*} 一次订阅全部缓存通道。</p>
     */
    public static final String EVICT_CHANNEL_PREFIX = "cache:evict:";

    /**
     * 空值占位符：DB 查询结果为空时写入分布式缓存，防止缓存穿透（反复查询不存在的 key 打到 DB）。
     */
    public static final String NULL_PLACEHOLDER = "__NULL__";

    /**
     * 默认分布式缓存有效期（秒）。
     */
    public static final long DEFAULT_REDIS_TTL_SECONDS = 1800;

    /**
     * 默认空值缓存有效期（秒）。
     */
    public static final long DEFAULT_NULL_TTL_SECONDS = 60;
}
