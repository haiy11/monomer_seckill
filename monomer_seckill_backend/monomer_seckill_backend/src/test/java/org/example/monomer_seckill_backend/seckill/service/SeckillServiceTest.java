package org.example.monomer_seckill_backend.seckill.service;

import org.example.monomer_seckill_backend.common.Constants;
import org.example.monomer_seckill_backend.common.Result;
import org.example.monomer_seckill_backend.goods.entity.SeckillGoods;
import org.example.monomer_seckill_backend.goods.service.SeckillGoodsService;
import org.example.monomer_seckill_backend.goods.service.StockService;
import org.example.monomer_seckill_backend.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 秒杀服务单元测试。
 */
class SeckillServiceTest {

    @Test
    void seckill_duplicateInRedis_returnsFailWithoutCreatingOrder() {
        SeckillGoodsService seckillGoodsService = Mockito.mock(SeckillGoodsService.class);
        StockService stockService = Mockito.mock(StockService.class);
        OrderService orderService = Mockito.mock(OrderService.class);

        SeckillGoods sg = new SeckillGoods();
        sg.setName("测试秒杀商品");
        sg.setSeckillPrice(new BigDecimal("100.00"));
        Mockito.when(seckillGoodsService.requirePurchasable(1L)).thenReturn(sg);
        Mockito.when(stockService.deduct(1L, 100L)).thenReturn(Constants.LUA_DUPLICATE);

        SeckillService service = new SeckillService(seckillGoodsService, stockService, orderService);
        Result<String> result = service.seckill(1L, 100L);

        assertEquals(500, result.getCode().intValue());
        assertTrue(result.getMsg().contains("抢购"));
        verify(orderService, never()).createSeckillOrder(anyLong(), anyLong(), any());
    }

    @Test
    void seckill_stockEmpty_returnsFail() {
        SeckillGoodsService seckillGoodsService = Mockito.mock(SeckillGoodsService.class);
        StockService stockService = Mockito.mock(StockService.class);
        OrderService orderService = Mockito.mock(OrderService.class);

        SeckillGoods sg = new SeckillGoods();
        sg.setName("测试秒杀商品");
        sg.setSeckillPrice(new BigDecimal("100.00"));
        Mockito.when(seckillGoodsService.requirePurchasable(2L)).thenReturn(sg);
        Mockito.when(stockService.deduct(2L, 200L)).thenReturn(Constants.LUA_STOCK_EMPTY);

        SeckillService service = new SeckillService(seckillGoodsService, stockService, orderService);
        Result<String> result = service.seckill(2L, 200L);

        assertEquals(500, result.getCode().intValue());
        assertTrue(result.getMsg().contains("库存不足"));
        verify(orderService, never()).createSeckillOrder(anyLong(), anyLong(), any());
    }
}
