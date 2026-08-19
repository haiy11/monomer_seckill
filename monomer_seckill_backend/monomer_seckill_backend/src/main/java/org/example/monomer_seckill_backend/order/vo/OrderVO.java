package org.example.monomer_seckill_backend.order.vo;

import lombok.Data;
import org.example.monomer_seckill_backend.order.entity.OrderItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单详情视图对象（订单主表 + 明细）。
 *
 * @author haiy
 * @date 2026/08/17
 */
@Data
public class OrderVO {

    private Long id;

    private String orderNo;

    private Long userId;

    private BigDecimal totalAmount;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime payTime;

    /** 订单明细 */
    private List<OrderItem> items;
}
