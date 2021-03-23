package com.igferry.kafka.context;

import java.util.Map;

public class Response<T> {

    private String topic;
    private T body;
    private Map<String, Object> extMap;
    private String errCode;
    private String errMsg;
    private String stepMsg;

    public Response(){}

    public Response( String errCode, String errMsg) {
        this.topic = topic;
        this.errCode = errCode;
        this.errMsg = errMsg;
    }


    public Response(String topic, String errCode, String errMsg) {
        this.topic = topic;
        this.errCode = errCode;
        this.errMsg = errMsg;
    }

    public Response(T body,String topic, String errCode, String errMsg) {
        this.body = body;
        this.topic = topic;
        this.errCode = errCode;
        this.errMsg = errMsg;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public T getBody() {
        return body;
    }

    public void setBody(T body) {
        this.body = body;
    }

    public Map<String, Object> getExtMap() {
        return extMap;
    }

    public void setExtMap(Map<String, Object> extMap) {
        this.extMap = extMap;
    }

    public String getErrCode() {
        return errCode;
    }

    public void setErrCode(String errCode) {
        this.errCode = errCode;
    }

    public String getErrMsg() {
        return errMsg;
    }

    public void setErrMsg(String errMsg) {
        this.errMsg = errMsg;
    }

    public String getStepMsg() {
        return stepMsg;
    }

    public void setStepMsg(String stepMsg) {
        this.stepMsg = stepMsg;
    }
}
