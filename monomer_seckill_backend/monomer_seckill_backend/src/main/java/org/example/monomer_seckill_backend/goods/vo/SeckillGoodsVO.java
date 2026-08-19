package org.example.monomer_seckill_backend.goods.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀商品视图对象（含原商品信息与秒杀状态）。
 *
 * @author haiy
 * @date 2026/08/17
 */
@Data
public class SeckillGoodsVO {

    private Long id;

    private Long merchantId;

    /** 秒杀商品名称 */
    private String name;

    /** 秒杀价 */
    private BigDecimal seckillPrice;

    /** 秒杀库存 */
    private Integer seckillStock;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer status;

    /** 秒杀状态（相对当前时间）：0-未开始 1-进行中 2-已结束 */
    private Integer seckillState;
}
