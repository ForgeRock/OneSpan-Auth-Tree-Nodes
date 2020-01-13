package com.os.tid.forgerock.openam.models;

public class ExceptionOutput {
    private String retcode;
    private String business_retcode;
    private String message;

    public ExceptionOutput(String retcode, String business_retcode, String message) {
        this.retcode = retcode;
        this.business_retcode = business_retcode;
        this.message = message;
    }

    public String getRetcode() {
        return retcode;
    }

    public String getBusiness_retcode() {
        return business_retcode;
    }

    public String getMessage() {
        return message;
    }
}
