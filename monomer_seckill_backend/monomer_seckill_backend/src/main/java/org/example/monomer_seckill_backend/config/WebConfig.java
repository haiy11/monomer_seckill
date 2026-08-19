package org.example.monomer_seckill_backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 全局配置类。
 *
 * <p>配置跨域（CORS）与认证拦截器：前端本地调试访问后端接口需要跨域支持；
 * 用户登录态、管理员权限由对应拦截器统一校验。</p>
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

    /**
     * 注册全局 CORS 规则：允许任意来源、任意方法访问所有接口。
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * 注册认证拦截器。
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 需登录的接口（用户信息、商家申请、购物车、订单、秒杀下单）
        registry.addInterceptor(authInterceptor)
                .addPathPatterns(
                        "/api/user/info",
                        "/api/user/apply-merchant",
                        "/api/cart/**",
                        "/api/order/**",
                        "/api/seckill/**");
        // 商家接口
        registry.addInterceptor(merchantAuthInterceptor)
                .addPathPatterns("/api/merchant/**");
        // 管理员接口（登录接口除外）
        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/api/admin/**")
                .excludePathPatterns("/api/admin/login");
    }
}
