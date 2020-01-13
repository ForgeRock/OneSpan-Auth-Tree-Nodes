package com.os.tid.forgerock.openam.utils;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

public class StringUtils {
    private final static Logger logger = LoggerFactory.getLogger("amAuth");

    private StringUtils() {
    }

    public static String stringToHex(String string) {
        return String.format("%040x", new BigInteger(1, string.getBytes(StandardCharsets.UTF_8)));
    }

    public static String hexToString(String hex){
        try {
            return new String(Hex.decodeHex(hex.toCharArray()));
        } catch (DecoderException e) {
            logger.error("StringUtils exception: " + e.getMessage());
            return "";
        }
    }

    //"https://duoliang11071-mailin.sdb.tid.onespan.cloud"
    public static String getAPIEndpoint(String ostid_tenant_name, String ostid_environment){
        return String.format("https://%1$s.%2$s.tid.onespan.cloud",ostid_tenant_name,ostid_environment);
    }
}
