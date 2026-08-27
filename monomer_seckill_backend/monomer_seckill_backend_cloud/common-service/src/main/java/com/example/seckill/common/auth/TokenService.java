package com.example.seckill.common.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * 登录 token 服务（P4 起由 Redis 会话 token 改为无状态 JWT）。
 *
 * <p>JWT 载荷约定：{@code sub}=用户ID、{@code role}=角色（0 普通用户 / 1 商家 / 2 管理员）、
 * {@code iat}/{@code exp}=签发/过期时间。签名算法固定为 HS256，密钥取自配置项
 * {@code jwt.secret}（长度须 ≥ 32 字节）。</p>
 *
 * <p>JWT 无状态：签发后不再写入 Redis，校验只需本地验签 + 过期校验，天然适合网关集中鉴权；
 * 各下游服务仍通过本类做防御性校验，与网关共享同一密钥。</p>
 *
 * <p>P7 起标记 {@code @RefreshScope}：{@code jwt.secret}/{@code jwt.expire-minutes} 迁移到
 * Nacos Config 后，修改配置无需重启即可生效（新密钥立即用于签发/校验，旧 token 随之失效）。</p>
 *
 * @author haiy
 * @date 2026/08/17
 */
@Service
@RefreshScope
public class TokenService {

    /** JWT 载荷：角色字段名 */
    public static final String CLAIM_ROLE = "role";

    /** HMAC 签名密钥（由 jwt.secret 生成） */
    private final SecretKey secretKey;

    /** token 有效期（分钟） */
    private final long expireMinutes;

    public TokenService(@Value("${jwt.secret}") String secret,
                        @Value("${jwt.expire-minutes:120}") long expireMinutes) {
        // HS256 要求密钥 ≥ 256 位（32 字节），jwt.secret 必须满足
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireMinutes = expireMinutes;
    }

    /**
     * 为用户签发 JWT。
     *
     * @param userId 用户ID
     * @param role   用户角色（0 普通用户 / 1 商家 / 2 管理员）
     * @return JWT 字符串
     */
    public String createToken(Long userId, Integer role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expireMinutes * 60_000L);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_ROLE, role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 根据 token 解析 userId。
     *
     * @param token token 字符串（不带前缀）
     * @return userId；token 无效或已过期时返回 null
     */
    public Long getUserId(String token) {
        Claims claims = parse(token);
        if (claims == null) {
            return null;
        }
        String subject = claims.getSubject();
        return subject == null ? null : Long.valueOf(subject);
    }

    /**
     * 根据 token 解析角色。
     *
     * @param token token 字符串（不带前缀）
     * @return 角色值；token 无效或已过期时返回 null
     */
    public Integer getRole(String token) {
        Claims claims = parse(token);
        return claims == null ? null : claims.get(CLAIM_ROLE, Integer.class);
    }

    /**
     * 解析并校验 JWT：验签 + 过期校验。
     *
     * @return 有效时返回 Claims，否则返回 null
     */
    private Claims parse(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            // 签名不匹配 / 已过期 / 格式非法统一按未登录处理，不向外暴露细节
            return null;
        }
    }
}
