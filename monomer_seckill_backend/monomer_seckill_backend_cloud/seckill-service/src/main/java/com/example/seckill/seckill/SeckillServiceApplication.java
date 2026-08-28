package com.example.seckill.seckill;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 秒杀服务启动类。
 *
 * <p>秒杀入口独立成服务，隔离高并发流量；秒杀商品、Redis 预扣库存、秒杀订单
 * 的创建与生命周期均在本服务内闭环，不依赖其它服务。</p>
 *
 * <p>P8 起：秒杀下单异步削峰（Redis 预扣 → MQ → 异步建单），订单超时关闭由死信队列（DLX）
 * 触发，不再依赖 {@code @Scheduled} 定时扫描，故移除 {@code @EnableScheduling}。</p>
 *
 * @author haiy
 * @date 2026/08/17
 */
@SpringBootApplication(scanBasePackages = {
        "com.example.seckill.seckill",
        "com.example.seckill.common.core",
        "com.example.seckill.common.auth",
        "com.example.seckill.common.redis",
        "com.example.seckill.common.web",
        "com.example.seckill.common.mybatis",
        "com.example.seckill.common.cache"
})
@MapperScan("com.example.seckill.seckill.mapper")
public class SeckillServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SeckillServiceApplication.class, args);
    }
}
