package com.os.tid.forgerock.openam.utils;

import java.io.IOException;
import java.net.URI;
import java.util.Base64;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.ImmutableMap;
import com.os.tid.forgerock.openam.config.Constants;
import com.os.tid.forgerock.openam.models.HttpEntity;

public class RestUtils {
    private static final Logger logger = LoggerFactory.getLogger("amAuth");

    private RestUtils() {
    }

    public static HttpEntity doPostJSON(String url, String payload, SSLConnectionSocketFactory sslConSocFactory) throws IOException {
        logger.debug("RestUtils doPostJSON url: " + url);
        logger.debug("RestUtils doPostJSON payload: " + payload);

        org.apache.http.HttpEntity entity = new org.apache.http.entity.StringEntity(payload, ContentType.APPLICATION_JSON);
        
        HttpDynamicMethod httpDynamicMethod = new HttpDynamicMethod("POST", url);
        httpDynamicMethod.setEntity(entity);
        httpDynamicMethod.setHeader("Content-Type", "application/json");
        httpDynamicMethod.setHeader("Accept", "application/json");

	    HttpClientBuilder clientbuilder = HttpClients.custom();
	    if(sslConSocFactory != null) {
	    	clientbuilder.setSSLSocketFactory(sslConSocFactory);
	    }
        try (     
    	     CloseableHttpClient httpClient = clientbuilder.build();
             CloseableHttpResponse response = httpClient.execute(httpDynamicMethod)) {
        	int sourceResponseCode = response.getStatusLine().getStatusCode();
        	logger.debug("RestUtils doPostJSON response status: " + sourceResponseCode);
        	String responseBody = null;
        	if(response.getEntity() != null) {
        		responseBody = EntityUtils.toString(response.getEntity());
	            logger.debug("RestUtils doPostJSON response: " + responseBody);
        	}
            Header headerField = response.getFirstHeader(Constants.OSTID_LOG_CORRELATION_ID);
            String log_correlation_id = headerField != null && !StringUtils.isEmpty(headerField.getValue()) ? headerField.getValue() : "";
            try {
                return new HttpEntity(JSON.parseObject(responseBody), sourceResponseCode, log_correlation_id);
            } catch (Exception e) {
                return new HttpEntity(new JSONObject(ImmutableMap.of("response",responseBody)), sourceResponseCode, log_correlation_id);
            }
        }
    }
    
    
    public static HttpEntity doPutJSON(String url, String payload, SSLConnectionSocketFactory sslConSocFactory) throws IOException {
        logger.debug("RestUtils doPutJSON url: " + url);
        logger.debug("RestUtils doPutJSON payload: " + payload);

        org.apache.http.HttpEntity entity = new org.apache.http.entity.StringEntity(payload, ContentType.APPLICATION_JSON);
        
        HttpDynamicMethod httpDynamicMethod = new HttpDynamicMethod("PUT", url);
        httpDynamicMethod.setEntity(entity);
        httpDynamicMethod.setHeader("Content-Type", "application/json");
        httpDynamicMethod.setHeader("Accept", "application/json");

	    HttpClientBuilder clientbuilder = HttpClients.custom();
	    if(sslConSocFactory != null) {
	    	clientbuilder.setSSLSocketFactory(sslConSocFactory);
	    }
        try (     
    	     CloseableHttpClient httpClient = clientbuilder.build();
             CloseableHttpResponse response = httpClient.execute(httpDynamicMethod)) {
           	int sourceResponseCode = response.getStatusLine().getStatusCode();
           	logger.debug("RestUtils doPutJSON response status: " + sourceResponseCode);
           	String responseBody = null;
           	if(response.getEntity() != null) {
           		responseBody = EntityUtils.toString(response.getEntity());
   	            logger.debug("RestUtils doPutJSON response: " + responseBody);
           	}
           Header headerField = response.getFirstHeader(Constants.OSTID_LOG_CORRELATION_ID);
           String log_correlation_id = headerField != null && !StringUtils.isEmpty(headerField.getValue()) ? headerField.getValue() : "";
           try {
               return new HttpEntity(JSON.parseObject(responseBody), sourceResponseCode, log_correlation_id);
           } catch (Exception e) {
               return new HttpEntity(new JSONObject(ImmutableMap.of("response",responseBody)), sourceResponseCode, log_correlation_id);
           }
       }
    }
    
    
    
