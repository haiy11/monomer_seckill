package org.example.monomer_seckill_backend.controller;

import org.example.monomer_seckill_backend.common.Result;
import org.example.monomer_seckill_backend.service.SeckillService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 秒杀接口。
 *
 * @author haiy
 * @date 2026/08/17
 */
@RestController
@RequestMapping("/api/seckill")
public class SeckillController {

    private final SeckillService seckillService;

    public SeckillController(SeckillService seckillService) {
        this.seckillService = seckillService;
    }

    /**
     * 秒杀下单接口。
     *
     * @param goodsId 商品ID（路径参数）
     * @param userId  用户ID（Query 参数）
     * @return 成功时携带订单ID；失败时携带错误信息
     */
    @PostMapping("/{goodsId}")
    public Result<Long> seckill(@PathVariable Long goodsId, @RequestParam Long userId) {
        return seckillService.seckill(goodsId, userId);
    }
}
