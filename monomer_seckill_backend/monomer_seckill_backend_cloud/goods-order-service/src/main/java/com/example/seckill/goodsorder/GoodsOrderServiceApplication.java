package com.example.seckill.goodsorder;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 商品/购物车/订单服务启动类。
 *
 * <p>承担商品、秒杀商品、购物车、订单（含秒杀订单）业务，
 * 并向 seckill-service 暴露内部扣库存/下单接口（OpenFeign 调用）。</p>
 *
 * @author haiy
 * @date 2026/08/17
 */
@SpringBootApplication(scanBasePackages = {
        "com.example.seckill.goodsorder",
        "com.example.seckill.common.core",
        "com.example.seckill.common.auth",
        "com.example.seckill.common.redis",
        "com.example.seckill.common.web",
        "com.example.seckill.common.mybatis",
        "com.example.seckill.common.service"
})
@MapperScan("com.example.seckill.common.mapper")
@EnableScheduling
public class GoodsOrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GoodsOrderServiceApplication.class, args);
    }
}
