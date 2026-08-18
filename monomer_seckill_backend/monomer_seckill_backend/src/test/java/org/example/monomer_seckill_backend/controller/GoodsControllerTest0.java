package org.example.monomer_seckill_backend.controller;

import org.example.monomer_seckill_backend.common.Result;
import org.example.monomer_seckill_backend.entity.Goods;
import org.example.monomer_seckill_backend.service.GoodsService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

/**
 * 反射类设置需要的类的单元测试，能有字段注入（@Autowired 或 @Resource）
 */
class GoodsControllerTest0 {

    @Test
    void list() {
        // 1. 创建 Mock 对象
        GoodsService mockService = Mockito.mock(GoodsService.class);
        Mockito.when(mockService.list()).thenReturn(List.of(new Goods()));

        // 2. 直接传入 Mock 对象创建 Controller
        GoodsController controller = new GoodsController(mockService);

        // 3. 测试方法
        Result<List<Goods>> result = controller.list();

        // 4. 验证结果
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.getData().size());
    }

    @Test
    void detail() {
    }
}