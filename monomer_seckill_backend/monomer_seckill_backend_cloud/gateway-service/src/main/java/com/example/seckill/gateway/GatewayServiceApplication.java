package com.example.seckill.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 统一流量入口网关启动类（P4）。
 *
 * <p>基于 Spring Cloud Gateway 提供统一入口：路由转发到 user-service /
 * goods-order-service / seckill-service，并在全局过滤器中完成 JWT 鉴权。
 * 网关本身注册到 Nacos，供后续链路追踪/监控发现。</p>
 *
 * @author haiy
 * @date 2026/08/17
 */
@SpringBootApplication
public class GatewayServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayServiceApplication.class, args);
    }
}