    public static HttpEntity doPatchJSON(String url, String payload, SSLConnectionSocketFactory sslConSocFactory) throws IOException {
        logger.debug("RestUtils doPatchJSON url: " + url);
        logger.debug("RestUtils doPatchJSON payload: " + payload);

        org.apache.http.HttpEntity entity = new org.apache.http.entity.StringEntity(payload, ContentType.APPLICATION_JSON);
        
        HttpDynamicMethod httpDynamicMethod = new HttpDynamicMethod("PATCH", url);
        httpDynamicMethod.setEntity(entity);
        httpDynamicMethod.setHeader("Content-Type", "application/json");
        httpDynamicMethod.setHeader("Accept", "application/json");

	    HttpClientBuilder clientbuilder = HttpClients.custom();
	    if(sslConSocFactory != null) {
	    	clientbuilder.setSSLSocketFactory(sslConSocFactory);
	    }
        try (     
    	     CloseableHttpClient httpClient = clientbuilder.build();
             CloseableHttpResponse response = httpClient.execute(httpDynamicMethod)) {
           	int sourceResponseCode = response.getStatusLine().getStatusCode();
           	logger.debug("RestUtils doPatchJSON response status: " + sourceResponseCode);
           	String responseBody = null;
           	if(response.getEntity() != null) {
           		responseBody = EntityUtils.toString(response.getEntity());
   	            logger.debug("RestUtils doPatchJSON response: " + responseBody);
           	}
           Header headerField = response.getFirstHeader(Constants.OSTID_LOG_CORRELATION_ID);
           String log_correlation_id = headerField != null && !StringUtils.isEmpty(headerField.getValue()) ? headerField.getValue() : "";
           try {
               return new HttpEntity(JSON.parseObject(responseBody), sourceResponseCode, log_correlation_id);
           } catch (Exception e) {
               return new HttpEntity(new JSONObject(ImmutableMap.of("response",responseBody)), sourceResponseCode, log_correlation_id);
           }
       }
    }

