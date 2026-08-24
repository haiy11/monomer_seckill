package com.example.seckill.gateway.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * 网关侧 JWT 解析工具。
 *
 * <p>与 common-service 的 {@code TokenService} 保持同一密钥、同一算法（HS256）与同一载荷约定：
 * {@code sub}=用户ID、{@code role}=角色。网关只负责校验与解析，不签发 token（签发在 user-service 登录时完成）。</p>
 *
 * @author haiy
 * @date 2026/08/17
 */
@Component
public class JwtUtil {

    /** JWT 载荷：角色字段名（与 TokenService.CLAIM_ROLE 保持一致） */
    public static final String CLAIM_ROLE = "role";

    /** HMAC 签名密钥 */
    private final SecretKey secretKey;

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 解析并校验 JWT：验签 + 过期校验。
     *
     * @param token token 字符串（不含 Bearer 前缀）
     * @return 有效时返回 {@link JwtInfo}，否则返回 null
     */
    public JwtInfo parse(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            String subject = claims.getSubject();
            Long userId = subject == null ? null : Long.valueOf(subject);
            Integer role = claims.get(CLAIM_ROLE, Integer.class);
            return new JwtInfo(userId, role);
        } catch (Exception e) {
            // 签名不匹配 / 已过期 / 格式非法统一视为未登录
            return null;
        }
    }

    /**
     * JWT 解析结果。
     */
    public record JwtInfo(Long userId, Integer role) {
    }
}
