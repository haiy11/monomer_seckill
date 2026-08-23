package com.example.seckill.user;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 用户/管理员/商家服务启动类。
 *
 * <p>承担用户注册登录、商家申请，以及管理员审核、商家商品管理等人员相关业务；
 * 商品/订单/秒杀商品等审核与管理操作经 OpenFeign 调用对应服务。</p>
 *
 * @author haiy
 * @date 2026/08/17
 */
@SpringBootApplication(scanBasePackages = {
        "com.example.seckill.user",
        "com.example.seckill.common.core",
        "com.example.seckill.common.auth",
        "com.example.seckill.common.redis",
        "com.example.seckill.common.web",
        "com.example.seckill.common.mybatis"
})
@MapperScan("com.example.seckill.user.mapper")
@EnableFeignClients
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
