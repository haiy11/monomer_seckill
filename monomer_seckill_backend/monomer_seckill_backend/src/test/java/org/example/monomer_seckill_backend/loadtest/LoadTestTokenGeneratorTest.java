package org.example.monomer_seckill_backend.loadtest;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.example.monomer_seckill_backend.common.Constants;
import org.example.monomer_seckill_backend.common.TokenService;
import org.example.monomer_seckill_backend.user.entity.User;
import org.example.monomer_seckill_backend.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 压测准备：在 Redis 中批量生成 token，并生成 wrk 所需的 tokens.txt。
 *
 * <p>解决「每次压测都要手动往 Redis 写一堆 token」的问题：
 * 为每个压测用户生成一个 UUID token，按 {@code mall:token:{token}} → userId 写入 Redis
 * （与 {@link TokenService} 同一 key 格式，value 存 userId），并把所有 token 逐行写入
 * {@code docs/tokens.txt}，供 {@code docs/post.lua} 读取。</p>
 *
 * <p>前置条件：先运行 {@link LoadTestUserGeneratorTest} 批量新建用户；MySQL/Redis 已启动。</p>
 *
 * @author haiy
 * @date 2026/08/17
 */
@Slf4j
@SpringBootTest
class LoadTestTokenGeneratorTest {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void generateTokensAndWriteFile() throws Exception {
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                .likeRight(User::getUsername, LoadTestConfig.USERNAME_PREFIX)
                .orderByAsc(User::getId));
        assertFalse(users.isEmpty(),
                "未找到压测用户（前缀 " + LoadTestConfig.USERNAME_PREFIX
                        + "），请先运行 LoadTestUserGeneratorTest 批量新建用户");

        // 清理上一次运行生成的旧 token，避免 Redis 中堆积
        cleanupPreviousTokens();

        StringBuilder sb = new StringBuilder();
        String firstToken = null;
        Long firstUserId = null;
        for (User user : users) {
            String token = UUID.randomUUID().toString().replace("-", "");
            stringRedisTemplate.opsForValue().set(
                    Constants.TOKEN_KEY_PREFIX + token,
                    String.valueOf(user.getId()),
                    Duration.ofHours(LoadTestConfig.TOKEN_TTL_HOURS));
            stringRedisTemplate.opsForSet().add(LoadTestConfig.LOADTEST_TOKENS_KEY, token);
            sb.append(token).append('\n');
            if (firstToken == null) {
                firstToken = token;
                firstUserId = user.getId();
            }
        }

        Path tokensFile = resolveTokensFile();
        // 注意：必须用 \n 换行。tokens.txt 会在 WSL 中被 Lua 的 io.lines 读取，
        // Lua 不会剥离行尾的 \r，Windows 的 \r\n 会把 \r 带入 token 导致鉴权失败。
        Files.writeString(tokensFile, sb.toString(), StandardCharsets.UTF_8);

        // 校验：文件行数 = 用户数；token 已在 Redis 中正确映射到 userId
        List<String> lines = Files.readAllLines(tokensFile, StandardCharsets.UTF_8);
        assertEquals(users.size(), lines.size(), "tokens.txt 行数应等于压测用户数");
        assertTrue(lines.stream().allMatch(line -> !line.isBlank()), "token 不允许为空行");
        assertEquals(String.valueOf(firstUserId),
                stringRedisTemplate.opsForValue().get(Constants.TOKEN_KEY_PREFIX + firstToken),
                "token 应在 Redis 中映射到正确的 userId");

        log.info("已生成 {} 个压测 token 到 {}", users.size(), tokensFile.toAbsolutePath());
    }

    /**
     * 清理上一次运行生成的旧 token。
     */
    private void cleanupPreviousTokens() {
        Set<String> previous = stringRedisTemplate.opsForSet().members(LoadTestConfig.LOADTEST_TOKENS_KEY);
        if (previous == null || previous.isEmpty()) {
            return;
        }
        for (String token : previous) {
            stringRedisTemplate.delete(Constants.TOKEN_KEY_PREFIX + token);
        }
        stringRedisTemplate.delete(LoadTestConfig.LOADTEST_TOKENS_KEY);
    }

    /**
     * 定位 tokens.txt 输出路径：向上查找包含 post.lua 的 docs 目录，找不到则回退到当前目录。
     */
    private Path resolveTokensFile() {
        Path dir = Paths.get(System.getProperty("user.dir"));
        for (Path cur = dir; cur != null; cur = cur.getParent()) {
            Path docs = cur.resolve("docs");
            if (Files.isDirectory(docs) && Files.exists(docs.resolve("post.lua"))) {
                return docs.resolve("tokens.txt");
            }
        }
        return dir.resolve("tokens.txt");
    }
}
