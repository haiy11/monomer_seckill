package org.example.monomer_seckill_backend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("org.example.monomer_seckill_backend.mapper")
public class MonomerSeckillBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(MonomerSeckillBackendApplication.class, args);
	}

}
