package com.itheima.reggie.filter;

import com.alibaba.fastjson.JSON;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.util.AntPathMatcher;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebFilter(filterName = "loginCheckinFilter", urlPatterns = "/*")
@Slf4j
public class LoginCheckFilter implements Filter {

    public static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        String requestURI = request.getRequestURI();
        String[] urls = new String[] {
                "/employee/login",
                "/employee/logout",
                "/backend/**",
                "/front/**",
                "/user/sendMsg",
                "/user/login",
                "/doc.html",
                "/webjars/**",
                "/swagger-resources",
                "/v2/api-docs"
        };
        //log.info("攔截到{}", request.getRequestURI());
        if (!check(urls, requestURI)) {
            Long employee = (Long) request.getSession().getAttribute("employee");
            Long user = (Long) request.getSession().getAttribute("user");
            if (employee == null && user == null) {
                // if the user has not logged, then use the outputStream to the client side
                response.getWriter().write(JSON.toJSONString(R.error("NOTLOGIN")));
                return;
            }
            //log.info("user has logged in with {}", request.getSession().getAttribute("employee"));
            if (employee != null) {
                BaseContext.setCurrentId(employee);
            }

            if (user != null) {
                BaseContext.setCurrentId(user);
            }
        }
        filterChain.doFilter(request, response);
    }

    public boolean check(String[] urls, String requestURI) {
        for (String url : urls) {
            if (PATH_MATCHER.match(url, requestURI)) return true;
        }
        return false;
    }
}
