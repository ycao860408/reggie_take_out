package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.common.CustomException;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.DishFlavor;
import com.itheima.reggie.mapper.DishMapper;
import com.itheima.reggie.service.DishFlavorService;
import com.itheima.reggie.service.DishService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {

    @Autowired
    private DishFlavorService dishFlavorService;

    @Override
    @Transactional
    public void saveWithFlavor(DishDto dishDto) {
        this.save(dishDto);
        Long id = dishDto.getId();
        List<DishFlavor> flavors = dishDto.getFlavors();

        for (DishFlavor df : flavors) {
            df.setDishId(id);
        }

        dishFlavorService.saveBatch(flavors);
    }


    @Override
    public DishDto getByIdWithFlavor(Long id) {
        final Dish dish = this.getById(id);
        DishDto res = new DishDto();
        BeanUtils.copyProperties(dish, res);

        LambdaQueryWrapper<DishFlavor> lqw = new LambdaQueryWrapper<>();
        lqw.eq(DishFlavor::getDishId, id);
        final List<DishFlavor> flavorList = dishFlavorService.list(lqw);
        res.setFlavors(flavorList);
        return res;
    }

    @Override
    @Transactional
    public void updateWithFlavor(DishDto dishDto) {
        this.updateById(dishDto);

        // delete the old record in dish_flavor table
        LambdaQueryWrapper<DishFlavor> dql = new LambdaQueryWrapper<>();
        dql.eq(DishFlavor::getDishId, dishDto.getId());
        dishFlavorService.remove(dql);
        // add new flavors to this id
        List<DishFlavor> flavors = dishDto.getFlavors();
        for (DishFlavor df : flavors) {
            df.setDishId(dishDto.getId());
        }
        dishFlavorService.saveBatch(flavors);
    }


    @Override
    @Transactional
    public void removeWithFlavor(List<Long> ids) {
        LambdaQueryWrapper<Dish> lqw1 = new LambdaQueryWrapper<>();
        lqw1.in(Dish::getId, ids);
        lqw1.eq(Dish::getStatus, 1);
        int count = this.count(lqw1);
        if (count > 0) {
            throw new CustomException("菜品正在售賣中，不能刪除");
        }
        this.removeByIds(ids);
        LambdaQueryWrapper<DishFlavor> lqw2 = new LambdaQueryWrapper<>();
        lqw2.in(DishFlavor::getDishId, ids);
        dishFlavorService.remove(lqw2);
    }
}
