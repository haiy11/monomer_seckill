package org.example.monomer_seckill_backend.service;

import org.example.monomer_seckill_backend.entity.Goods;
import org.example.monomer_seckill_backend.mapper.GoodsMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 商品查询服务。
 *
 * @author haiy
 * @date 2026/08/17
 */
@Service
public class GoodsService {

    /** 商品 Mapper */
    private final GoodsMapper goodsMapper;

    public GoodsService(GoodsMapper goodsMapper) {
        this.goodsMapper = goodsMapper;
    }

    /**
     * 查询全部秒杀商品。
     *
     * @return 商品列表
     */
    public List<Goods> list() {
        return goodsMapper.selectList(null);
    }

    /**
     * 按 ID 查询秒杀商品。
     *
     * @param id 商品ID
     * @return 商品，不存在时返回 null
     */
    public Goods getById(Long id) {
        return goodsMapper.selectById(id);
    }
}
