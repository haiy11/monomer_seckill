package com.example.seckill.common.cache;

import com.example.seckill.common.redis.RedisUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * 通用多级缓存组件（L1 Caffeine 本地缓存 → L2 Redis 分布式缓存 → L3 DB）。
 *
 * <p>读路径：本地 → 分布式 → DB，逐级回填；DB 查询为空时写入空值占位符（防穿透）。
 * 写路径采用「Cache-Aside（旁路缓存）」策略：更新 DB 后删除缓存。删除时先删本地 L1、
 * 再删分布式 L2，最后通过 Redis Pub/Sub 广播通知其它实例删除各自 L1，保证多实例最终一致。</p>
 *
 * <p>本地缓存 key 与分布式缓存 key 统一（均为 {@code redisKeyBuilder} 生成的字符串），
 * 因此广播消息只需携带该 key，无需额外的类型解析。</p>
 *
 * @param <K> 业务主键类型（如 Long 商品ID）
 * @param <V> 缓存对象类型（如秒杀商品实体）
 * @author haiy
 * @date 2026/08/17
 */
@Slf4j
public class MultiLevelCache<K, V> {

    private final String name;
    private final Cache<String, V> local;
    private final RedisUtil redis;
    private final ObjectMapper objectMapper;
    private final Class<V> valueType;
    private final Function<K, String> redisKeyBuilder;
    private final Function<K, V> dbLoader;
    private final Duration redisTtl;
    private final Duration redisNullTtl;
    private final String nullPlaceholder;
    private final String evictChannel;

    private MultiLevelCache(Builder<K, V> builder) {
        this.name = builder.name;
        this.local = builder.local;
        this.redis = builder.redis;
        this.objectMapper = builder.objectMapper;
        this.valueType = builder.valueType;
        this.redisKeyBuilder = builder.redisKeyBuilder;
        this.dbLoader = builder.dbLoader;
        this.redisTtl = builder.redisTtl;
        this.redisNullTtl = builder.redisNullTtl;
        this.nullPlaceholder = builder.nullPlaceholder;
        this.evictChannel = CacheConstants.EVICT_CHANNEL_PREFIX + builder.name;
        builder.registry.register(evictChannel, this);
    }

    /**
     * 读取缓存：L1 Caffeine → L2 Redis → L3 DB，逐级回填。
     *
     * @param key 业务主键
     * @return 缓存对象，不存在返回 null
     */
    public V get(K key) {
        if (key == null) {
            return null;
        }
        String redisKey = redisKeyBuilder.apply(key);

        // L1：Caffeine 本地缓存
        V cached = local.getIfPresent(redisKey);
        if (cached != null) {
            log.debug("[多级缓存] name={} key={} 命中 L1（本地）", name, redisKey);
            return cached;
        }

        // L2：Redis 分布式缓存
        String json = redis.get(redisKey);
        if (json != null) {
            if (nullPlaceholder.equals(json)) {
                log.debug("[多级缓存] name={} key={} 命中 L2 空值占位", name, redisKey);
                return null;
            }
            V value = parse(json);
            if (value != null) {
                local.put(redisKey, value);
                log.debug("[多级缓存] name={} key={} 命中 L2（Redis），回填 L1", name, redisKey);
                return value;
            }
            // 反序列化失败视为脏数据，删除后回源
            redis.delete(redisKey);
        }

        // L3：DB
        V loaded = dbLoader.apply(key);
        if (loaded != null) {
            String value = toJson(loaded);
            if (value != null) {
                redis.set(redisKey, value, redisTtl.getSeconds(), TimeUnit.SECONDS);
            }
            local.put(redisKey, loaded);
            log.debug("[多级缓存] name={} key={} 回源 DB，回填 L1+L2", name, redisKey);
        } else {
            redis.set(redisKey, nullPlaceholder, redisNullTtl.getSeconds(), TimeUnit.SECONDS);
            log.debug("[多级缓存] name={} key={} 回源 DB 为空，写入空值占位", name, redisKey);
        }
        return loaded;
    }

