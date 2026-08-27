package com.example.seckill.seckill.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowItem;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRuleManager;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import java.util.List;

/**
 * Sentinel 服务级流量防护配置（P5），P7 起规则阈值改为 Nacos Config 动态刷新。
 *
 * <p>针对 seckill-service 的秒杀下单资源做两类防护：</p>
 * <ul>
 *   <li><b>熔断降级</b>：慢调用比例熔断——统计窗口内慢调用（RT &gt; 阈值）占比 &gt; 阈值时打开熔断器；</li>
 *   <li><b>热点参数限流</b>：按商品 ID（seckillGoodsId）维度限流，热点商品单独收紧阈值。</li>
 * </ul>
 *
 * <p>资源名与 {@code @SentinelResource} 注解的 {@code value} 严格一致；阈值由
 * {@link SentinelProperties}（{@code @RefreshScope}）提供。容器启动时通过
 * {@link #initRules()} 加载一次，之后监听 {@link RefreshScopeRefreshedEvent}——
 * 当 Nacos 中 {@code sentinel.seckill.*} 配置变更时重载规则，无需重启。</p>
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

    private final SentinelProperties properties;

    public SentinelConfig(SentinelProperties properties) {
        this.properties = properties;
    }

    /**
     * 启动时加载一次规则。
     */
    @PostConstruct
    public void initRules() {
        loadDegradeRules();
        loadParamFlowRules();
    }

    /**
     * 配置刷新后重载规则（@RefreshScope Bean 已重新绑定新值）。
     *
     * <p>注意：不依赖事件携带的 keys，直接整体重载，规则幂等覆盖，逻辑最简单也最不易错。</p>
     */
    @EventListener(RefreshScopeRefreshedEvent.class)
    public void onConfigRefresh(RefreshScopeRefreshedEvent event) {
        log.info("[Sentinel 秒杀] 检测到 Nacos 配置刷新，重载熔断/热点参数限流规则");
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
                        .setCount(properties.getDegradeRtMs())
                        .setSlowRatioThreshold(properties.getSlowRatioThreshold())
                        .setMinRequestAmount(properties.getMinRequestAmount())
                        .setStatIntervalMs(properties.getStatIntervalMs())
                        .setTimeWindow(properties.getTimeWindowSeconds()),
                // 演示资源：便于通过人工慢调用观察熔断（与真实资源规则一致）
                new DegradeRule(SECKILL_DEMO_RESOURCE)
                        .setGrade(RuleConstant.DEGRADE_GRADE_RT)
                        .setCount(properties.getDegradeRtMs())
                        .setSlowRatioThreshold(properties.getSlowRatioThreshold())
                        .setMinRequestAmount(properties.getMinRequestAmount())
                        .setStatIntervalMs(properties.getStatIntervalMs())
                        .setTimeWindow(properties.getTimeWindowSeconds())
        );
        DegradeRuleManager.loadRules(rules);
        log.info("[Sentinel 秒杀] 已加载熔断规则：rt>{}ms 且慢调用占比>{} 触发熔断，熔断时长 {}s",
                properties.getDegradeRtMs(), properties.getSlowRatioThreshold(), properties.getTimeWindowSeconds());
    }

    /**
     * 加载热点参数限流规则（商品 ID 维度）。
     *
     * <p>资源 {@code seckill} 的第 0 个参数是 {@code seckillGoodsId}。默认单商品秒杀 QPS 上限，
     * 热点商品单独收紧阈值。</p>
     */
    private void loadParamFlowRules() {
        ParamFlowRule rule = new ParamFlowRule(SECKILL_RESOURCE)
                .setParamIdx(0)
                .setGrade(RuleConstant.FLOW_GRADE_QPS)
                .setCount(properties.getParamFlowDefaultQps())
                .setDurationInSec(1L)
                .setParamFlowItemList(List.of(
                        ParamFlowItem.newItem(properties.getParamFlowHotGoodsId(), properties.getParamFlowHotQps())
                ));
        ParamFlowRuleManager.loadRules(List.of(rule));
        log.info("[Sentinel 秒杀] 已加载热点参数限流规则：默认 {} QPS，热点商品 {} 收紧到 {} QPS",
                properties.getParamFlowDefaultQps(), properties.getParamFlowHotGoodsId(), properties.getParamFlowHotQps());
    }
}
