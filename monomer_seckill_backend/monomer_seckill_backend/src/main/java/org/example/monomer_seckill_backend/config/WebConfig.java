package org.example.monomer_seckill_backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 全局配置类。
 *
 * <p>配置跨域（CORS）：前端本地调试（file:// 或独立端口）访问后端接口时需要跨域支持。</p>
 *
 * @author haiy
 * @date 2026/08/17
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 注册全局 CORS 规则：允许任意来源、任意方法访问所有接口。
     *
     * @param registry CORS 注册表
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
}
