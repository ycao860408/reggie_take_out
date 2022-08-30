package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.CustomException;
import com.itheima.reggie.entity.*;
import com.itheima.reggie.mapper.OrderMapper;
import com.itheima.reggie.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Orders> implements OrderService {

    @Autowired
    private ShoppingCartService shoppingCartService;

    @Autowired
    private UserService userService;

    @Autowired
    private AddressBookService addressBookService;

    @Autowired
    private OrderDetailService orderDetailService;

    @Override
    @Transactional
    public void submit(Orders orders) {
        // get the id of the user
        final Long currentId = BaseContext.getCurrentId();
        // get the data from the shoppingCart
        LambdaQueryWrapper<ShoppingCart> lqw1 = new LambdaQueryWrapper<>();
        lqw1.eq(ShoppingCart::getUserId, currentId);
        final List<ShoppingCart> list = shoppingCartService.list(lqw1);

        if (list == null || list.size() == 0) {
            throw new CustomException("購物車爲空，請先行下單！");
        }

        // get the information of the User;
        final User user = userService.getById(currentId);
        // get the information fo the address;
        final Long addressBookId = orders.getAddressBookId();
        final AddressBook addressBook = addressBookService.getById(addressBookId);
        if (addressBook == null) {
            throw new CustomException("地址信息有誤，不能下單！");
        }

        //insert data to the order table
        final long orderId = IdWorker.getId();

        AtomicInteger amount = new AtomicInteger(0);// thread safe

        List<OrderDetail> orderDetails = list.stream().map((item) ->{
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setOrderId(orderId);
            orderDetail.setName(item.getName());
            orderDetail.setNumber(item.getNumber());
            orderDetail.setDishFlavor(item.getDishFlavor());
            orderDetail.setDishId(item.getDishId());
            orderDetail.setSetmealId(item.getSetmealId());
            orderDetail.setImage(item.getImage());
            orderDetail.setAmount(item.getAmount());
            amount.addAndGet(item.getAmount().multiply(new BigDecimal(item.getNumber())).intValue());
            return orderDetail;
        }).collect(Collectors.toList());

        orders.setNumber(String.valueOf(orderId));
        orders.setId(orderId);
        orders.setOrderTime(LocalDateTime.now());
        orders.setCheckoutTime(LocalDateTime.now());
        orders.setStatus(2);
        orders.setAmount(new BigDecimal(amount.get()));
        orders.setUserId(currentId);
        orders.setUserName(user.getName());
        orders.setConsignee(addressBook.getConsignee());
        orders.setPhone(addressBook.getPhone());
        orders.setAddress((addressBook.getProvinceName() == null ? "" : addressBook.getProvinceName()) + (addressBook.getCityName() == null ? "" : addressBook.getCityName()
        ) + (addressBook.getDistrictName() == null ? "" : addressBook.getDistrictName()) + (addressBook.getDetail() == null ? "" : addressBook.getDetail()));
        this.save(orders);
        // insert data to the order detail table
        orderDetailService.saveBatch(orderDetails);
        // clean the shopingCart;
        shoppingCartService.remove(lqw1);
    }
}
