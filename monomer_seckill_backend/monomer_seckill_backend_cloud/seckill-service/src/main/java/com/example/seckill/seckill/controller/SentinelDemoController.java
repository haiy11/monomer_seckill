package com.example.seckill.seckill.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.example.seckill.common.core.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sentinel 熔断演示端点（P5 学习用，非业务接口）。
 *
 * <p>模拟一个「可能变慢的下游调用」（如慢查询 / 慢 Redis / 慢 Feign），配合
 * {@code SentinelConfig} 中 {@code seckill-demo} 资源的慢调用比例熔断规则，
 * 演示「慢调用比例 &gt; 20% → 熔断打开 → 快速失败 → 半开恢复」的完整过程。</p>
 *
 * <p>触发方式：持续以 delay &gt; 200ms 调用本端点（如 delay=300），慢调用占比超过 20% 后，
 * 熔断器打开，后续请求直接走 {@link #demoBlockHandler} 快速失败，不再真正 sleep。</p>
 *
 * @author haiy
 * @date 2026/08/25
 */
@RestController
@RequestMapping("/api/seckill/demo")
public class SentinelDemoController {

    /**
     * 模拟慢调用：delay 毫秒大于 200 即被 Sentinel 计为「慢调用」。
     *
     * @param delay 模拟耗时（毫秒），默认 300
     */
    @SentinelResource(value = "seckill-demo", blockHandler = "demoBlockHandler")
    @GetMapping("/slow")
    public Result<String> slow(@RequestParam(defaultValue = "300") int delay) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return Result.ok("慢调用模拟完成，耗时 " + delay + "ms");
    }

    /**
     * {@code seckill-demo} 资源被熔断时的兜底处理（Sentinel 框架回调）。
     *
     * <p>签名约定：与原方法参数一致，末尾追加 {@link BlockException}。</p>
     */
    public Result<String> demoBlockHandler(int delay, BlockException ex) {
        return Result.fail(503, "演示资源已熔断，请稍后再试（熔断保护生效）");
    }
}
