package com.example.seckill.seckill.config;

import com.example.seckill.common.cache.MultiLevelCache;
import com.example.seckill.common.cache.MultiLevelCacheRegistry;
import com.example.seckill.common.redis.RedisUtil;
import com.example.seckill.seckill.constant.SeckillConstants;
import com.example.seckill.seckill.entity.SeckillGoods;
import com.example.seckill.seckill.mapper.SeckillGoodsMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 秒杀服务多级缓存配置。
 *
 * <p>构建「热点秒杀商品信息」的多级缓存：L1 Caffeine 本地缓存 → L2 Redis 分布式缓存 → L3 DB。
 * 秒杀商品详情是秒杀场景下最热、QPS 最高的读路径，本地缓存可大幅降低 Redis/DB 压力与响应时间。</p>
 *
 * @author haiy
 * @date 2026/08/17
 */
@Configuration
public class CacheConfig {

    /**
     * 秒杀商品多级缓存 Bean。
     */
    @Bean
    public MultiLevelCache<Long, SeckillGoods> seckillGoodsCache(RedisUtil redisUtil,
                                                                  ObjectMapper objectMapper,
                                                                  SeckillGoodsMapper seckillGoodsMapper,
                                                                  MultiLevelCacheRegistry registry) {
        return MultiLevelCache.<Long, SeckillGoods>builder()
                .name("seckill-goods")
                .local(Caffeine.<String, SeckillGoods>newBuilder()
                        .maximumSize(SeckillConstants.GOODS_LOCAL_CACHE_MAX_SIZE)
                        .expireAfterWrite(Duration.ofSeconds(SeckillConstants.GOODS_LOCAL_CACHE_TTL_SECONDS))
                        .recordStats()
                        .build())
                .redis(redisUtil)
                .objectMapper(objectMapper)
                .valueType(SeckillGoods.class)
                .redisKeyBuilder(id -> SeckillConstants.GOODS_KEY_PREFIX + id)
                .dbLoader(seckillGoodsMapper::selectById)
                .redisTtl(Duration.ofSeconds(SeckillConstants.GOODS_CACHE_TTL_SECONDS))
                .redisNullTtl(Duration.ofSeconds(SeckillConstants.GOODS_NULL_TTL_SECONDS))
                .nullPlaceholder(SeckillConstants.GOODS_CACHE_NULL)
                .registry(registry)
                .build();
    }
}
