package com.example.seckill.seckill.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商家申请秒杀商品请求（内部接口入参）。
 *
 * @author haiy
 * @date 2026/08/17
 */
@Data
public class SeckillApplyRequest {

    /** 秒杀商品名称 */
    private String name;

    /** 秒杀价 */
    private BigDecimal seckillPrice;

    /** 秒杀库存 */
    private Integer seckillStock;

    /** 秒杀开始时间 */
    private LocalDateTime startTime;

    /** 秒杀结束时间 */
    private LocalDateTime endTime;
}
