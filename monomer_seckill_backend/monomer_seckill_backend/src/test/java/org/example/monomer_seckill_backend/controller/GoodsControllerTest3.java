package org.example.monomer_seckill_backend.controller;

import org.example.monomer_seckill_backend.common.Result;
import org.example.monomer_seckill_backend.entity.Goods;
import org.example.monomer_seckill_backend.service.GoodsService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;

/**
 * 使用 Spring Test 框架（一般用于集成测试）
 */
@SpringBootTest  // 启动完整的 Spring 容器
class GoodsControllerTest3 {

    @MockBean  // Spring 会自动替换容器中的 Bean 为 Mock
    private GoodsService mockService;

    @Autowired  // Spring 自动注入 Controller
    private GoodsController controller;

    @Test
    void testList() {
        // 定义 Mock 行为
        Mockito.when(mockService.list()).thenReturn(List.of(new Goods()));

        // 调用方法
        Result<List<Goods>> result = controller.list();

        // 验证
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.getData().size());
    }
    @Test
    void detail() {
    }
}