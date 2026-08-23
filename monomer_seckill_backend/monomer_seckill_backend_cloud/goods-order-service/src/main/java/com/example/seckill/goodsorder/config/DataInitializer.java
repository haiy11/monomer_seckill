package com.example.seckill.goodsorder.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.seckill.common.core.Constants;
import com.example.seckill.common.entity.Goods;
import com.example.seckill.common.entity.SeckillGoods;
import com.example.seckill.common.entity.User;
import com.example.seckill.common.mapper.GoodsMapper;
import com.example.seckill.common.mapper.SeckillGoodsMapper;
import com.example.seckill.common.mapper.UserMapper;
import com.example.seckill.common.service.StockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * 应用启动初始化器：创建默认账号、示例商品与秒杀商品，并预载秒杀库存到 Redis。
 *
 * <p>共享数据库下，种子数据统一由本服务（数据属主）幂等初始化，
 * 其余服务不再重复执行建表/种子，避免多服务启动竞态。</p>
 *
 * @author haiy
 * @date 2026/08/17
 */
@Slf4j
@Component
public class DataInitializer implements ApplicationRunner {

    private final UserMapper userMapper;
    private final GoodsMapper goodsMapper;
    private final SeckillGoodsMapper seckillGoodsMapper;
    private final StockService stockService;

    public DataInitializer(UserMapper userMapper, GoodsMapper goodsMapper,
                           SeckillGoodsMapper seckillGoodsMapper, StockService stockService) {
        this.userMapper = userMapper;
        this.goodsMapper = goodsMapper;
        this.seckillGoodsMapper = seckillGoodsMapper;
        this.stockService = stockService;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedUsers();
        User merchant = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, "merchant1"));
        if (merchant != null) {
            seedGoods(merchant.getId());
            seedSeckillGoods(merchant.getId());
        }
        stockService.preloadAll();
        log.info("秒杀库存已预载到 Redis");
    }

    private void seedUsers() {
        Long count = userMapper.selectCount(null);
        if (count != null && count > 0) {
            return;
        }
        insertUser("admin", "admin123", "管理员", Constants.ROLE_ADMIN);
        insertUser("user1", "123456", "演示用户", Constants.ROLE_USER);
        insertUser("merchant1", "123456", "演示商家", Constants.ROLE_MERCHANT);
        log.info("已初始化默认账号：admin/admin123（管理员）、user1/123456（用户）、merchant1/123456（商家）");
    }

    private void insertUser(String username, String password, String nickname, int role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(md5(password));
        user.setNickname(nickname);
        user.setRole(role);
        userMapper.insert(user);
    }

    private void seedGoods(Long merchantId) {
        Long count = goodsMapper.selectCount(null);
        if (count != null && count > 0) {
            return;
        }
        insertGoods(merchantId, "iPhone 15 Pro", "Apple iPhone 15 Pro 256G", "8999.00", 100);
        insertGoods(merchantId, "小米14", "小米14 12G+256G 黑色", "3999.00", 50);
        insertGoods(merchantId, "AirPods Pro 2", "Apple AirPods Pro 第二代", "1899.00", 30);
        log.info("已初始化 3 件示例商品（已上架）");
    }

    private void insertGoods(Long merchantId, String name, String desc, String price, int stock) {
        Goods goods = new Goods();
        goods.setMerchantId(merchantId);
        goods.setName(name);
        goods.setDescription(desc);
        goods.setPrice(new BigDecimal(price));
        goods.setStock(stock);
        goods.setStatus(Constants.GOODS_STATUS_ON);
        goodsMapper.insert(goods);
    }

    private void seedSeckillGoods(Long merchantId) {
        Long count = seckillGoodsMapper.selectCount(null);
        if (count != null && count > 0) {
            return;
        }
        SeckillGoods sg = new SeckillGoods();
        sg.setMerchantId(merchantId);
        sg.setName("iPhone 15 Pro 秒杀专场");
        sg.setSeckillPrice(new BigDecimal("7999.00"));
        sg.setSeckillStock(50);
        sg.setStartTime(LocalDateTime.now().minusHours(1));
        sg.setEndTime(LocalDateTime.now().plusHours(24));
        sg.setStatus(Constants.SECKILL_STATUS_ON);
        seckillGoodsMapper.insert(sg);
        log.info("已初始化 1 个示例秒杀商品（进行中）");
    }

    private String md5(String raw) {
        return DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
    }
}
