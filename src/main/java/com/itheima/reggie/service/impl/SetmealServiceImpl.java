package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.common.CustomException;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.entity.SetmealDish;
import com.itheima.reggie.mapper.SetmealMapper;
import com.itheima.reggie.service.SetmealDishService;
import com.itheima.reggie.service.SetmealService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
public class SetmealServiceImpl extends ServiceImpl<SetmealMapper, Setmeal> implements SetmealService {

    @Autowired
    private SetmealDishService setmealDishService;
    @Override
    @Transactional
    public void saveWithDish(SetmealDto setmealDto) {

        this.save(setmealDto);

        final Long id = setmealDto.getId();

        List<SetmealDish> list = setmealDto.getSetmealDishes();

        for (SetmealDish setmealDish: list) {
            setmealDish.setSetmealId(id);
        }

        setmealDishService.saveBatch(list);
    }

    @Override
    @Transactional
    public void removeWithDish(List<Long> ids) {

        LambdaQueryWrapper<Setmeal> lqw1 = new LambdaQueryWrapper<>();
        lqw1.in(Setmeal::getId, ids);
        lqw1.eq(Setmeal::getStatus, 1);
        int count = this.count(lqw1);
        if (count > 0) {
            throw new CustomException("套餐正在售賣中，不能刪除！");
        }

        this.removeByIds(ids);

        LambdaQueryWrapper<SetmealDish> lqw2 = new LambdaQueryWrapper<>();
        lqw2.in(SetmealDish::getSetmealId, ids);
        setmealDishService.remove(lqw2);
    }
}
