package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.ShoppingCart;
import com.itheima.reggie.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/shoppingCart")
@Slf4j
public class ShoppingCartController {

    @Autowired
    private ShoppingCartService shoppingCartService;


    @PostMapping("/add")
    public R<ShoppingCart> add(@RequestBody ShoppingCart shoppingCart) {
        log.info("購物車數據 {}", shoppingCart);
        Long currentId = BaseContext.getCurrentId();
        System.out.println("=========================" + currentId +"===============================");
        shoppingCart.setUserId(currentId);
        Long dishId = shoppingCart.getDishId();
        Long setmealId = shoppingCart.getSetmealId();
        LambdaQueryWrapper<ShoppingCart> lqw = new LambdaQueryWrapper<>();
        lqw.eq(ShoppingCart::getUserId, currentId);
        if (dishId != null) {
            lqw.eq(ShoppingCart::getDishId, dishId);

        } else {
            lqw.eq(ShoppingCart::getSetmealId, setmealId);
        }

        ShoppingCart one = shoppingCartService.getOne(lqw);

        if (one != null) {
            Integer num = one.getNumber();
            one.setNumber(num + 1);
            shoppingCartService.updateById(one);
        } else {
            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCartService.save(shoppingCart);
            one = shoppingCart;
        }
        return R.success(one);
    }

    @GetMapping("/list")
    public R<List<ShoppingCart>> list() {
        final Long currentId = BaseContext.getCurrentId();
        LambdaQueryWrapper<ShoppingCart> lqw = new LambdaQueryWrapper<>();
        lqw.eq(ShoppingCart::getUserId, currentId);
        lqw.orderByDesc(ShoppingCart::getCreateTime);
        final List<ShoppingCart> list = shoppingCartService.list(lqw);
        return R.success(list);
    }

    @DeleteMapping("/clean")
    public R<String> clean() {
        final Long currentId = BaseContext.getCurrentId();
        LambdaQueryWrapper<ShoppingCart> lqw = new LambdaQueryWrapper<>();
        lqw.eq(ShoppingCart::getUserId, currentId);
        shoppingCartService.remove(lqw);
        return R.success("清空購物車成功！");
    }

    @PostMapping("/sub")
    public R<String> sub(@RequestBody ShoppingCart shoppingCart) {
        log.info("要刪減的物品！{}", shoppingCart);
        final Long currentId = BaseContext.getCurrentId();
        final Long dishId = shoppingCart.getDishId();
        LambdaQueryWrapper<ShoppingCart> lqw = new LambdaQueryWrapper<>();
        lqw.eq(ShoppingCart::getUserId, currentId);
        lqw.eq(ShoppingCart::getDishId, dishId);
        final ShoppingCart one = shoppingCartService.getOne(lqw);
        final Integer number = one.getNumber();
        if (number == 1) {
            shoppingCartService.removeById(one);
        } else {
            one.setNumber(number - 1);
            shoppingCartService.updateById(one);
        }
        return R.success("刪除成功！");
    }
}
