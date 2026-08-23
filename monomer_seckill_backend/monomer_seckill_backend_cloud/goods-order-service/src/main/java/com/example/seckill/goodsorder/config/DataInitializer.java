package com.example.seckill.goodsorder.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.seckill.goodsorder.constant.GoodsOrderConstants;
import com.example.seckill.goodsorder.entity.Goods;
import com.example.seckill.goodsorder.entity.User;
import com.example.seckill.goodsorder.mapper.GoodsMapper;
import com.example.seckill.goodsorder.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

/**
 * 应用启动初始化器：创建默认账号与示例正常商品。
 *
 * <p>共享数据库下，建表与「用户 + 正常商品」种子数据由本服务（数据属主）幂等初始化；
 * 秒杀商品种子数据由 seckill-service 负责。</p>
 *
 * @author haiy
 * @date 2026/08/17
 */
@Slf4j
@Component
public class DataInitializer implements ApplicationRunner {

    private final UserMapper userMapper;
    private final GoodsMapper goodsMapper;

    public DataInitializer(UserMapper userMapper, GoodsMapper goodsMapper) {
        this.userMapper = userMapper;
        this.goodsMapper = goodsMapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedUsers();
        User merchant = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, "merchant1"));
        if (merchant != null) {
            seedGoods(merchant.getId());
        }
    }

    private void seedUsers() {
        Long count = userMapper.selectCount(null);
        if (count != null && count > 0) {
            return;
        }
        insertUser("admin", "admin123", "管理员", GoodsOrderConstants.ROLE_ADMIN);
        insertUser("user1", "123456", "演示用户", GoodsOrderConstants.ROLE_USER);
        insertUser("merchant1", "123456", "演示商家", GoodsOrderConstants.ROLE_MERCHANT);
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
        goods.setStatus(GoodsOrderConstants.GOODS_STATUS_ON);
        goodsMapper.insert(goods);
    }

    private String md5(String raw) {
        return DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
    }
}
