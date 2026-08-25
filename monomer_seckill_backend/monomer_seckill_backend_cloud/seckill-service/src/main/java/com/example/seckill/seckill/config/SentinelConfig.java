package com.example.seckill.seckill.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowItem;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRuleManager;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Sentinel 服务级流量防护配置（P5）。
 *
 * <p>针对 seckill-service 的秒杀下单资源做两类防护：</p>
 * <ul>
 *   <li><b>熔断降级</b>：慢调用比例熔断——统计窗口内慢调用（RT &gt; 200ms）占比 &gt; 20% 时打开熔断器；</li>
 *   <li><b>热点参数限流</b>：按商品 ID（seckillGoodsId）维度限流，热点商品单独收紧阈值。</li>
 * </ul>
 *
 * <p>资源名与 {@code @SentinelResource} 注解的 {@code value} 严格一致，规则写死在代码里，
 * 不依赖 Sentinel Dashboard 也能生效。熔断规则同样适用于真实的秒杀下单资源 {@code seckill}；
 * 为便于观察熔断效果，额外提供 {@code seckill-demo} 演示资源（见 {@code SentinelDemoController}）。</p>
 *
 * @author haiy
 * @date 2026/08/25
 */
@Slf4j
@Configuration
public class SentinelConfig {

    /** 秒杀下单资源名（与 @SentinelResource 的 value 一致） */
    public static final String SECKILL_RESOURCE = "seckill";

    /** 熔断演示资源名（与 @SentinelResource 的 value 一致） */
    public static final String SECKILL_DEMO_RESOURCE = "seckill-demo";

    /** 慢调用判定阈值：RT 超过该毫秒数视为「慢调用」 */
    private static final double MAX_ALLOWED_RT_MS = 200;

    /** 慢调用比例阈值：慢调用占比超过 20% 触发熔断 */
    private static final double SLOW_RATIO_THRESHOLD = 0.2;

    /** 熔断时长：熔断打开 10 秒后进入半开状态 */
    private static final int TIME_WINDOW_SECONDS = 10;

    @PostConstruct
    public void initRules() {
        loadDegradeRules();
        loadParamFlowRules();
    }

    /**
     * 加载熔断降级规则（慢调用比例）。
     *
     * <p>Sentinel 1.8.x 的 DEGRADE_GRADE_RT 语义是「慢调用比例」：{@code count} 表示 RT 阈值（ms），
     * {@code slowRatioThreshold} 表示慢调用比例阈值（0~1）。</p>
     */
    private void loadDegradeRules() {
        List<DegradeRule> rules = List.of(
                // 秒杀下单：真实资源的熔断规则（P5 计划「stock-service 慢调用比例 > 20% 触发」）
                new DegradeRule(SECKILL_RESOURCE)
                        .setGrade(RuleConstant.DEGRADE_GRADE_RT)
                        .setCount(MAX_ALLOWED_RT_MS)
                        .setSlowRatioThreshold(SLOW_RATIO_THRESHOLD)
                        .setMinRequestAmount(5)
                        .setStatIntervalMs(1000)
                        .setTimeWindow(TIME_WINDOW_SECONDS),
                // 演示资源：便于通过人工慢调用观察熔断（与真实资源规则一致）
                new DegradeRule(SECKILL_DEMO_RESOURCE)
                        .setGrade(RuleConstant.DEGRADE_GRADE_RT)
                        .setCount(MAX_ALLOWED_RT_MS)
                        .setSlowRatioThreshold(SLOW_RATIO_THRESHOLD)
                        .setMinRequestAmount(5)
                        .setStatIntervalMs(1000)
                        .setTimeWindow(TIME_WINDOW_SECONDS)
        );
        DegradeRuleManager.loadRules(rules);
        log.info("[Sentinel 秒杀] 已加载熔断规则：{}", rules);
    }

    /**
     * 加载热点参数限流规则（商品 ID 维度）。
     *
     * <p>资源 {@code seckill} 的第 0 个参数是 {@code seckillGoodsId}。默认单商品秒杀 QPS 上限 10，
     * 其中热点商品 id=1 单独收紧到 5 QPS。</p>
     */
    private void loadParamFlowRules() {
        ParamFlowRule rule = new ParamFlowRule(SECKILL_RESOURCE)
                .setParamIdx(0)
                .setGrade(RuleConstant.FLOW_GRADE_QPS)
                .setCount(10)
                .setDurationInSec(1L)
                .setParamFlowItemList(List.of(
                        ParamFlowItem.newItem(1L, 5)
                ));
        ParamFlowRuleManager.loadRules(List.of(rule));
        log.info("[Sentinel 秒杀] 已加载热点参数限流规则：{}", rule);
    }
}
