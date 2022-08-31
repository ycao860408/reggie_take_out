package com.itheima.reggie.controller;

import com.aliyuncs.utils.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.User;
import com.itheima.reggie.service.UserService;
import com.itheima.reggie.utils.ValidateCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private RedisTemplate redisTemplate;

    @PostMapping("/sendMsg")
    public R<String> sendMsg(@RequestBody User user, HttpSession session) {
        // read the phone number of the user;
        final String phone = user.getPhone();
        if (StringUtils.isEmpty(phone)) {
            return R.error("短信發送失敗");
        }
        // generate 4digit validate code
        final String code = ValidateCodeUtils.generateValidateCode(4).toString();
        log.info("code={}", code);
        // user aliyun to send the message about the validate code to the user
        //SMSUtils.sendMessage();
        // save the validate code in the session for later validation!~

        redisTemplate.opsForValue().set(phone, code,5l, TimeUnit.MINUTES);
        return R.success("手機驗證碼短信發送成功！");
    }


    @PostMapping("/login")
    public R<User> login(@RequestBody Map map, HttpSession session) {
        // get phonenumber
        String phone = (String)map.get("phone");
        // get validating code
        String code = (String)map.get("code");
        // get the target from session
        String targetCode = (String)redisTemplate.opsForValue().get(phone);
        // line up with target if so  let it go
        if (targetCode == null || !targetCode.equals(code)) {
            return R.error("登陸失敗！");
        }
        // check if there is no such phone number, register one!
        LambdaQueryWrapper<User> lqw = new LambdaQueryWrapper<>();
        lqw.eq(User::getPhone, phone);
        User user = userService.getOne(lqw);
        if (user == null) {
            user = new User();
            user.setPhone(phone);
            userService.save(user);
        }
        session.setAttribute("user", user.getId());
        redisTemplate.delete(phone);
        return R.success(user);
    }

    @PostMapping("/loginout")
    public R<String> loginOut(HttpSession session) {
        session.removeAttribute("user");
        return R.success("退出成功！");
    }
}
