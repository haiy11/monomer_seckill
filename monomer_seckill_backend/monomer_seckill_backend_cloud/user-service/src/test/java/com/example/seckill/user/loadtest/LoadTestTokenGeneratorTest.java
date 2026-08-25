package com.example.seckill.user.loadtest;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.seckill.common.auth.TokenService;
import com.example.seckill.user.entity.User;
import com.example.seckill.user.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 压测准备：为压测用户批量签发 JWT，生成 JMeter 所需的 tokens.txt。
 *
 * <p>解决「每个请求都要带一个不同用户的 token，手动拿太麻烦」的问题：
 * 用 {@link TokenService} 为每个压测用户签发一个 JWT，逐行写入 {@code docs/tokens.txt}，
 * 供 JMeter 的 CSV 数据源逐行取用。</p>
 *
 * <p>P4 起登录态是无状态 JWT（token 即 JWT 本身，不需要写 Redis），所以本测试比单体版更简单：
 * 直接签发 JWT 落文件即可，token 有效期由 {@code jwt.expire-minutes}（默认 120 分钟）决定。</p>
 *
 * <p>前置条件：先运行 {@link LoadTestUserGeneratorTest} 批量新建用户。</p>
 *
 * @author haiy
 * @date 2026/08/25
 */
@Slf4j
@SpringBootTest
class LoadTestTokenGeneratorTest {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private TokenService tokenService;

    @Test
    void generateTokensAndWriteFile() throws Exception {
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                .likeRight(User::getUsername, LoadTestConfig.USERNAME_PREFIX)
                .orderByAsc(User::getId));
        assertFalse(users.isEmpty(),
                "未找到压测用户（前缀 " + LoadTestConfig.USERNAME_PREFIX
                        + "），请先运行 LoadTestUserGeneratorTest 批量新建用户");

        StringBuilder sb = new StringBuilder();
        for (User user : users) {
            sb.append(tokenService.createToken(user.getId(), user.getRole())).append('\n');
        }

        Path tokensFile = resolveTokensFile();
        // 注意：必须用 \n 换行。tokens.txt 会被 JMeter 的 CSV 数据源逐行读取，
        // 行尾若带 \r 会把 \r 混进 token，导致鉴权失败。
        Files.writeString(tokensFile, sb.toString(), StandardCharsets.UTF_8);

        // 校验：文件行数 = 用户数；无空行
        List<String> lines = Files.readAllLines(tokensFile, StandardCharsets.UTF_8);
        assertEquals(users.size(), lines.size(), "tokens.txt 行数应等于压测用户数");
        assertTrue(lines.stream().allMatch(line -> !line.isBlank()), "token 不允许为空行");

        log.info("已生成 {} 个压测 JWT 到 {}", users.size(), tokensFile.toAbsolutePath());
    }

    /**
     * 定位 tokens.txt 输出路径：向上查找 docs 目录（项目根），找不到则回退到当前目录。
     */
    private Path resolveTokensFile() {
        Path dir = Paths.get(System.getProperty("user.dir"));
        for (Path cur = dir; cur != null; cur = cur.getParent()) {
            Path docs = cur.resolve("docs");
            if (Files.isDirectory(docs)) {
                return docs.resolve("tokens.txt");
            }
        }
        return dir.resolve("tokens.txt");
    }
}
