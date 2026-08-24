package com.example.seckill.gateway.filter;

import com.example.seckill.gateway.auth.JwtUtil;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 全局 JWT 鉴权过滤器（P4）。
 *
 * <p>统一流量入口的鉴权点：对受保护接口校验 {@code Authorization: Bearer <jwt>}，
 * 并按路径做角色校验（管理员/商家）。公开接口（注册、登录、商品/秒杀商品查询）直接放行。
 * 鉴权通过后把用户身份写入 {@code X-User-Id} / {@code X-User-Role} 头透传给下游。</p>
 *
 * @author haiy
 * @date 2026/08/17
 */
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    /** 用户角色：普通用户 */
    private static final int ROLE_USER = 0;
    /** 用户角色：商家 */
    private static final int ROLE_MERCHANT = 1;
    /** 用户角色：管理员 */
    private static final int ROLE_ADMIN = 2;

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    public AuthGlobalFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        HttpMethod method = request.getMethod();

        // 服务间内部接口（/internal/**）不对外暴露，即便误配路由也直接拒绝
        if (path.startsWith("/internal")) {
            return writeError(exchange, HttpStatus.NOT_FOUND, "接口不存在");
        }
        // 非业务路径（如 /actuator）交给路由处理，未配置路由自然 404
        if (!path.startsWith("/api")) {
            return chain.filter(exchange);
        }
        // CORS 预检请求放行（跨域头由 globalcors 统一处理）
        if (HttpMethod.OPTIONS.equals(method)) {
            return chain.filter(exchange);
        }
        // 公开接口无需登录
        if (isPublicPath(method, path)) {
            return chain.filter(exchange);
        }

        // 受保护接口：校验 JWT
        JwtUtil.JwtInfo info = jwtUtil.parse(extractToken(request));
        if (info == null || info.userId() == null) {
            return writeError(exchange, HttpStatus.UNAUTHORIZED, "未登录或登录已过期");
        }
        // 管理员接口：要求 role=2
        if (path.startsWith("/api/admin/") && !hasRole(info, ROLE_ADMIN)) {
            return writeError(exchange, HttpStatus.FORBIDDEN, "无管理员权限");
        }
        // 商家接口：要求 role=1
        if (path.startsWith("/api/merchant/") && !hasRole(info, ROLE_MERCHANT)) {
            return writeError(exchange, HttpStatus.FORBIDDEN, "无商家权限");
        }

        // 鉴权通过，把身份透传给下游（下游当前仍会自行校验 JWT，此处为后续信任链预留）
        ServerHttpRequest mutated = request.mutate()
                .header("X-User-Id", String.valueOf(info.userId()))
                .header("X-User-Role", String.valueOf(info.role()))
                .build();
        return chain.filter(exchange.mutate().request(mutated).build());
    }

    /**
     * 判断是否公开接口（无需登录）。
     */
    private boolean isPublicPath(HttpMethod method, String path) {
        // 商品 / 秒杀商品查询公开
        if (HttpMethod.GET.equals(method)) {
            return path.startsWith("/api/goods") || path.startsWith("/api/seckill-goods");
        }
        // 注册 / 用户登录 / 管理员登录公开
        if (HttpMethod.POST.equals(method)) {
            return path.equals("/api/user/register")
                    || path.equals("/api/user/login")
                    || path.equals("/api/admin/login");
        }
        return false;
    }

    /**
     * 判断解析出的角色是否匹配期望角色（空角色视为不匹配）。
     */
    private boolean hasRole(JwtUtil.JwtInfo info, int expected) {
        return info.role() != null && info.role() == expected;
    }

    /**
     * 从 Authorization 头提取 Bearer token。
     */
    private String extractToken(ServerHttpRequest request) {
        String auth = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (auth != null && auth.startsWith(BEARER_PREFIX)) {
            return auth.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    /**
     * 写出统一 JSON 错误响应（与下游 Result 结构一致：code/msg/data）。
     */
    private Mono<Void> writeError(ServerWebExchange exchange, HttpStatus status, String msg) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.parseMediaType("application/json;charset=UTF-8"));
        String body = "{\"code\":" + status.value() + ",\"msg\":\"" + msg + "\",\"data\":null}";
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        // 提前于路由相关过滤器执行，确保未鉴权的请求不会到达下游
        return -100;
    }
}
