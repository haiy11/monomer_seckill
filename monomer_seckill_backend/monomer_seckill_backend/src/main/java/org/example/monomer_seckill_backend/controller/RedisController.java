package org.example.monomer_seckill_backend.controller;

import org.example.monomer_seckill_backend.common.Result;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Redis 连通性测试接口
 */
@RestController
@RequestMapping("/api/redis")
public class RedisController {

    private final StringRedisTemplate stringRedisTemplate;

    public RedisController(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @GetMapping("/ping")
    public Result<String> ping() {
        stringRedisTemplate.opsForValue().set("seckill:ping", "pong");
        String value = stringRedisTemplate.opsForValue().get("seckill:ping");
        return Result.ok(value);
    }
}
