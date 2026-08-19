package org.example.monomer_seckill_backend.cart.controller;

import org.example.monomer_seckill_backend.cart.dto.CartItemRequest;
import org.example.monomer_seckill_backend.cart.service.CartService;
import org.example.monomer_seckill_backend.cart.vo.CartItemVO;
import org.example.monomer_seckill_backend.common.Result;
import org.example.monomer_seckill_backend.common.UserContext;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 购物车接口（需登录）。
 *
 * @author haiy
 * @date 2026/08/17
 */
@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public Result<List<CartItemVO>> list() {
        return Result.ok(cartService.list(UserContext.getUserId()));
    }

    @PostMapping
    public Result<Void> add(@RequestBody CartItemRequest request) {
        cartService.add(UserContext.getUserId(), request);
        return Result.ok();
    }

    @PutMapping("/{goodsId}")
    public Result<Void> update(@PathVariable Long goodsId, @RequestParam Integer quantity) {
        cartService.updateQuantity(UserContext.getUserId(), goodsId, quantity);
        return Result.ok();
    }

    @DeleteMapping("/{goodsId}")
    public Result<Void> remove(@PathVariable Long goodsId) {
        cartService.remove(UserContext.getUserId(), goodsId);
        return Result.ok();
    }
}
