package org.example.monomer_seckill_backend.goods.controller;

import org.example.monomer_seckill_backend.common.Result;
import org.example.monomer_seckill_backend.goods.service.SeckillGoodsService;
import org.example.monomer_seckill_backend.goods.vo.SeckillGoodsVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 秒杀商品接口（用户端查看）。
 *
 * @author haiy
 * @date 2026/08/17
 */
@RestController
@RequestMapping("/api/seckill-goods")
public class SeckillGoodsController {

    private final SeckillGoodsService seckillGoodsService;

    public SeckillGoodsController(SeckillGoodsService seckillGoodsService) {
        this.seckillGoodsService = seckillGoodsService;
    }

    @GetMapping
    public Result<List<SeckillGoodsVO>> list() {
        return Result.ok(seckillGoodsService.listOnSale());
    }

    @GetMapping("/{id}")
    public Result<SeckillGoodsVO> detail(@PathVariable Long id) {
        return Result.ok(seckillGoodsService.getVO(id));
    }
}
