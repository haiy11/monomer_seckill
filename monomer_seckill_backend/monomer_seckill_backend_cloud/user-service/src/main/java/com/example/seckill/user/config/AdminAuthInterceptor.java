package com.example.seckill.user.config;

import com.example.seckill.common.auth.TokenService;
import com.example.seckill.common.core.Constants;
import com.example.seckill.common.core.UserContext;
import com.example.seckill.common.entity.User;
import com.example.seckill.common.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * 管理员认证拦截器。
 *
 * <p>拦截 /api/admin/**（登录接口除外），校验 token 且要求角色为管理员（role=2）。</p>
 *
 * @author haiy
 * @date 2026/08/17
 */
@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    private final TokenService tokenService;
    private final UserMapper userMapper;

    public AdminAuthInterceptor(TokenService tokenService, UserMapper userMapper) {
        this.tokenService = tokenService;
        this.userMapper = userMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if (CorsUtils.isPreFlightRequest(request)) {
            return true;
        }
        Long userId = tokenService.getUserId(extractToken(request));
        if (userId == null) {
            writeError(response, 401, "未登录或登录已过期");
            return false;
        }
        User user = userMapper.selectById(userId);
        if (user == null || user.getRole() == null || user.getRole() != Constants.ROLE_ADMIN) {
            writeError(response, 403, "无管理员权限");
            return false;
        }
        UserContext.set(userId, user.getRole());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private String extractToken(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7);
        }
        return null;
    }

    private void writeError(HttpServletResponse response, int status, String msg) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":" + status + ",\"msg\":\"" + msg + "\"}");
    }
}
