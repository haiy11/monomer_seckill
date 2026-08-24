package com.example.seckill.common.auth;

import com.example.seckill.common.core.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * 用户认证拦截器（仅校验登录态，不校验角色）。
 *
 * <p>校验 Authorization 头中的 token，解析出 userId 写入 {@link UserContext}。
 * 各服务在自身 WebConfig 中按需注册到受保护路径上。</p>
 *
 * @author haiy
 * @date 2026/08/17
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final TokenService tokenService;

    public AuthInterceptor(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        // 放行 CORS 预检请求（OPTIONS），否则带 Authorization 头的跨域请求会被误拦
        if (CorsUtils.isPreFlightRequest(request)) {
            return true;
        }
        String token = extractToken(request);
        Long userId = tokenService.getUserId(token);
        if (userId == null) {
            writeError(response, 401, "未登录或登录已过期");
            return false;
        }
        UserContext.set(userId, tokenService.getRole(token));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    /**
     * 从 Authorization 头提取 Bearer token。
     */
    private String extractToken(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7);
        }
        return null;
    }

    /**
     * 写出 JSON 格式的错误响应。
     */
    private void writeError(HttpServletResponse response, int status, String msg) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":" + status + ",\"msg\":\"" + msg + "\"}");
    }
}
