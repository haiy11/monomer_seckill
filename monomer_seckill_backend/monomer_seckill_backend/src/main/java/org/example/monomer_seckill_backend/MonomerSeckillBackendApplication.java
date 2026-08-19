package org.example.monomer_seckill_backend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 秒杀商城后端启动类（单体，P2 扩展：普通用户 / 商家 / 管理员 三角色）。
 *
 * @author haiy
 * @date 2026/08/17
 */
@SpringBootApplication
@EnableScheduling
@MapperScan({
        "org.example.monomer_seckill_backend.user.mapper",
        "org.example.monomer_seckill_backend.goods.mapper",
        "org.example.monomer_seckill_backend.order.mapper",
        "org.example.monomer_seckill_backend.cart.mapper"
})
public class MonomerSeckillBackendApplication {

    /**
     * 应用启动主方法。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(MonomerSeckillBackendApplication.class, args);
    }

}
