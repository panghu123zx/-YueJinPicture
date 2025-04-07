package com.ph.phpictureback.common;

import com.ph.phpictureback.exception.ErrorCode;

/**
 * 返回信息的封装类
 */
public class ResultUtils {

    /**
     * 成功
     * @param data 数据
     * @return
     * @param <T>
     */
    public static <T> BaseResponse<T> success(T data){
        return new BaseResponse<>(0,data,"ok");
    }

    /**
     * 失败
     * @param errorCode
     * @return
     */
    public static BaseResponse<?> error(ErrorCode errorCode){
        return new BaseResponse<>(errorCode);
    }

    /**
     * 失败，不带数据
     * @param code
     * @param message
     * @return
     */
    public static BaseResponse<?> error(int code,String message){
        return new BaseResponse<>(code,message);
    }

    /**
     *失败，带有错误码和自定义信息
     * @param errorCode
     * @param message
     * @return
     */
    public static BaseResponse<?> error(ErrorCode errorCode,String message){
        return new BaseResponse<>(errorCode.getCode(),message);
    }
}
