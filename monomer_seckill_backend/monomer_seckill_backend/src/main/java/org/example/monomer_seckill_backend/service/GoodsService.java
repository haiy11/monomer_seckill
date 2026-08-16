package org.example.monomer_seckill_backend.service;

import org.example.monomer_seckill_backend.entity.Goods;
import org.example.monomer_seckill_backend.mapper.GoodsMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 商品查询服务
 */
@Service
public class GoodsService {

    private final GoodsMapper goodsMapper;

    public GoodsService(GoodsMapper goodsMapper) {
        this.goodsMapper = goodsMapper;
    }

    public List<Goods> list() {
        return goodsMapper.selectList(null);
    }

    public Goods getById(Long id) {
        return goodsMapper.selectById(id);
    }
}
