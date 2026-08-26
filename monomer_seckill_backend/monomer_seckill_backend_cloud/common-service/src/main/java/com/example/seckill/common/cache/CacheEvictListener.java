package com.example.seckill.common.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 本地缓存失效广播监听器。
 *
 * <p>订阅 Redis Pub/Sub 模式 {@code cache:evict:*}. 当任一服务实例更新 DB 并删除分布式缓存后，
 * 会向「前缀 + 缓存名」通道发布被删除的缓存 key；本监听器收到后通过
 * {@link MultiLevelCacheRegistry} 定位缓存实例，删除本进程内对应的 L1 本地缓存。</p>
 *
 * @author haiy
 * @date 2026/08/17
 */
@Slf4j
@Component
public class CacheEvictListener implements MessageListener {

    private final MultiLevelCacheRegistry registry;

    public CacheEvictListener(MultiLevelCacheRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
        String redisKey = new String(message.getBody(), StandardCharsets.UTF_8);
        registry.evict(channel, redisKey);
    }
}
