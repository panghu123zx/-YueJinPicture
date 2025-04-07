package com.ph.phpictureback.common;

import com.ph.phpictureback.exception.ErrorCode;
import lombok.Data;

import java.io.Serializable;

@Data
public class BaseResponse<T> implements Serializable {

    private int code;
    /**
     * 内容
     */
    private T data;
    /**
     * 信息
     */
    private String message;

    /**
     * 封装返回信息
     * @param code
     * @param data
     * @param message
     */
    public BaseResponse(int code,T data,String message){
        this.code=code;
        this.data=data;
        this.message=message;
    }

    /**
     * 封装的返回信息，不带有信息
     * @param code
     * @param data
     */
    public BaseResponse(int code,T data){
        this(code,data,"");
    }

    /**
     * 封装返回错误的方法，不带有数据
     * @param errorCode
     */
    public BaseResponse(ErrorCode errorCode){
        this(errorCode.getCode(),null,errorCode.getMessage());
    }
}
