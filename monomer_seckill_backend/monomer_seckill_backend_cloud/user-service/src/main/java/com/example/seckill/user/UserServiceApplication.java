package com.example.seckill.user;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 用户/管理员/商家服务启动类。
 *
 * <p>承担用户注册登录、商家申请，以及管理员审核、商家商品管理等人员相关业务。</p>
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
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
