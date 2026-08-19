package org.example.monomer_seckill_backend.cart.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 购物车条目视图对象（含商品信息）。
 *
 * @author haiy
 * @date 2026/08/17
 */
@Data
public class CartItemVO {

    private Long id;

    private Long goodsId;

    /** 商品名称 */
    private String goodsName;

    /** 商品单价 */
    private BigDecimal price;

    /** 商品库存 */
    private Integer stock;

    /** 购买数量 */
    private Integer quantity;

    /** 小计金额 */
    private BigDecimal amount;
}
