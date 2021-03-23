package com.igferry.exception;

import com.igferry.constants.IgferryExceptionEnum;

/**
 *  @author: yangchenghuan
 *  @Date: 2021/3/21
 *  @Description: error exception
 */
public final class IGFerryException extends RuntimeException {

    private String errCode;

    private String errMsg;

    public IGFerryException(IgferryExceptionEnum exceptionEnum) {
        super(exceptionEnum.getMsg());
        this.errCode = exceptionEnum.getCode();
        this.errMsg = exceptionEnum.getMsg();
    }

    public IGFerryException(String errMsg) {
        super(errMsg);
        this.errMsg = errMsg;
        this.errCode = "5000";
    }

    public IGFerryException(String code, String errMsg) {
        this.errCode = code;
        this.errMsg = errMsg;
    }


    public String getCode() {
        return errCode;
    }

    public String getErrMsg() {
        return errMsg;
    }
}
