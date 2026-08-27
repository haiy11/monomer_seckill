package com.example.seckill.gateway.sentinel;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * 网关 Sentinel 流控规则的可刷新配置（P7）。
 *
 * <p>阈值原本硬编码在 {@link GatewaySentinelConfig} 里，P7 迁移到 Nacos Config 后改为
 * {@code @ConfigurationProperties} + {@code @RefreshScope}：修改 Nacos 中的
 * {@code sentinel.gateway.seckill-qps} 后无需重启，{@link GatewaySentinelConfig}
 * 监听 {@code RefreshScopeRefreshedEvent} 自动重载流控规则。</p>
 *
 * @author haiy
 * @date 2026/08/26
 */
@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "sentinel.gateway")
public class GatewaySentinelProperties {

    /** 网关流控阈值：秒杀下单接口（/api/seckill/**）每秒最大 QPS */
    private double seckillQps = 100;
}
