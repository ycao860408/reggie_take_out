package com.itheima.reggie.common;

public class BaseContext {
    private static ThreadLocal<Long> curLocal = new ThreadLocal<>();

    public static void setCurrentId(Long id){
        curLocal.set(id);
    }

    public static Long getCurrentId() {
        return curLocal.get();
    }
}
