package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.OrdersDto;
import com.itheima.reggie.entity.OrderDetail;
import com.itheima.reggie.entity.Orders;
import com.itheima.reggie.entity.ShoppingCart;
import com.itheima.reggie.entity.User;
import com.itheima.reggie.service.OrderDetailService;
import com.itheima.reggie.service.OrderService;
import com.itheima.reggie.service.ShoppingCartService;
import com.itheima.reggie.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/order")
@Slf4j
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderDetailService orderDetailService;

    @Autowired
    private ShoppingCartService shoppingCartService;

    @Autowired
    private UserService userService;

    @PostMapping("/submit")
    public R<String> submit(@RequestBody Orders orders) {
        /*final Long userId = orders.getUserId();
        final User user = userService.getById(userId);
        orders.setUserName(user.getName());*/
        orderService.submit(orders);
        return R.success("下單已成功！");
    }

    @GetMapping("userPage")
    public R<Page<OrdersDto>> getPage(Integer page, Integer pageSize) {
        log.info("page {}, pageSize {}", page, pageSize);
        Page<Orders> pageInfo = new Page<>(page, pageSize);
        final Long currentId = BaseContext.getCurrentId();
        LambdaQueryWrapper<Orders> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Orders::getUserId, currentId);
        lqw.orderByDesc(Orders::getOrderTime);
        orderService.page(pageInfo, lqw);
        final List<Orders> records = pageInfo.getRecords();

        Page<OrdersDto> ordersDtoPage = new Page<>();
        BeanUtils.copyProperties(pageInfo, ordersDtoPage, "records");
        List<OrdersDto> dtoRecords = new ArrayList<>();
        for (Orders record : records) {
            OrdersDto ordersDto = new OrdersDto();
            BeanUtils.copyProperties(record, ordersDto);
            final Long id = record.getId();
            LambdaQueryWrapper<OrderDetail> lqw1 = new LambdaQueryWrapper<>();
            lqw1.eq(OrderDetail::getOrderId, id);
            final List<OrderDetail> list = orderDetailService.list(lqw1);
            ordersDto.setOrderDetails(list);
            dtoRecords.add(ordersDto);
        }
        ordersDtoPage.setRecords(dtoRecords);
        return R.success(ordersDtoPage);
    }

    @PostMapping("/again")
    public R<String> again(@RequestBody Orders order) {
        final Long id = order.getId();
        LambdaQueryWrapper<OrderDetail> lqw = new LambdaQueryWrapper<>();
        lqw.eq(OrderDetail::getOrderId, id);
        final List<OrderDetail> list = orderDetailService.list(lqw);
        log.info("之前下的單項 {}", list);
        final Long currentId = BaseContext.getCurrentId();
        for (OrderDetail orderDetail : list) {
            ShoppingCart shoppingCart  = new ShoppingCart();
            BeanUtils.copyProperties(orderDetail, shoppingCart, "id");
            shoppingCart.setUserId(currentId);
            final Long dishId = shoppingCart.getDishId();
            final Long setmealId = shoppingCart.getSetmealId();
            LambdaQueryWrapper<ShoppingCart>  lqw2 = new LambdaQueryWrapper<>();
            if (dishId != null) {
                lqw2.eq(ShoppingCart::getDishId, dishId);
            } else {
                lqw2.eq(ShoppingCart::getSetmealId, setmealId);
            }
            ShoppingCart one = shoppingCartService.getOne(lqw2);
            if (one != null) {
                one.setNumber(one.getNumber() + shoppingCart.getNumber());
                one.setCreateTime(LocalDateTime.now());
                shoppingCartService.updateById(one);
            } else {
                one = shoppingCart;
                one.setCreateTime(LocalDateTime.now());
                shoppingCartService.save(one);
            }
        }
        return R.success("下單成功！");
    }

    @GetMapping("/page")
    public R<Page<Orders>> backendPage(Integer page, Integer pageSize, Long number, @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date beginTime,@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date endTime) {
        Page<Orders> pageInfo = new Page<>(page, pageSize);
        LambdaQueryWrapper<Orders> lqw = new LambdaQueryWrapper<>();
        lqw.eq(number != null, Orders::getId, number);
        lqw.ge(beginTime != null, Orders::getOrderTime, beginTime);
        lqw.le(endTime != null, Orders::getOrderTime, endTime);
        lqw.orderByAsc(Orders::getStatus);
        lqw.orderByDesc(Orders::getOrderTime);
        orderService.page(pageInfo,lqw);
        return R.success(pageInfo);
    }

    @PutMapping
    public R<String> updateOrderStatus(@RequestBody Orders orders) {
        orderService.updateById(orders);
        return R.success("狀態更新成功！");
    }
}
