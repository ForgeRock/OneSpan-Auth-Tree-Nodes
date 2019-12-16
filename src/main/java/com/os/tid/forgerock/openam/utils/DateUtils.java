package com.os.tid.forgerock.openam.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;

public class DateUtils {
    private static final Logger logger = LoggerFactory.getLogger("amAuth");

    private DateUtils() {
    }

    public static String getMilliStringAfterCertainSecs(int seconds){
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, seconds);
        return calendar.getTime().getTime() + "";
    }

    public static boolean hasExpired(String expiryTimeStamp){
        try {
            return System.currentTimeMillis() > Long.parseLong(expiryTimeStamp);
        }catch (NumberFormatException e){
            return false;
        }
    }
}
