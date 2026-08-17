package org.example.monomer_seckill_backend.controller;

import org.example.monomer_seckill_backend.common.Result;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Redis 连通性测试接口。
 *
 * @author haiy
 * @date 2026/08/17
 */
@RestController
@RequestMapping("/api/redis")
public class RedisController {

    /** Redis 模板（String 值类型），由 Spring Boot 自动配置注入 */
    private final StringRedisTemplate stringRedisTemplate;

    public RedisController(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * Redis 连通性测试：写入并读取一个 key，验证连接可用。
     *
     * @return 读回的值，正常为 pong
     */
    @GetMapping("/ping")
    public Result<String> ping() {
        stringRedisTemplate.opsForValue().set("seckill:ping", "pong");
        String value = stringRedisTemplate.opsForValue().get("seckill:ping");
        return Result.ok(value);
    }
}
