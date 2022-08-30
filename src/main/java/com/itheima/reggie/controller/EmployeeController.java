package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.Employee;
import com.itheima.reggie.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/employee")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    @PostMapping("/login")
    public R<Employee> login(@RequestBody Employee employee, HttpServletRequest request) {
        // md5 password;
        String  password = employee.getPassword();
        String name = employee.getUsername();
        password = DigestUtils.md5DigestAsHex(password.getBytes());
        final LambdaQueryWrapper<Employee> lmw = new LambdaQueryWrapper<>();
        lmw.eq(Employee::getUsername, name);
        Employee one = employeeService.getOne(lmw); // because username has an unique key!

        //check the name has been found or not!
        if (one == null) {
            return R.error("登陸失敗！");
        }

        // match the password!

        if (!password.equals(one.getPassword())) {
            return R.error("密碼不正確，請重新輸入或找回密碼！");
        }

        if(one.getStatus() == 0) {
            return R.error("您的賬戶已被鎖定，請聯解鎖後再行登陸！");
        }
        request.getSession().setAttribute("employee", one.getId());
        return R.success(one);
    }

    @PostMapping("/logout")
    public R<String> logOut(HttpServletRequest request) {
        request.getSession().removeAttribute("employee");
        return R.success("退出成功！");
    }

    @PostMapping
    public R<String> save(HttpServletRequest request, @RequestBody Employee employee) {
        log.info("新增員工信息： {}", employee.toString());
        //new user will give a default password.
        employee.setPassword(DigestUtils.md5DigestAsHex("123456".getBytes()));
       /* employee.setCreateTime(LocalDateTime.now()); // get the current system time
        employee.setUpdateTime(LocalDateTime.now());
        long empid = (long)request.getSession().getAttribute("employee");
        employee.setCreateUser(empid);
        employee.setUpdateUser(empid);*/
        employeeService.save(employee);
        return R.success("新增員工添加成功");
    }

    @GetMapping("/page")
    public R<Page<Employee>> getPage(int page, int pageSize, String name) {
        log.info("page:{}, pageSize:{}, name: {}", page, pageSize, name);
        Page pageInfo = new Page(page, pageSize);
        LambdaQueryWrapper<Employee> lqw = new LambdaQueryWrapper();
        lqw.like(name!= null, Employee::getName, name);
        lqw.orderByDesc(Employee::getUpdateTime);
        employeeService.page(pageInfo, lqw);
        return R.success(pageInfo);
    }

    @PutMapping
    public R<String> update(HttpServletRequest request, @RequestBody Employee employee) {
        //發過去的時候在渲染頁面的時候，就已經失去精度了。之後，從頁面上拿到數據已經就是是去精度的數據，在查詢等等所有事情就都會出錯誤。辦法就是從根上發過去的數據不要用長整型
        //而是變化成為字符串之後，再傳過去。加入一個什麽序列化和反序列化的轉換器，such that一些類型在序列化的時候，按照要求轉化成為另一種類型。
        log.info("input to update: " + employee.toString());
        /*employee.setUpdateTime(LocalDateTime.now());
        long empid = (long)request.getSession().getAttribute("employee");
        employee.setUpdateUser(empid);*/
        employeeService.updateById(employee);
        return R.success("員工信息修改成功！");
    }

    @GetMapping("/{id}")
    public R<Employee> getById(@PathVariable("id") Long id) {
        log.info("Base on id to get the id");
        Employee employee = employeeService.getById(id);
        if (employee == null) {
            return R.error("員工信息不存在！");
        }
        return R.success(employee);
    }
}
