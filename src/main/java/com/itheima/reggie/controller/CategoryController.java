package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/category")
@Slf4j
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @PostMapping
    public R<String> save(@RequestBody Category category) {
        log.info("category {}", category);
        categoryService.save(category);
        return R.success("新增分類成功！");
    }

    @GetMapping("/page")
    public R<Page> getPage(int page, int pageSize) {
        Page<Category> pageInfo = new Page<>(page, pageSize);
        LambdaQueryWrapper<Category> lqw = new LambdaQueryWrapper<>();
        lqw.orderByAsc(Category::getSort);
        categoryService.page(pageInfo, lqw);
        return R.success(pageInfo);
    }

    @DeleteMapping
    public R<String> delete(Long id) {
        log.info("根據id來刪除！");
        categoryService.remove(id);
        return R.success("分類刪除成功！");
    }

    @PutMapping
    public R<String> update(@RequestBody Category category) {
        log.info("修改分類信息！");
        categoryService.updateById(category);
        return R.success("修改分類成功！");
    }

    @GetMapping("/list")
    public R<List<Category>> list(Category category) {
        LambdaQueryWrapper<Category> lqw = new LambdaQueryWrapper<>();
        Integer type = category.getType();
        lqw.eq(type != null, Category::getType, type);
        lqw.orderByAsc(Category::getSort).orderByDesc(Category::getUpdateTime);
        List<Category> list = categoryService.list(lqw);
        return R.success(list);
    }


}
