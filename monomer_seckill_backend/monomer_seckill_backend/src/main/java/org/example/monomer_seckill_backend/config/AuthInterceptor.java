package org.example.monomer_seckill_backend.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.monomer_seckill_backend.common.TokenService;
import org.example.monomer_seckill_backend.common.UserContext;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * 用户认证拦截器。
 *
 * <p>拦截需登录的普通用户接口（/api/user/info、/api/order/**），
 * 校验 Authorization 头中的 token，解析出 userId 写入 {@link UserContext}。</p>
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
        Long userId = tokenService.getUserId(extractToken(request));
        if (userId == null) {
            writeError(response, 401, "未登录或登录已过期");
            return false;
        }
        UserContext.set(userId, null);
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
