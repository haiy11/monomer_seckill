package com.example.seckill.seckill;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 秒杀服务启动类。
 *
 * <p>秒杀入口独立成服务，隔离高并发流量；本服务持有秒杀商品与 Redis 预扣库存，
 * 下单时通过 OpenFeign 调用 goods-order-service 完成 DB 扣减与订单落库。</p>
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
        "com.example.seckill.common.mybatis"
})
@MapperScan("com.example.seckill.seckill.mapper")
@EnableFeignClients
public class SeckillServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SeckillServiceApplication.class, args);
    }
}
