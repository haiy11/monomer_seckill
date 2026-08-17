package org.example.monomer_seckill_backend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 秒杀项目后端启动类。
 *
 * <p>Spring Boot 启动入口，通过 @MapperScan 扫描 mybatis-plus 的 Mapper 接口，
 * 使其自动注册为 Spring Bean。</p>
 *
 * @author haiy
 * @date 2026/08/17
 */
@SpringBootApplication
@MapperScan("org.example.monomer_seckill_backend.mapper")
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
