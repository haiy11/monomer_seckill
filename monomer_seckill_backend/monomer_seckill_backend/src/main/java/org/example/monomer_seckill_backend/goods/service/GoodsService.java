package org.example.monomer_seckill_backend.goods.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.monomer_seckill_backend.common.BizException;
import org.example.monomer_seckill_backend.common.Constants;
import org.example.monomer_seckill_backend.goods.entity.Goods;
import org.example.monomer_seckill_backend.goods.mapper.GoodsMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 商品服务（公开查询）。
 *
 * @author haiy
 * @date 2026/08/17
 */
@Service
public class GoodsService {

    private final GoodsMapper goodsMapper;

    public GoodsService(GoodsMapper goodsMapper) {
        this.goodsMapper = goodsMapper;
    }

    /**
     * 查询已上架商品列表（用户端正常商品页）。
     */
    public List<Goods> listOnSale() {
        return goodsMapper.selectList(new LambdaQueryWrapper<Goods>()
                .eq(Goods::getStatus, Constants.GOODS_STATUS_ON)
                .orderByAsc(Goods::getId));
    }

    /**
     * 按 ID 查询商品。
     */
    public Goods getById(Long id) {
        return id == null ? null : goodsMapper.selectById(id);
    }

    /**
     * 查询商品，不存在时抛异常。
     */
    public Goods requireGoods(Long id) {
        Goods goods = getById(id);
        if (goods == null) {
            throw new BizException("商品不存在");
        }
        return goods;
    }
}
