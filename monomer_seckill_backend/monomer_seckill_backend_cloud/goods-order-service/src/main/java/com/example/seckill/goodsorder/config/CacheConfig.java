package com.example.seckill.goodsorder.config;

import com.example.seckill.common.cache.MultiLevelCache;
import com.example.seckill.common.cache.MultiLevelCacheRegistry;
import com.example.seckill.common.redis.RedisUtil;
import com.example.seckill.goodsorder.constant.GoodsOrderConstants;
import com.example.seckill.goodsorder.entity.Goods;
import com.example.seckill.goodsorder.mapper.GoodsMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 商品/订单服务多级缓存配置。
 *
 * <p>构建「正常商品信息」的多级缓存：L1 Caffeine 本地缓存 → L2 Redis 分布式缓存 → L3 DB。
 * 商品详情为高频读路径；库存变化（下单扣减/取消回补）与商品审核/上下架均会触发缓存删除。</p>
 *
 * @author haiy
 * @date 2026/08/17
 */
@Configuration
public class CacheConfig {

    /**
     * 商品多级缓存 Bean。
     */
    @Bean
    public MultiLevelCache<Long, Goods> goodsCache(RedisUtil redisUtil,
                                                    ObjectMapper objectMapper,
                                                    GoodsMapper goodsMapper,
                                                    MultiLevelCacheRegistry registry) {
        return MultiLevelCache.<Long, Goods>builder()
                .name("goods")
                .local(Caffeine.<String, Goods>newBuilder()
                        .maximumSize(GoodsOrderConstants.GOODS_LOCAL_CACHE_MAX_SIZE)
                        .expireAfterWrite(Duration.ofSeconds(GoodsOrderConstants.GOODS_LOCAL_CACHE_TTL_SECONDS))
                        .recordStats()
                        .build())
                .redis(redisUtil)
                .objectMapper(objectMapper)
                .valueType(Goods.class)
                .redisKeyBuilder(id -> GoodsOrderConstants.GOODS_KEY_PREFIX + id)
                .dbLoader(goodsMapper::selectById)
                .redisTtl(Duration.ofSeconds(GoodsOrderConstants.GOODS_CACHE_TTL_SECONDS))
                .redisNullTtl(Duration.ofSeconds(GoodsOrderConstants.GOODS_NULL_TTL_SECONDS))
                .registry(registry)
                .build();
    }
}
