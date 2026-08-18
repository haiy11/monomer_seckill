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

import java.util.List;

/**
 * 通过 Mockito 的 @InjectMocks 注入
 *
 * 1. 扫描 GoodsController 的所有字段
 * 2. 找到类型为 GoodsService 的字段
 * 3. 使用反射将 @Mock 标注的 mockService 注入进去
 */
class GoodsControllerTest2 {

    @Mock  // 创建 Mock 对象
    private GoodsService mockService;

    @InjectMocks  // 自动注入 Mock 到 Controller
    private GoodsController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);  // 初始化 Mockito
    }

    @Test
    void testList() {
        // 定义 Mock 行为
        Mockito.when(mockService.list()).thenReturn(List.of(new Goods()));

        // 直接调用方法（Mock 已自动注入）
        Result<List<Goods>> result = controller.list();

        // 验证
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.getData().size());
    }

    @Test
    void detail() {
    }
}