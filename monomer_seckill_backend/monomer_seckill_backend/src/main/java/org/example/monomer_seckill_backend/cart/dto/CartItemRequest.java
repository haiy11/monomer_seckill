package org.example.monomer_seckill_backend.cart.dto;

import lombok.Data;

/**
 * 购物车添加/修改请求。
 *
 * @author haiy
 * @date 2026/08/17
 */
@Data
public class CartItemRequest {

    /** 商品ID */
    private Long goodsId;

    /** 购买数量 */
    private Integer quantity;
}
