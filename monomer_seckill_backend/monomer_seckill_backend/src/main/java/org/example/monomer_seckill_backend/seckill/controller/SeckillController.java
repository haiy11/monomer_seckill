package org.example.monomer_seckill_backend.seckill.controller;

import org.example.monomer_seckill_backend.common.Result;
import org.example.monomer_seckill_backend.common.UserContext;
import org.example.monomer_seckill_backend.seckill.service.SeckillService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 秒杀下单接口（需登录，用户身份从 token 解析，不允许自行传 userId）。
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
     * 秒杀下单。
     *
     * @param seckillGoodsId 秒杀商品ID
     * @return 成功携带订单号
     */
    @PostMapping("/{seckillGoodsId}")
    public Result<String> seckill(@PathVariable Long seckillGoodsId) {
        return seckillService.seckill(seckillGoodsId, UserContext.getUserId());
    }
}
