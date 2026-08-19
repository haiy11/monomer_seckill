package org.example.monomer_seckill_backend.merchant.service;

import org.example.monomer_seckill_backend.common.Constants;
import org.example.monomer_seckill_backend.goods.entity.Goods;
import org.example.monomer_seckill_backend.goods.mapper.GoodsMapper;
import org.example.monomer_seckill_backend.goods.mapper.SeckillGoodsMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

/**
 * 商家服务单元测试。
 */
class MerchantServiceTest {

    @Test
    void addGoods_defaultsToPendingAndOwner() {
        GoodsMapper goodsMapper = Mockito.mock(GoodsMapper.class);
        SeckillGoodsMapper seckillGoodsMapper = Mockito.mock(SeckillGoodsMapper.class);
        MerchantService service = new MerchantService(goodsMapper, seckillGoodsMapper);

        Goods goods = new Goods();
        goods.setName("测试商品");
        goods.setPrice(new BigDecimal("100.00"));
        goods.setStock(10);

        Goods created = service.addGoods(5L, goods);

        assertEquals(Constants.GOODS_STATUS_PENDING, created.getStatus().intValue());
        assertEquals(5L, created.getMerchantId().longValue());
        verify(goodsMapper).insert(goods);
    }
}
