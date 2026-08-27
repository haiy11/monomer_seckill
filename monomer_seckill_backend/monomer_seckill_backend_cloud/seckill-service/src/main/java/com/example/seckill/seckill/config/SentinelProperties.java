package com.example.seckill.seckill.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * 秒杀服务 Sentinel 防护规则的可刷新配置（P7）。
 *
 * <p>阈值原本硬编码在 {@link SentinelConfig} 里，P7 迁移到 Nacos Config 后改为
 * {@code @ConfigurationProperties} + {@code @RefreshScope}：修改 Nacos 中的
 * {@code sentinel.seckill.*} 配置后，无需重启即可被重新绑定，再由 {@link SentinelConfig}
 * 监听 {@code RefreshScopeRefreshedEvent} 重载规则生效。</p>
 *
 * <p>各字段均给出与 P5 计划一致的默认值，即使 Nacos 中未配置也能正常启动。</p>
 *
 * @author haiy
 * @date 2026/08/26
 */
@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "sentinel.seckill")
public class SentinelProperties {

    /** 慢调用判定阈值（毫秒）：RT 超过该值视为「慢调用」 */
    private double degradeRtMs = 200;

    /** 慢调用比例阈值（0~1）：统计窗口内慢调用占比超过该值触发熔断 */
    private double slowRatioThreshold = 0.2;

    /** 熔断时长（秒）：熔断打开后经过该时长进入半开状态 */
    private int timeWindowSeconds = 10;

    /** 熔断统计窗口内最小请求数：低于该值不触发熔断，避免小流量误熔断 */
    private int minRequestAmount = 5;

    /** 熔断统计窗口时长（毫秒） */
    private int statIntervalMs = 1000;

    /** 热点参数限流：单商品默认 QPS 上限（第 0 个参数 = 秒杀商品ID） */
    private int paramFlowDefaultQps = 10;

    /** 热点参数限流：热点商品单独收紧后的 QPS 上限 */
    private int paramFlowHotQps = 5;

    /** 热点参数限流：热点商品 ID */
    private long paramFlowHotGoodsId = 1;
}
