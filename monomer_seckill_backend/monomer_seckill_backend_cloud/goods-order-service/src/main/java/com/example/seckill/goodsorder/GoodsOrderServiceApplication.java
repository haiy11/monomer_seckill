package com.example.seckill.goodsorder;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 商品/购物车/订单服务启动类。
 *
 * <p>承担正常商品、购物车、订单业务；秒杀商品/库存/秒杀订单已下沉到 seckill-service。</p>
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
        "com.example.seckill.common.cache"
})
@MapperScan("com.example.seckill.goodsorder.mapper")
@EnableScheduling
public class GoodsOrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GoodsOrderServiceApplication.class, args);
    }
}
