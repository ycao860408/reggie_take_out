package com.itheima.reggie.common;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Data
@ApiModel("返回結果")
public class R<T> implements Serializable {

    @ApiModelProperty("編碼")
    private Integer code; //编码：1成功，0和其它数字为失败
    @ApiModelProperty("錯誤信息")
    private String msg; //错误信息
    @ApiModelProperty("數據")
    private T data; //数据
    @ApiModelProperty("動態數據")
    private Map map = new HashMap(); //动态数据

    public static <T> R<T> success(T object) {
        R<T> r = new R<T>();
        r.data = object;
        r.code = 1;
        return r;
    }

    public static <T> R<T> error(String msg) {
        R r = new R();
        r.msg = msg;
        r.code = 0;
        return r;
    }

    public R<T> add(String key, Object value) {
        this.map.put(key, value);
        return this;
    }

}