    /**
     * 删除缓存（更新 DB 后调用）：删本地 L1 + 删分布式 L2 + 广播通知其它实例删 L1。
     */
    public void evict(K key) {
        if (key == null) {
            return;
        }
        String redisKey = redisKeyBuilder.apply(key);
        local.invalidate(redisKey);
        redis.delete(redisKey);
        redis.publish(evictChannel, redisKey);
        log.debug("[多级缓存] name={} key={} 已删除 L1+L2 并广播失效", name, redisKey);
    }

    /**
     * 仅删除本地 L1 缓存（供广播订阅端调用）。
     */
    void evictLocalByKey(String redisKey) {
        if (redisKey == null) {
            return;
        }
        local.invalidate(redisKey);
        log.debug("[多级缓存] name={} key={} 收到广播，删除本地 L1", name, redisKey);
    }

    private V parse(String json) {
        try {
            return objectMapper.readValue(json, valueType);
        } catch (JsonProcessingException e) {
            log.warn("[多级缓存] name={} 反序列化失败，将回源查库，json={}", name, json, e);
            return null;
        }
    }

    private String toJson(V value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.error("[多级缓存] name={} 序列化失败，本次不写缓存", name, e);
            return null;
        }
    }

    public static <K, V> Builder<K, V> builder() {
        return new Builder<>();
    }

    /**
     * 多级缓存构建器。
     */
    public static final class Builder<K, V> {

        private String name;
        private Cache<String, V> local;
        private RedisUtil redis;
        private ObjectMapper objectMapper;
        private Class<V> valueType;
        private Function<K, String> redisKeyBuilder;
        private Function<K, V> dbLoader;
        private MultiLevelCacheRegistry registry;
        private Duration redisTtl = Duration.ofSeconds(CacheConstants.DEFAULT_REDIS_TTL_SECONDS);
        private Duration redisNullTtl = Duration.ofSeconds(CacheConstants.DEFAULT_NULL_TTL_SECONDS);
        private String nullPlaceholder = CacheConstants.NULL_PLACEHOLDER;

        public Builder<K, V> name(String name) {
            this.name = name;
            return this;
        }

        public Builder<K, V> local(Cache<String, V> local) {
            this.local = local;
            return this;
        }

        public Builder<K, V> redis(RedisUtil redis) {
            this.redis = redis;
            return this;
        }

        public Builder<K, V> objectMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
            return this;
        }

        public Builder<K, V> valueType(Class<V> valueType) {
            this.valueType = valueType;
            return this;
        }

        public Builder<K, V> redisKeyBuilder(Function<K, String> redisKeyBuilder) {
            this.redisKeyBuilder = redisKeyBuilder;
            return this;
        }

        public Builder<K, V> dbLoader(Function<K, V> dbLoader) {
            this.dbLoader = dbLoader;
            return this;
        }

        public Builder<K, V> registry(MultiLevelCacheRegistry registry) {
            this.registry = registry;
            return this;
        }

        public Builder<K, V> redisTtl(Duration redisTtl) {
            this.redisTtl = redisTtl;
            return this;
        }

        public Builder<K, V> redisNullTtl(Duration redisNullTtl) {
            this.redisNullTtl = redisNullTtl;
            return this;
        }

        public Builder<K, V> nullPlaceholder(String nullPlaceholder) {
            this.nullPlaceholder = nullPlaceholder;
            return this;
        }

        public MultiLevelCache<K, V> build() {
            Objects.requireNonNull(name, "缓存名 name 不能为空");
            Objects.requireNonNull(local, "本地缓存 local 不能为空");
            Objects.requireNonNull(redis, "redis 不能为空");
            Objects.requireNonNull(objectMapper, "objectMapper 不能为空");
            Objects.requireNonNull(valueType, "valueType 不能为空");
            Objects.requireNonNull(redisKeyBuilder, "redisKeyBuilder 不能为空");
            Objects.requireNonNull(dbLoader, "dbLoader 不能为空");
            Objects.requireNonNull(registry, "registry 不能为空");
            return new MultiLevelCache<>(this);
        }
    }
}
