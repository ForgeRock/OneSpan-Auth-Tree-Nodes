package com.os.tid.forgerock.openam.backup;

public enum ActivationStatusEnum {
    PENDING("pending"),
    ACTIVATED("activated"),
    TIMEOUT("timeout"),
    UNKNOWN("unknown");

    private final String value;

    public String getValue() {
        return value;
    }

    ActivationStatusEnum(String value){this.value=value;}
}
