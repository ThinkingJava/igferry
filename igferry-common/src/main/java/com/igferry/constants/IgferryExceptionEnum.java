package com.igferry.constants;

/**
 *  @author: yangchenghuan
 *  @Date: 2021/3/14
 *  @Description:
 */
public enum IgferryExceptionEnum {
    /**
     * param error
     */
    PARAM_ERROR("1000", "param error"),
    /**
     * service not find
     */
    SERVICE_NOT_FIND("1001", "service not find,maybe not register"),
    /**
     * invalid config
     */
    CONFIG_ERROR("1002", "invalid config"),
    /**
     * userName or password error
     */
    LOGIN_ERROR("1003", "userName or password error"),
    /**
     *  not login
     */
    NOT_LOGIN("1004","not login"),
    /**
     * token error
     */
    TOKEN_ERROR("1005","token error");

    private String code;

    private String msg;

    IgferryExceptionEnum(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public String getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
