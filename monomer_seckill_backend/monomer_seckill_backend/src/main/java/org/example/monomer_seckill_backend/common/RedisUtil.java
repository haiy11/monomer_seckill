package org.example.monomer_seckill_backend.common;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Redis 工具类。
 *
 * <p>基于 {@link StringRedisTemplate} 封装常用命令，供秒杀预扣库存、token 存储等场景复用。
 * 秒杀计数、用户集合均为字符串/整数操作，用 StringRedisTemplate 最简单且高效。</p>
 *
 * @author haiy
 * @date 2026/08/17
 */
@Component
public class RedisUtil {

    /** Redis 字符串模板 */
    private final StringRedisTemplate stringRedisTemplate;

    public RedisUtil(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 写入字符串（永久有效）。
     */
    public void set(String key, String value) {
        stringRedisTemplate.opsForValue().set(key, value);
    }

    /**
     * 写入字符串并设置过期时间。
     */
    public void set(String key, String value, long timeout, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    /**
     * 读取字符串。
     */
    public String get(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    /**
     * 仅当 key 不存在时写入（常用于懒加载预扣库存，避免并发重复初始化）。
     */
    public Boolean setIfAbsent(String key, String value) {
        return stringRedisTemplate.opsForValue().setIfAbsent(key, value);
    }

    /**
     * 仅当 key 不存在时写入，并设置过期时间（常用于空值缓存等需要自动过期的场景）。
     */
    public Boolean setIfAbsent(String key, String value, long timeout, TimeUnit unit) {
        return stringRedisTemplate.opsForValue().setIfAbsent(key, value, timeout, unit);
    }

    /**
     * 自增 1。
     */
    public Long increment(String key) {
        return stringRedisTemplate.opsForValue().increment(key);
    }

    /**
     * 自减 1。
     */
    public Long decrement(String key) {
        return stringRedisTemplate.opsForValue().decrement(key);
    }

    /**
     * 向集合添加成员。
     */
    public Long sAdd(String key, String... values) {
        return stringRedisTemplate.opsForSet().add(key, values);
    }

    /**
     * 判断成员是否在集合中。
     */
    public Boolean sIsMember(String key, Object value) {
        return stringRedisTemplate.opsForSet().isMember(key, value);
    }

    /**
     * 从集合移除成员。
     */
    public Long sRemove(String key, Object... values) {
        return stringRedisTemplate.opsForSet().remove(key, values);
    }

    /**
     * 判断 key 是否存在。
     */
    public Boolean hasKey(String key) {
        return stringRedisTemplate.hasKey(key);
    }

    /**
     * 删除 key。
     */
    public Boolean delete(String key) {
        return stringRedisTemplate.delete(key);
    }

    /**
     * 执行 Lua 脚本（返回 Long 类型结果）。
     *
     * @param script Lua 脚本
     * @param keys   KEYS 参数
     * @param args   ARGV 参数
     * @return 脚本返回的 Long 结果
     */
    public Long executeScript(RedisScript<Long> script, List<String> keys, Object... args) {
        return stringRedisTemplate.execute(script, keys, args);
    }
}
