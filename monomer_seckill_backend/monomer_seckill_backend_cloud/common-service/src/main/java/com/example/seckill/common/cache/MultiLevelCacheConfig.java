package com.example.seckill.common.cache;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * 多级缓存基础设施配置。
 *
 * <p>注册 {@link RedisMessageListenerContainer}，以模式订阅 {@code cache:evict:*} 通道，
 * 监听各服务实例发布的「本地缓存失效广播」，保证多实例下 L1 本地缓存最终一致。</p>
 *
 * @author haiy
 * @date 2026/08/17
 */
@Configuration
public class MultiLevelCacheConfig {

    /**
     * 消息监听容器：订阅 cache:evict:* 模式，回调 {@link CacheEvictListener}。
     */
    @Bean
    public RedisMessageListenerContainer cacheEvictListenerContainer(
            RedisConnectionFactory connectionFactory, CacheEvictListener cacheEvictListener) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(cacheEvictListener,
                new PatternTopic(CacheConstants.EVICT_CHANNEL_PREFIX + "*"));
        return container;
    }
}
