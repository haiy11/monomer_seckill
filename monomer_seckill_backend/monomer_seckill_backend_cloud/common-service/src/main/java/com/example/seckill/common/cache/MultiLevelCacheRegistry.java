package com.example.seckill.common.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 多级缓存注册表。
 *
 * <p>维护「失效广播通道 → 缓存实例」的映射。每个 {@link MultiLevelCache} 在构建时按
 * 自己的广播通道注册进来；当订阅端收到某通道的失效消息时，据此定位到对应缓存并删除
 * 其进程内 L1 本地缓存，实现多实例本地缓存一致性。</p>
 *
 * @author haiy
 * @date 2026/08/17
 */
@Slf4j
@Component
public class MultiLevelCacheRegistry {

    /** channel（如 cache:evict:seckill-goods）→ 缓存实例 */
    private final Map<String, MultiLevelCache<?, ?>> byChannel = new ConcurrentHashMap<>();

    /**
     * 注册缓存实例。
     */
    public void register(String channel, MultiLevelCache<?, ?> cache) {
        MultiLevelCache<?, ?> previous = byChannel.putIfAbsent(channel, cache);
        if (previous != null) {
            throw new IllegalStateException("缓存通道重复注册：" + channel);
        }
        log.info("已注册多级缓存，channel={}", channel);
    }

    /**
     * 处理一条失效广播消息：删除对应缓存的本地 L1 条目。
     *
     * @param channel 实际广播通道（如 cache:evict:goods）
     * @param redisKey 被失效的缓存 key（本地缓存与分布式缓存共用同一 key）
     */
    public void evict(String channel, String redisKey) {
        MultiLevelCache<?, ?> cache = byChannel.get(channel);
        if (cache == null) {
            log.debug("收到未知通道的失效消息，忽略，channel={}", channel);
            return;
        }
        cache.evictLocalByKey(redisKey);
    }
}
