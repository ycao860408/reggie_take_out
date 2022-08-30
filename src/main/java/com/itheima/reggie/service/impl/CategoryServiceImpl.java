package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.common.CustomException;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.mapper.CategoryMapper;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.DishService;
import com.itheima.reggie.service.SetmealService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Autowired
    private DishService dishService;

    @Autowired
    private SetmealService setmealService;


    /**
     * check whether this category has associated with any meal or set meal?
     * @param id
     */
    @Override
    public void remove(Long id) {
        LambdaQueryWrapper<Dish> dlqw = new LambdaQueryWrapper<>();
        dlqw.eq(Dish::getCategoryId, id);
        int countDish = dishService.count(dlqw);

        if (countDish > 0) {
            throw new CustomException("當前分類下關聯了菜品， 不能刪除！");
        }

        LambdaQueryWrapper<Setmeal> slqw = new LambdaQueryWrapper<>();
        slqw.eq(Setmeal::getCategoryId, id);
        int countSetMeal = setmealService.count(slqw);
        if (countSetMeal > 0) {
            throw new CustomException("當前分類下關聯了套餐， 不能刪除！");
        }
        super.removeById(id);
    }
}
