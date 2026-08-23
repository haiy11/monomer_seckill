package com.example.seckill.goodsorder;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 商品/购物车/订单服务启动类。
 *
 * <p>承担正常商品、购物车、订单（含秒杀订单落库）业务，
 * 向 seckill-service 暴露内部建单接口，并在取消/超时关闭秒杀订单时反向调用 seckill-service 回滚 Redis。</p>
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
        "com.example.seckill.common.mybatis"
})
@MapperScan("com.example.seckill.goodsorder.mapper")
@EnableScheduling
@EnableFeignClients
public class GoodsOrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GoodsOrderServiceApplication.class, args);
    }
}
