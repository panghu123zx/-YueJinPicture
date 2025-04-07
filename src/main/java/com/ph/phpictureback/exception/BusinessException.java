package com.ph.phpictureback.exception;

import lombok.Getter;

/**
 * 业务异常的封装
 */
@Getter
public class BusinessException extends RuntimeException{
    /**
     * 错误码
     */
    private final int code;

    public BusinessException(int code,String message){
        super(message);
        this.code=code;
    }

    public BusinessException(ErrorCode errorCode){
        super(errorCode.getMessage());
        this.code=errorCode.getCode();
    }
    public BusinessException(ErrorCode errorCode,String message){
        super(message);
        this.code=errorCode.getCode();
    }
}
