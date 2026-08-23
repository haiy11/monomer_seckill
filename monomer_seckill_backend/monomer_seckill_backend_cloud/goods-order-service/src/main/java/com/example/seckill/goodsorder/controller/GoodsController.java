package com.example.seckill.goodsorder.controller;

import com.example.seckill.common.core.Result;
import com.example.seckill.goodsorder.entity.Goods;
import com.example.seckill.goodsorder.service.GoodsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 正常商品接口（用户端）。
 *
 * @author haiy
 * @date 2026/08/17
 */
@RestController
@RequestMapping("/api/goods")
public class GoodsController {

    private final GoodsService goodsService;

    public GoodsController(GoodsService goodsService) {
        this.goodsService = goodsService;
    }

    @GetMapping
    public Result<List<Goods>> list() {
        return Result.ok(goodsService.listOnSale());
    }

    @GetMapping("/{id}")
    public Result<Goods> detail(@PathVariable Long id) {
        return Result.ok(goodsService.getById(id));
    }
}
