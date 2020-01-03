package com.os.tid.forgerock.openam.utils;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

public class StringUtils {

    private StringUtils() {
    }

    public static String toHex(String arg) {
        return String.format("%040x", new BigInteger(1, arg.getBytes(StandardCharsets.UTF_8)));
    }

    //"https://duoliang11071-mailin.sdb.tid.onespan.cloud"
    public static String getAPIEndpoint(String ostid_tenant_name, String ostid_environment){
        return String.format("https://%1$s.%2$s.tid.onespan.cloud",ostid_tenant_name,ostid_environment);
    }
}
