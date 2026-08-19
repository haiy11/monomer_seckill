package org.example.monomer_seckill_backend.common;

/**
 * 当前登录用户上下文（ThreadLocal）。
 *
 * <p>由认证拦截器在请求进入时写入，Controller/Service 通过
 * {@link #getUserId()} / {@link #getRole()} 读取；请求结束后在 afterCompletion 中清理。</p>
 *
 * @author haiy
 * @date 2026/08/17
 */
public final class UserContext {

    /** 当前请求用户ID */
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    /** 当前请求用户角色 */
    private static final ThreadLocal<Integer> ROLE = new ThreadLocal<>();

    private UserContext() {
    }

    /**
     * 写入当前用户信息。
     */
    public static void set(Long userId, Integer role) {
        USER_ID.set(userId);
        ROLE.set(role);
    }

    /**
     * 获取当前用户ID。
     */
    public static Long getUserId() {
        return USER_ID.get();
    }

    /**
     * 获取当前用户角色。
     */
    public static Integer getRole() {
        return ROLE.get();
    }

    /**
     * 清理上下文，防止线程复用导致数据串扰。
     */
    public static void clear() {
        USER_ID.remove();
        ROLE.remove();
    }
}
