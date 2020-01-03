package com.os.tid.forgerock.openam.models;

import com.alibaba.fastjson.JSONObject;

public class HttpEntity {
    private JSONObject responseJSON;
    private int httpStatus;
    private String log_correlation_id;

    public HttpEntity(JSONObject responseJSON, int httpStatus, String log_correlation_id) {
        this.responseJSON = responseJSON;
        this.httpStatus = httpStatus;
        this.log_correlation_id = log_correlation_id;
    }

    public JSONObject getResponseJSON() {
        return responseJSON;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getLog_correlation_id() {
        return log_correlation_id;
    }

    public void setResponseJSON(JSONObject responseJSON) {
        this.responseJSON = responseJSON;
    }

    public void setHttpStatus(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    public void setLog_correlation_id(String log_correlation_id) {
        this.log_correlation_id = log_correlation_id;
    }

    public boolean isSuccess() {
        return httpStatus >= 200 && httpStatus <= 299;
    }
}
