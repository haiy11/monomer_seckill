package com.example.seckill.seckill.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.seckill.seckill.constant.SeckillConstants;
import com.example.seckill.seckill.entity.SeckillGoods;
import com.example.seckill.seckill.entity.User;
import com.example.seckill.seckill.mapper.SeckillGoodsMapper;
import com.example.seckill.seckill.mapper.UserMapper;
import com.example.seckill.seckill.service.StockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀服务启动初始化器：创建示例秒杀商品并预载秒杀库存到 Redis。
 *
 * <p>示例秒杀商品种子数据由本服务（秒杀域属主）幂等初始化；
 * 若商家账号/数据表尚未就绪（启动顺序），跳过本次初始化，首次请求时由 StockService 懒加载兜底。</p>
 *
 * @author haiy
 * @date 2026/08/17
 */
@Slf4j
@Component
public class DataInitializer implements ApplicationRunner {

    private final StockService stockService;
    private final SeckillGoodsMapper seckillGoodsMapper;
    private final UserMapper userMapper;

    public DataInitializer(StockService stockService, SeckillGoodsMapper seckillGoodsMapper, UserMapper userMapper) {
        this.stockService = stockService;
        this.seckillGoodsMapper = seckillGoodsMapper;
        this.userMapper = userMapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            seedSeckillGoods();
            stockService.preloadAll();
            log.info("秒杀库存已预载到 Redis");
        } catch (Exception e) {
            log.warn("秒杀初始化跳过（可能数据表尚未就绪），将在首次请求时懒加载: {}", e.getMessage());
        }
    }

    private void seedSeckillGoods() {
        Long count = seckillGoodsMapper.selectCount(null);
        if (count != null && count > 0) {
            return;
        }
        User merchant = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, "merchant1"));
        if (merchant == null) {
            return;
        }
        SeckillGoods sg = new SeckillGoods();
        sg.setMerchantId(merchant.getId());
        sg.setName("iPhone 15 Pro 秒杀专场");
        sg.setSeckillPrice(new BigDecimal("7999.00"));
        sg.setSeckillStock(50);
        sg.setStartTime(LocalDateTime.now().minusHours(1));
        sg.setEndTime(LocalDateTime.now().plusHours(24));
        sg.setStatus(SeckillConstants.SECKILL_STATUS_ON);
        seckillGoodsMapper.insert(sg);
        log.info("已初始化 1 个示例秒杀商品（进行中）");
    }
}
