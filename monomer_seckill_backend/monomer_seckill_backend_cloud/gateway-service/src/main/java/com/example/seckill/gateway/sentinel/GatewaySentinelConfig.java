package com.example.seckill.gateway.sentinel;

import com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiDefinition;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPathPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.GatewayApiDefinitionManager;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Sentinel 网关流控配置（P5），P7 起阈值改为 Nacos Config 动态刷新。
 *
 * <p>基于 Spring Cloud Gateway 的 Sentinel 集成（{@code spring-cloud-alibaba-sentinel-gateway}）：
 * SCA 自动装配已注册 {@code SentinelGatewayFilter} 与 {@code SentinelGatewayBlockExceptionHandler}，
 * 本类负责：① 自定义「被限流」时的响应体；② 程序化加载网关流控规则（QPS）。</p>
 *
 * <p>规则在容器刷新完成后（{@link ApplicationRunner}）加载，晚于 SCA 自动装配的默认回调注册；
 * 阈值由 {@link GatewaySentinelProperties}（{@code @RefreshScope}）提供，之后监听
 * {@link RefreshScopeRefreshedEvent}——Nacos 中 {@code sentinel.gateway.seckill-qps} 变更时
 * 自动重载规则，无需重启。</p>
 *
 * @author haiy
 * @date 2026/08/25
 */
@Slf4j
@Configuration
public class GatewaySentinelConfig implements ApplicationRunner {

    /** 秒杀下单 API 分组名（自定义，供 GatewayFlowRule 引用） */
    private static final String SECKILL_API = "seckill_api";

    private final GatewaySentinelProperties properties;

    public GatewaySentinelConfig(GatewaySentinelProperties properties) {
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        initBlockHandler();
        loadGatewayRules();
    }

    /**
     * 配置刷新后重载网关流控规则（@RefreshScope Bean 已重新绑定新值）。
     */
    @EventListener(RefreshScopeRefreshedEvent.class)
    public void onConfigRefresh(RefreshScopeRefreshedEvent event) {
        log.info("[Sentinel 网关] 检测到 Nacos 配置刷新，重载网关流控规则");
        loadGatewayRules();
    }

    /**
     * 自定义「被限流」时的响应：HTTP 429 + 统一 JSON（与下游 Result 结构一致：code/msg/data）。
     */
    private void initBlockHandler() {
        GatewayCallbackManager.setBlockHandler((exchange, t) -> {
            Map<String, Object> body = new HashMap<>();
            body.put("code", 429);
            body.put("msg", "请求过于频繁，已被限流，请稍后再试");
            body.put("data", null);
            return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body);
        });
        log.info("[Sentinel 网关] 已注册自定义限流响应（HTTP 429 + 统一 JSON）");
    }

    /**
     * 加载网关流控规则：把 /api/seckill/** 归为一个「秒杀下单」API，限制 QPS（阈值来自配置）。
     *
     * <p>网关流控用「自定义 API 分组 + 路径前缀匹配」而非路由 ID，这样只限流秒杀下单
     * （/api/seckill/**），不会误伤同属 seckill-service 的秒杀商品浏览（/api/seckill-goods/**）。</p>
     */
    private void loadGatewayRules() {
        // 1. 定义 API 分组：按路径前缀匹配 /api/seckill/**
        Set<ApiPredicateItem> items = new HashSet<>();
        items.add(new ApiPathPredicateItem()
                .setPattern("/api/seckill/**")
                .setMatchStrategy(SentinelGatewayConstants.URL_MATCH_STRATEGY_PREFIX));
        ApiDefinition seckillApi = new ApiDefinition(SECKILL_API).setPredicateItems(items);

        Set<ApiDefinition> apiDefinitions = new HashSet<>();
        apiDefinitions.add(seckillApi);
        GatewayApiDefinitionManager.loadApiDefinitions(apiDefinitions);

        // 2. 流控规则：秒杀下单 API 每秒最多 N 个请求（QPS，来自 Nacos 配置）
        double qps = properties.getSeckillQps();
        Set<GatewayFlowRule> rules = new HashSet<>();
        rules.add(new GatewayFlowRule(SECKILL_API)
                .setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_CUSTOM_API_NAME)
                .setGrade(RuleConstant.FLOW_GRADE_QPS)
                .setCount(qps)
                .setIntervalSec(1L)
                .setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_DEFAULT));
        GatewayRuleManager.loadRules(rules);

        log.info("[Sentinel 网关] 已加载网关流控规则：{} QPS={}", SECKILL_API, qps);
    }
}
