package org.example.monomer_seckill_backend.common;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * 登录 token 服务。
 *
 * <p>P2 阶段用「Redis 存储 token → userId」的方式做轻量会话，
 * 为 P4 网关 JWT 鉴权打基础（届时替换为 JWT 即可，接口语义不变）。</p>
 *
 * @author haiy
 * @date 2026/08/17
 */
@Service
public class TokenService {

    /** Redis 字符串模板 */
    private final StringRedisTemplate stringRedisTemplate;

    public TokenService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 为用户签发 token，并写入 Redis（带过期时间）。
     *
     * @param userId 用户ID
     * @return token 字符串
     */
    public String createToken(Long userId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        stringRedisTemplate.opsForValue().set(
                Constants.TOKEN_KEY_PREFIX + token,
                String.valueOf(userId),
                Duration.ofMinutes(Constants.TOKEN_TTL_MINUTES));
        return token;
    }

    /**
     * 根据 token 解析 userId。
     *
     * @param token token 字符串（不带前缀）
     * @return userId；token 无效或已过期时返回 null
     */
    public Long getUserId(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String value = stringRedisTemplate.opsForValue().get(Constants.TOKEN_KEY_PREFIX + token);
        return value == null ? null : Long.valueOf(value);
    }

    /**
     * 删除 token（登出 / 失效）。
     *
     * @param token token 字符串
     */
    public void deleteToken(String token) {
        if (token != null && !token.isBlank()) {
            stringRedisTemplate.delete(Constants.TOKEN_KEY_PREFIX + token);
        }
    }
}
