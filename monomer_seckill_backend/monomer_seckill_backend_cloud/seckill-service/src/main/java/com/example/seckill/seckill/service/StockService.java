package com.example.seckill.seckill.service;

import com.example.seckill.common.redis.RedisUtil;
import com.example.seckill.seckill.constant.SeckillConstants;
import com.example.seckill.seckill.entity.SeckillGoods;
import com.example.seckill.seckill.mapper.SeckillGoodsMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 秒杀库存服务（Redis 预扣库存核心，秒杀域专属）。
 *
 * <p>把秒杀库存预载到 Redis，用 Lua 脚本原子完成「判库存 → 判重复 → 扣库存 → 记用户」。
 * 数据库侧秒杀库存的持久化扣减/回补由 goods-order-service 在建单时完成，
 * 本服务只负责 Redis 这一高并发热路径。</p>
 *
 * @author haiy
 * @date 2026/08/17
 */
@Service
public class StockService {

    private final RedisUtil redisUtil;
    private final SeckillGoodsMapper seckillGoodsMapper;

    private final RedisScript<Long> deductScript;
    private final RedisScript<Long> rollbackScript;

    public StockService(RedisUtil redisUtil, SeckillGoodsMapper seckillGoodsMapper) {
        this.redisUtil = redisUtil;
        this.seckillGoodsMapper = seckillGoodsMapper;
        this.deductScript = loadScript("lua/seckill_deduct.lua");
        this.rollbackScript = loadScript("lua/seckill_rollback.lua");
    }

    /**
     * 预载单个秒杀商品的库存到 Redis，并清空已购用户集合。
     */
    public void preload(Long seckillGoodsId) {
        SeckillGoods sg = seckillGoodsMapper.selectById(seckillGoodsId);
        if (sg == null) {
            return;
        }
        redisUtil.set(stockKey(seckillGoodsId), String.valueOf(sg.getSeckillStock()));
        redisUtil.delete(usersKey(seckillGoodsId));
    }

    /**
     * 预载全部秒杀商品库存到 Redis。
     */
    public void preloadAll() {
        List<SeckillGoods> list = seckillGoodsMapper.selectList(null);
        for (SeckillGoods sg : list) {
            redisUtil.set(stockKey(sg.getId()), String.valueOf(sg.getSeckillStock()));
            redisUtil.delete(usersKey(sg.getId()));
        }
    }

    /**
     * 懒加载：库存 key 不存在时从数据库初始化。
     */
    public void ensureLoaded(Long seckillGoodsId) {
        String stockKey = stockKey(seckillGoodsId);
        if (Boolean.TRUE.equals(redisUtil.hasKey(stockKey))) {
            return;
        }
        SeckillGoods sg = seckillGoodsMapper.selectById(seckillGoodsId);
        if (sg != null) {
            redisUtil.setIfAbsent(stockKey, String.valueOf(sg.getSeckillStock()));
        } else {
            redisUtil.setIfAbsent(stockKey, SeckillConstants.STOCK_NULL_VALUE,
                    SeckillConstants.STOCK_NULL_TTL_SECONDS, TimeUnit.SECONDS);
        }
    }

    /**
     * Redis 原子扣库存（Lua）。
     */
    public long deduct(Long seckillGoodsId, Long userId) {
        ensureLoaded(seckillGoodsId);
        Long result = redisUtil.executeScript(deductScript,
                List.of(stockKey(seckillGoodsId), usersKey(seckillGoodsId)),
                String.valueOf(userId));
        return result == null ? SeckillConstants.LUA_STOCK_EMPTY : result;
    }

    /**
     * 仅回滚 Redis（下单 DB 写入失败 / 取消 / 超时关闭时由调用方触发）。
     */
    public void rollbackRedis(Long seckillGoodsId, Long userId) {
        redisUtil.executeScript(rollbackScript,
                List.of(stockKey(seckillGoodsId), usersKey(seckillGoodsId)),
                String.valueOf(userId));
    }

    private String stockKey(Long seckillGoodsId) {
        return SeckillConstants.STOCK_KEY_PREFIX + seckillGoodsId;
    }

    private String usersKey(Long seckillGoodsId) {
        return SeckillConstants.USERS_KEY_PREFIX + seckillGoodsId;
    }

    private RedisScript<Long> loadScript(String path) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource(path));
        script.setResultType(Long.class);
        return script;
    }
}
