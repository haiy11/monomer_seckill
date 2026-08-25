package com.example.seckill.user.loadtest;

/**
 * 压测（P5 限流熔断 JMeter 压测）数据准备相关的共享配置。
 *
 * <p>两个压测准备测试共用这里的常量，保证「批量新建用户」与「生成 token」使用同一套用户名规则，
 * 使 token 能正确对应到数据库中的真实用户 ID。</p>
 *
 * @author haiy
 * @date 2026/08/25
 */
public final class LoadTestConfig {

    private LoadTestConfig() {
    }

    /** 批量新建的压测用户数量（改这里即可调整数量） */
    public static final int USER_COUNT = 2000;

    /** 压测用户名统一前缀（与 P2 单体的 loaduser_ 区分，避免共库时混淆） */
    public static final String USERNAME_PREFIX = "jmeteruser_";

    /** 压测用户统一登录密码 */
    public static final String DEFAULT_PASSWORD = "123456";

    /**
     * 按序号生成压测用户名，如 jmeteruser_00001。
     *
     * @param seq 序号（从 1 开始）
     * @return 用户名
     */
    public static String username(int seq) {
        return USERNAME_PREFIX + String.format("%05d", seq);
    }
}
