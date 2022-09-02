package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.CustomException;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.entity.SetmealDish;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.SetmealDishService;
import com.itheima.reggie.service.SetmealService;
import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Delete;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/setmeal")
@Slf4j
@Api(tags = "套餐相關接口")
public class SetmealController {

    @Autowired
    private SetmealService setmealService;

    @Autowired
    private SetmealDishService setmealDishService;

    @Autowired
    private CategoryService categoryService;

    @PostMapping
    @CacheEvict(value = "setmealCache", allEntries = true)
    @ApiOperation(value = "新增套餐接口")
    public R<String> save(@RequestBody SetmealDto setmealDto) {
        log.info("套餐信息 {}", setmealDto);
        setmealService.saveWithDish(setmealDto);
        return R.success("新增套餐成功！");
    }

    @GetMapping("/page")
    @ApiOperation(value = "套餐分頁查詢接口")
    @ApiImplicitParams({
            @ApiImplicitParam(name= "page", value = "頁碼", required = true),
            @ApiImplicitParam(name="pageSize", value = "每頁記錄數", required = true),
            @ApiImplicitParam(name="name", value = "套餐名稱", required = false)
    })
    public R<Page> page(Integer page, Integer pageSize, String name) {
        Page<Setmeal> pageInfo = new Page<>(page, pageSize);
        Page<SetmealDto> setmealPage = new Page<>();
        LambdaQueryWrapper<Setmeal> lqw = new LambdaQueryWrapper<>();
        lqw.like(name != null, Setmeal::getName, name);
        lqw.orderByDesc(Setmeal::getUpdateTime);
        setmealService.page(pageInfo, lqw);
        BeanUtils.copyProperties(pageInfo, setmealPage, "records");
        List<Setmeal> records = pageInfo.getRecords();
        List<SetmealDto> trecords = new ArrayList<>();
        for (Setmeal record : records) {
            SetmealDto trecord = new SetmealDto();
            BeanUtils.copyProperties(record, trecord);
            Long categoryId = record.getCategoryId();
            Category category = categoryService.getById(categoryId);
            if (category != null) {
                String categoryName = category.getName();
                trecord.setCategoryName(categoryName);
            }
            trecords.add(trecord);
        }
        setmealPage.setRecords(trecords);
        return R.success(setmealPage);
    }

    @DeleteMapping
    @CacheEvict(value = "setmealCache", allEntries = true)
    @ApiOperation("套餐刪除接口")
    public R<String> delete(@RequestParam List<Long> ids) {
        setmealService.removeWithDish(ids);
        return R.success("套餐數據刪除成功!");
    }

    @PostMapping("status/{status}")
    @ApiOperation("更改套餐售賣狀態接口")
    public R<String> updateStatus(@PathVariable Integer status, @RequestParam List<Long> ids) {
        //System.out.println(status);
        LambdaQueryWrapper<Setmeal> lqw = new LambdaQueryWrapper<>();
        lqw.in(Setmeal::getId, ids);
        lqw.eq(Setmeal::getStatus, status);
        int count = setmealService.count(lqw);

        if (count > 0) {
            throw new CustomException("請移除已經" + (status == 0 ? "停售": "啓售") + "的套餐！");
        }
        List<Setmeal> setmeals = new ArrayList<>();
        for (Long id : ids) {
            Setmeal setmeal = new Setmeal();
            setmeal.setStatus(status);
            setmeal.setId(id);
            setmeals.add(setmeal);
        }
        setmealService.updateBatchById(setmeals);
        return R.success("狀態更新成功！");
    }

    @GetMapping("/list")
    @Cacheable(value = "setmealCache", key = "#setmeal.categoryId + '_' + #setmeal.status")
    @ApiOperation("套餐條件查詢接口")
    public R<List<Setmeal>> list(Setmeal setmeal) {
        LambdaQueryWrapper<Setmeal> lqw = new LambdaQueryWrapper<>();
        final Long categoryId = setmeal.getCategoryId();
        final Integer status = setmeal.getStatus();
        lqw.eq(categoryId != null, Setmeal::getCategoryId, categoryId);
        lqw.eq(status != null, Setmeal::getStatus, status);
        lqw.orderByDesc(Setmeal::getUpdateTime);
        final List<Setmeal> list = setmealService.list(lqw);
        return R.success(list);
    }

   /* @GetMapping("/list")
    public R<List<SetmealDto>> list(Setmeal setmeal) {
        LambdaQueryWrapper<Setmeal> lqw = new LambdaQueryWrapper<>();
        final Long categoryId = setmeal.getCategoryId();
        final Integer status = setmeal.getStatus();
        lqw.eq(categoryId != null, Setmeal::getCategoryId, categoryId);
        lqw.eq(status != null, Setmeal::getStatus, status);
        lqw.orderByDesc(Setmeal::getUpdateTime);
        final List<Setmeal> templist = setmealService.list(lqw);
        List<SetmealDto> list = new ArrayList<>();
        for (Setmeal tempItem : templist) {
            SetmealDto trecord = new SetmealDto();
            BeanUtils.copyProperties(tempItem, trecord);
            Long tempCategoryId = tempItem.getCategoryId();
            Category category = categoryService.getById(tempCategoryId);
            if (category != null) {
                String categoryName = category.getName();
                trecord.setCategoryName(categoryName);
            }
            final Long itemId = tempItem.getId();
            LambdaQueryWrapper<SetmealDish> sdlqw = new LambdaQueryWrapper<>();
            sdlqw.eq(SetmealDish::getSetmealId, itemId);
            final List<SetmealDish> setmealDishList = setmealDishService.list(sdlqw);
            trecord.setSetmealDishes(setmealDishList);
            list.add(trecord);
        }
        return R.success(list);
    }*/

    @GetMapping("/{id}")
    @ApiOperation("根據Id查詢套餐接口")
    public R<SetmealDto> getById(@PathVariable("id") Long id) {
        final Setmeal setmeal = setmealService.getById(id);
        log.info("查出來的信息============================== {}", setmeal);
        SetmealDto setmealDto = new SetmealDto();
        BeanUtils.copyProperties(setmeal, setmealDto);
        final Long categoryId = setmeal.getCategoryId();
        final Category category = categoryService.getById(categoryId);
        if (category != null) {
            setmealDto.setCategoryName(category.getName());
        }

        LambdaQueryWrapper<SetmealDish> lqw = new LambdaQueryWrapper<>();
        lqw.eq(id != null, SetmealDish::getSetmealId, id);
        final List<SetmealDish> list = setmealDishService.list(lqw);
        setmealDto.setSetmealDishes(list);
        return R.success(setmealDto);
    }

    @PutMapping
    @ApiOperation("根據Id更新套餐接口")
    public R<String> update(@RequestBody SetmealDto setmealDto) {
        setmealService.updateById(setmealDto);
        return R.success("更新成功！");
    }

}
