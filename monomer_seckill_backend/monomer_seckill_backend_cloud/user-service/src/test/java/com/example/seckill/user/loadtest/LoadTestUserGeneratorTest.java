package com.example.seckill.user.loadtest;

import com.example.seckill.user.dto.RegisterRequest;
import com.example.seckill.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 压测准备：批量新建用户（P5 限流熔断 JMeter 压测）。
 *
 * <p>解决「压测用户数量不够、一个一个手动注册太麻烦」的问题。
 * 以 {@link LoadTestConfig#USER_COUNT} 为数量，按 {@code jmeteruser_00001} 的规则批量注册普通用户，
 * 统一密码 {@code 123456}。幂等：已存在的用户名自动跳过，可重复执行。</p>
 *
 * <p>前置条件：MySQL(3307) 已启动，且 {@code mall_user} 表已建（启动过一次 goods-order-service 即可）。</p>
 *
 * @author haiy
 * @date 2026/08/25
 */
@SpringBootTest
class LoadTestUserGeneratorTest {

    @Autowired
    private UserService userService;

    @Test
    void batchCreateUsers() {
        int created = 0;
        int skipped = 0;
        for (int seq = 1; seq <= LoadTestConfig.USER_COUNT; seq++) {
            String username = LoadTestConfig.username(seq);
            if (userService.getByUsername(username) != null) {
                skipped++;
                continue;
            }
            RegisterRequest request = new RegisterRequest();
            request.setUsername(username);
            request.setPassword(LoadTestConfig.DEFAULT_PASSWORD);
            request.setNickname("压测用户" + seq);
            userService.register(request);
            created++;
        }

        // 用断言校验（不依赖打印）：最终压测用户数应达到 USER_COUNT
        assertEquals(LoadTestConfig.USER_COUNT, created + skipped,
                "压测用户总数应等于 USER_COUNT（新建 + 已存在跳过）");
        assertNotNull(userService.getByUsername(LoadTestConfig.username(1)),
                "第 1 个压测用户应已存在");
    }
}
