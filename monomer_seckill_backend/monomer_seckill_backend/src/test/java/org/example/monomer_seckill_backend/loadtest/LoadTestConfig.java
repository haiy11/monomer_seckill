package org.example.monomer_seckill_backend.loadtest;

/**
 * 压测（P2 秒杀 wrk 压测）数据准备相关的共享配置。
 *
 * <p>两个压测准备测试共用这里的常量，保证「批量新建用户」与「生成 token」使用同一套用户名规则，
 * 使 token 能正确对应到数据库中的真实用户 ID。</p>
 *
 * @author haiy
 * @date 2026/08/17
 */
public final class LoadTestConfig {

    private LoadTestConfig() {
    }

    /** 批量新建的压测用户数量（改这里即可调整数量） */
    public static final int USER_COUNT = 20000;

    /** 压测用户名统一前缀 */
    public static final String USERNAME_PREFIX = "loaduser_";

    /** 压测用户统一登录密码 */
    public static final String DEFAULT_PASSWORD = "123456";

    /** 压测 token 有效期（小时），比登录 token 的 10 分钟长，避免压测期间过期 */
    public static final long TOKEN_TTL_HOURS = 24;

    /** Redis 中记录压测 token 的集合 key，重复运行前用于清理旧 token */
    public static final String LOADTEST_TOKENS_KEY = "loadtest:tokens";

    /**
     * 按序号生成压测用户名，如 loaduser_00001。
     *
     * @param seq 序号（从 1 开始）
     * @return 用户名
     */
    public static String username(int seq) {
        return USERNAME_PREFIX + String.format("%05d", seq);
    }
}
