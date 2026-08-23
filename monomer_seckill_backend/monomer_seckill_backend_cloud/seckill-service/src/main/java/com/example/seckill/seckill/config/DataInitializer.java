package com.example.seckill.seckill.config;

import com.example.seckill.seckill.service.StockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 秒杀服务启动初始化器：预载秒杀库存到 Redis。
 *
 * <p>秒杀商品种子数据由 goods-order-service 负责；本服务启动时把已存在的
 * 秒杀商品库存预载进 Redis。若表/数据尚未就绪（启动顺序），跳过本次预载，
 * 首次请求时由 StockService 懒加载兜底，不影响服务启动。</p>
 *
 * @author haiy
 * @date 2026/08/17
 */
@Slf4j
@Component
public class DataInitializer implements ApplicationRunner {

    private final StockService stockService;

    public DataInitializer(StockService stockService) {
        this.stockService = stockService;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            stockService.preloadAll();
            log.info("秒杀库存已预载到 Redis");
        } catch (Exception e) {
            log.warn("秒杀库存预载跳过（可能数据表尚未就绪），将在首次请求时懒加载: {}", e.getMessage());
        }
    }
}
