package com.example.seckill.common.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 全局跨域配置（各服务共享）。
 *
 * <p>前端本地调试访问后端接口需要跨域支持，统一在此放行任意来源。
 * 拦截器注册由各服务自身 WebConfig 负责。</p>
 *
 * @author haiy
 * @date 2026/08/17
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