    public static HttpEntity doHttpRequestWithoutResponse(String url, String payload, String httpmethod, Map<String, String> requestHeaders, SSLConnectionSocketFactory sslConSocFactory) throws IOException {
        logger.debug("RestUtils doHttpRequestWithoutResponse url: " + url);
        logger.debug("RestUtils doHttpRequestWithoutResponse payload: " + payload);

        org.apache.http.HttpEntity entity = new org.apache.http.entity.StringEntity(payload, ContentType.APPLICATION_JSON);
        
        HttpDynamicMethod httpDynamicMethod = new HttpDynamicMethod(httpmethod, url);
        httpDynamicMethod.setEntity(entity);
        httpDynamicMethod.setHeader("Content-Type", "application/json");
        httpDynamicMethod.setHeader("Accept", "application/json");
        if(requestHeaders != null && requestHeaders.size() > 0) {
			for (Entry<String, String> entry : requestHeaders.entrySet()) {
				String key = entry.getKey();
				String value = entry.getValue();
				httpDynamicMethod.setHeader(key, value);
			}
        }
        
	    HttpClientBuilder clientbuilder = HttpClients.custom();
	    if(sslConSocFactory != null) {
	    	clientbuilder.setSSLSocketFactory(sslConSocFactory);
	    }
        try (     
    	     CloseableHttpClient httpClient = clientbuilder.build();
             CloseableHttpResponse response = httpClient.execute(httpDynamicMethod)) {
           	int sourceResponseCode = response.getStatusLine().getStatusCode();
           	logger.debug("RestUtils doHttpRequestWithoutResponse response status: " + sourceResponseCode);
           	String responseBody = null;
           	if(response.getEntity() != null) {
           		responseBody = EntityUtils.toString(response.getEntity());
   	            logger.debug("RestUtils doHttpRequestWithoutResponse response: " + responseBody);
           	}
           Header headerField = response.getFirstHeader(Constants.OSTID_LOG_CORRELATION_ID);
           String log_correlation_id = headerField != null && !StringUtils.isEmpty(headerField.getValue()) ? headerField.getValue() : "";
           try {
               return new HttpEntity(JSON.parseObject(responseBody), sourceResponseCode, log_correlation_id);
           } catch (Exception e) {
               return new HttpEntity(new JSONObject(ImmutableMap.of("response",responseBody)), sourceResponseCode, log_correlation_id);
           }
       }
    }

    
    public static HttpEntity doGet(String url, SSLConnectionSocketFactory sslConSocFactory) throws IOException {
        logger.debug("RestUtils doGet url: " + url);
        
        HttpDynamicMethod httpDynamicMethod = new HttpDynamicMethod("GET", url);
        httpDynamicMethod.setHeader("Accept", "application/json");
        
	    HttpClientBuilder clientbuilder = HttpClients.custom();
	    if(sslConSocFactory != null) {
	    	clientbuilder.setSSLSocketFactory(sslConSocFactory);
	    }
        try (     
    	     CloseableHttpClient httpClient = clientbuilder.build();
             CloseableHttpResponse response = httpClient.execute(httpDynamicMethod)) {
           	int sourceResponseCode = response.getStatusLine().getStatusCode();
           	logger.debug("RestUtils doGet response status: " + sourceResponseCode);
           	String responseBody = null;
           	if(response.getEntity() != null) {
           		responseBody = EntityUtils.toString(response.getEntity());
   	            logger.debug("RestUtils doGet response: " + responseBody);
           	}
           Header headerField = response.getFirstHeader(Constants.OSTID_LOG_CORRELATION_ID);
           String log_correlation_id = headerField != null && !StringUtils.isEmpty(headerField.getValue()) ? headerField.getValue() : "";
           try {
               return new HttpEntity(JSON.parseObject(responseBody), sourceResponseCode, log_correlation_id);
           } catch (Exception e) {
               return new HttpEntity(new JSONObject(ImmutableMap.of("response",responseBody)), sourceResponseCode, log_correlation_id);
           }
       }
    }
    

    public static String doGetImage(String url, SSLConnectionSocketFactory sslConSocFactory) throws IOException {
        logger.debug("RestUtils doGetImage url: " + url);
        
        HttpDynamicMethod httpDynamicMethod = new HttpDynamicMethod("GET", url);
        httpDynamicMethod.setHeader("Accept", "image/png");
        
	    HttpClientBuilder clientbuilder = HttpClients.custom();
	    if(sslConSocFactory != null) {
	    	clientbuilder.setSSLSocketFactory(sslConSocFactory);
	    }
        try (     
    	     CloseableHttpClient httpClient = clientbuilder.build();
             CloseableHttpResponse response = httpClient.execute(httpDynamicMethod)) {
           	int sourceResponseCode = response.getStatusLine().getStatusCode();
           	logger.debug("RestUtils doGetImage response status: " + sourceResponseCode);
           	String imageForHtml  = null;
           	if(response.getEntity() != null) {
                byte[] imageBytes = EntityUtils.toByteArray(response.getEntity());
                String imageBase64  = Base64.getEncoder().encodeToString(imageBytes);
            	imageForHtml = "data:image/png;base64," + imageBase64;
   	            logger.debug("RestUtils doGetImage imageBase64 : " + imageForHtml);
           	}
           try {
               return imageForHtml;
           } catch (Exception e) {
               return url;
           }
       }
    }
    

}

class HttpDynamicMethod extends HttpEntityEnclosingRequestBase {
    private final String methodName;

    public HttpDynamicMethod(final String methodName, final String uri) {
        super();
        this.methodName = methodName;
        setURI(URI.create(uri));
    }

    @Override
    public String getMethod() {
        return methodName;
    }
}
