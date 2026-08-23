package com.example.seckill.seckill.config;

import com.example.seckill.common.auth.AuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 秒杀服务 Web 配置：仅注册登录拦截器（CORS 由 common 统一处理）。
 *
 * @author haiy
 * @date 2026/08/17
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    public WebConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 秒杀下单需登录
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/seckill/**");
    }
}
