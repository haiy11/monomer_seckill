package org.example.monomer_seckill_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置类。
 *
 * <p>定义 key 使用 String 序列化、value 使用 JSON 序列化的 RedisTemplate，
 * 为 P2 阶段「Redis 预扣库存」做准备。</p>
 *
 * @author haiy
 * @date 2026/08/17
 */
@Configuration
public class RedisConfig {

    /**
     * 构建 RedisTemplate Bean。
     *
     * <p>key 用 StringRedisSerializer（可读性好），value 用 GenericJackson2JsonRedisSerializer
     * （对象以 JSON 存储，便于跨语言读取）。</p>
     *
     * @param factory Redis 连接工厂（由 Spring Boot 自动配置注入）
     * @return RedisTemplate
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        StringRedisSerializer keySerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer valueSerializer = new GenericJackson2JsonRedisSerializer();

        // key 与 hash 的 key 均使用 String 序列化
        template.setKeySerializer(keySerializer);
        template.setHashKeySerializer(keySerializer);
        // value 与 hash 的 value 均使用 JSON 序列化
        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer);
        template.afterPropertiesSet();
        return template;
    }
}
