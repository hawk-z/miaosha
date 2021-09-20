package com.peace.response;

import lombok.Data;

@Data
public class CommonReturnType {
    private String status;
    private Object data;

    //没有传入状态默认是SUCCESS
    public static CommonReturnType create(Object res){
        return  CommonReturnType.create(res,"SUCCESS");
    }

    public static CommonReturnType create(Object res,String status){
        CommonReturnType type = new CommonReturnType();
        type.setData(res);
        type.setStatus(status);
        return type;
    }
}
