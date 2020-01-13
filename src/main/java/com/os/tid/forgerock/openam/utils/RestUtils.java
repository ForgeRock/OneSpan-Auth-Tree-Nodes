package com.os.tid.forgerock.openam.utils;

import com.alibaba.fastjson.JSON;
import com.os.tid.forgerock.openam.config.Constants;
import com.os.tid.forgerock.openam.models.HttpEntity;
import org.apache.commons.lang.StringUtils;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class RestUtils {
    private static final Logger logger = LoggerFactory.getLogger("amAuth");

    private RestUtils() {
    }

    public static HttpEntity doPostJSON(String url, String payload) throws IOException{
        URL client = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) client.openConnection();
        conn.setRequestProperty("Content-Type", "application/json;");
        conn.setRequestProperty("Accept", "application/json;");
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setDoInput(true);

        OutputStream os = conn.getOutputStream();
        os.write(payload.getBytes());
        os.flush();
        os.close();
        int sourceResponseCode = conn.getResponseCode();
        String headerField = conn.getHeaderField(Constants.OSTID_LOG_CORRELATION_ID);
        String log_correlation_id = StringUtils.isEmpty(headerField) ? "" : headerField;

        logger.debug("RestUtils request payload: " + payload);
        logger.debug("RestUtils doPostJSON: " + url + " : " +sourceResponseCode);

        Reader ir = (sourceResponseCode >= 200 && sourceResponseCode <= 299) ? new InputStreamReader(conn.getInputStream())
                : new InputStreamReader(conn.getErrorStream());
        BufferedReader in = new BufferedReader(ir);
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }

        in.close();
        conn.disconnect();
        logger.debug("RestUtils response: " + response.toString());

        return new HttpEntity(JSON.parseObject(response.toString()),sourceResponseCode,log_correlation_id);
    }


    public static HttpEntity doGet(String url) throws IOException{
        URL client = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) client.openConnection();
        conn.setRequestProperty("Content-Type", "application/json;");
        conn.setRequestProperty("Accept", "application/json;");
        conn.setRequestMethod("GET");

        int sourceResponseCode = conn.getResponseCode();
        String headerField = conn.getHeaderField(Constants.OSTID_LOG_CORRELATION_ID);
        String log_correlation_id = StringUtils.isEmpty(headerField) ? "" : headerField;

        logger.debug("RestUtils doGet: " + url + " : " +sourceResponseCode);

        Reader ir = (sourceResponseCode >= 200 && sourceResponseCode <= 299) ? new InputStreamReader(conn.getInputStream())
                : new InputStreamReader(conn.getErrorStream());
        BufferedReader in = new BufferedReader(ir);
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }

        in.close();
        conn.disconnect();
        logger.debug("RestUtils response: " + response.toString());

        return new HttpEntity(JSON.parseObject(response.toString()),sourceResponseCode,log_correlation_id);
    }

}
