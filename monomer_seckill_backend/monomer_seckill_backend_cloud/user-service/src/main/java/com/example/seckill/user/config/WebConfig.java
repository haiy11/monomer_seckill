package com.example.seckill.user.config;

import com.example.seckill.common.auth.AuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 用户服务 Web 配置：注册登录/商家/管理员三类拦截器（CORS 由 common 统一处理）。
 *
 * @author haiy
 * @date 2026/08/17
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final MerchantAuthInterceptor merchantAuthInterceptor;
    private final AdminAuthInterceptor adminAuthInterceptor;

    public WebConfig(AuthInterceptor authInterceptor, MerchantAuthInterceptor merchantAuthInterceptor,
                     AdminAuthInterceptor adminAuthInterceptor) {
        this.authInterceptor = authInterceptor;
        this.merchantAuthInterceptor = merchantAuthInterceptor;
        this.adminAuthInterceptor = adminAuthInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 需登录的接口（用户信息、商家申请）
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/user/info", "/api/user/apply-merchant");
        // 商家接口
        registry.addInterceptor(merchantAuthInterceptor)
                .addPathPatterns("/api/merchant/**");
        // 管理员接口（登录接口除外）
        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/api/admin/**")
                .excludePathPatterns("/api/admin/login");
    }
}
