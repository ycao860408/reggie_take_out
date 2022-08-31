package com.itheima.reggie.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.CustomException;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.DishFlavor;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.DishFlavorService;
import com.itheima.reggie.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/dish")
@Slf4j
public class DishController {

    @Autowired
    private DishFlavorService dishFlavorService;

    @Autowired
    private DishService dishService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;


    @PostMapping
    public R<String> save(@RequestBody DishDto dishDto) {
        log.info("上傳菜品！");
        dishService.saveWithFlavor(dishDto);
        String key = "dish_" + dishDto.getCategoryId() + "_1";
        redisTemplate.delete(key);
        return R.success("新增菜品成功！");
    }

    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String name) {

        Page<Dish> pageInfo = new Page<>(page, pageSize);
        Page<DishDto> dtoPage = new Page<>();
        LambdaQueryWrapper<Dish> lqw = new LambdaQueryWrapper<>();
        lqw.like(name!= null, Dish::getName, name);
        lqw.orderByDesc(Dish::getUpdateTime);
        dishService.page(pageInfo, lqw);

        BeanUtils.copyProperties(pageInfo, dtoPage, "records");

        final List<Dish> records = pageInfo.getRecords();
        List<DishDto> list = records.stream().map((item) ->{
            final Long categoryId = item.getCategoryId();
            final Category category = categoryService.getById(categoryId);
            //System.out.println("=========================================" + category);
            DishDto dishDto = new DishDto();
            BeanUtils.copyProperties(item, dishDto);
            if (category != null) {
                final String cname = category.getName();
                dishDto.setCategoryName(cname);
            }
            return dishDto;
        }).collect(Collectors.toList());

        dtoPage.setRecords(list);
        return R.success(dtoPage);
    }

    @GetMapping("/{id}")
    public R<DishDto> getInfo(@PathVariable("id") Long id) {
        final DishDto withFlavor = dishService.getByIdWithFlavor(id);
        return R.success(withFlavor);
    }

    @PutMapping
    public R<String> update(@RequestBody DishDto dishDto) {
        log.info("上傳菜品！");
        dishService.updateWithFlavor(dishDto);
        String key = "dish_" + dishDto.getCategoryId() + "_1";
        redisTemplate.delete(key);
        return R.success("修改菜品成功！");
    }

    @PostMapping("status/{status}")
    public R<String> updateStatus(@PathVariable Integer status, @RequestParam List<Long> ids) {
        //System.out.println(status);
        LambdaQueryWrapper<Dish> lqw = new LambdaQueryWrapper<>();
        lqw.in(Dish::getId, ids);
        lqw.eq(Dish::getStatus, status);
        int count = dishService.count(lqw);

        if (count > 0) {
            throw new CustomException("請移除已經" + (status == 0 ? "停售": "啓售") + "的菜品！");
        }
        List<Dish> dishes = new ArrayList<>();
        for (Long id : ids) {
            Dish dish = new Dish();
            dish.setStatus(status);
            dish.setId(id);
            dishes.add(dish);
        }
        dishService.updateBatchById(dishes);
        return R.success("狀態更新成功！");
    }

    @DeleteMapping
    public R<String> deleteByIds(@RequestParam List<Long> ids) {
       /* if (ids.contains(",")) {
            String[] idz = ids.split(",");
            List<Long> lids = new ArrayList<>();
            for (String id : idz) {
                lids.add(Long.parseLong(id));
            }
            dishService.deleteByIdsWithFlavor(lids);
        } else {
            System.out.println("single " + ids);
            dishService.deleteByIdWithFlavor(Long.parseLong(ids));
        }*/
        dishService.removeWithFlavor(ids);
        return R.success("刪除成功！");
    }


    /*@GetMapping("/list")
    public R<List<Dish>> list(Dish dish) {
        LambdaQueryWrapper<Dish> lqw = new LambdaQueryWrapper<>();
        Long catId = dish.getCategoryId();
        lqw.eq(catId != null, Dish::getCategoryId, catId);
        lqw.eq(Dish::getStatus, 1);
        lqw.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);
        final List<Dish> list = dishService.list(lqw);
        return R.success(list);
    }*/

    @GetMapping("/list")
    public R<List<DishDto>> list(Dish dish) {
        List<DishDto> list = null;
        Long catId = dish.getCategoryId();
        String key = "dish_" +  catId + "_" + 1;
        list = (List<DishDto>)redisTemplate.opsForValue().get(key);
        if (list != null) {
            return R.success(list);
        }
        LambdaQueryWrapper<Dish> lqw = new LambdaQueryWrapper<>();
        lqw.eq(catId != null, Dish::getCategoryId, catId);
        lqw.eq(Dish::getStatus, 1);
        lqw.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);
        final List<Dish> templist = dishService.list(lqw);
        list = new ArrayList<>();
        for (Dish tempDish : templist) {
            DishDto dishDto = new DishDto();
            BeanUtils.copyProperties(tempDish, dishDto);
            final Long categoryId = tempDish.getCategoryId();
            final Category category = categoryService.getById(categoryId);
            if (category != null) {
                String categoryName = category.getName();
                dishDto.setCategoryName(categoryName);
            }
            final Long dishId = tempDish.getId();
            LambdaQueryWrapper<DishFlavor> dflqw = new LambdaQueryWrapper<>();
            dflqw.eq(DishFlavor::getDishId, dishId);
            final List<DishFlavor> dishFlavors = dishFlavorService.list(dflqw);
            dishDto.setFlavors(dishFlavors);
            list.add(dishDto);
        }
        redisTemplate.opsForValue().set(key,list, 60l, TimeUnit.MINUTES);
        return R.success(list);
    }


}
