package com.ph.phpictureback.exception;

/**
 * 异常抛出的工具类
 */
public class ThrowUtils {
    /**
     * 抛出运行时异常
     * @param condition 条件
     * @param runtimeException 异常
     */
    public static void throwIf(boolean condition,RuntimeException runtimeException){
        if(condition){
            throw runtimeException;
        }
    }

    /**
     * 抛出业务异常
     * @param condition 条件
     * @param errorCode 错误码
     */
    public static void throwIf(boolean condition,ErrorCode errorCode){
        if(condition){
            throw new BusinessException(errorCode);
        }
    }

    /**
     * 抛出业务异常，可以自定义错误信息
     * @param condition 条件
     * @param errorCode 错误码
     * @param message 错误信息
     */
    public static void throwIf(boolean condition,ErrorCode errorCode,String message){
        if(condition){
            throw new BusinessException(errorCode,message);
        }
    }
}
