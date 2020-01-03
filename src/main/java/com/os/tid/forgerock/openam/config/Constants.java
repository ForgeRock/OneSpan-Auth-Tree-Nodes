package com.os.tid.forgerock.openam.config;

import java.util.Date;

public class Constants {
    private Constants() {
    }


    /**
     * OneSpan TID API
     */
    public static String OSTID_API_USER_REGISTER = "/userregister/v1/";
    public static String OSTID_API_USER_UNREGISTER = "/userunregister/v1/";
    public static String OSTID_API_CHECK_ACTIVATION = "/checkactivationstatus/v1";
    public static String OSTID_API_CHECK_SESSION_STATUS = "/v1/sessions/%1$s";
    public static String OSTID_API_CRTONTO_RENDER = "/visualcode/v1/render?format=%1$s&message=%2$s";
    public static String OSTID_API_EVENT_VALIDATION = "/eventvalidation/v2";
    public static String OSTID_API_TRANSACTION = "/transaction/v2";
//    public static String OSTID_API_EVENT_VALIDATION_V1 = "/event/v1";

    public static String OSTID_JSON_USER_REGISTER = "{%6$s\"login\":\"%1$s\",\"clientIP\":\"%2$s\",\"browserCDDC\":{\"fingerprintHash\":\"%3$s\",\"fingerprintRaw\":%4$s},\"sessionIdentifier\":\"%5$s\"}";
    public static String OSTID_JSON_CHECK_ACTIVATION = "{\"login\":\"%1$s\",\"timeoutSeconds\":\"%2$d\"}";
    public static String OSTID_JSON_EVENT_VALIDATION = "{%1$s%2$s\"eventType\":\"%3$s\",\"login\":\"%4$s\",\"clientIP\":\"%5$s\",\"browserCDDC\":{\"fingerprintHash\":\"%6$s\",\"fingerprintRaw\":%7$s},\"sessionID\":\"%8$s\",\"digipassDomain\":\"%9$s\",\"dataToSign\":\"%4$s#%9$s\"}";
    public static String OSTID_JSON_PASSKEY = "\"passKey\":\"%1$s\",";
    public static String OSTID_JSON_ISNOTIFIED = "\"notificationsActivated\":%1$s,";
    public static String OSTID_CRONTO_FORMULA = "%1$s;%2$s;001;%3$s;%4$s;%5$s";


    public static String OSTID_JSON_TRANSACTIONS = "{%1$s%13$s%14$s\"accountRef\":\"%2$s\",\"amount\":\"%3$s\",\"currency\":\"%4$s\",\"transactionType\":\"%5$s\",\"creditorIBAN\":\"%6$s\",\"creditorName\":\"%7$s\",\"dataToSign\":[\"{\\\"amount\\\": \\\"%3$s\\\",\\\"beneficiary\\\": \\\"%7$s\\\",\\\"currency\\\": \\\"%4$s\\\",\\\"iban\\\": \\\"%6$s\\\"}\"],\"login\":\"%8$s\",\"sessionID\":\"%9$s\",\"browserCDDC\":{\"fingerprintRaw\":%10$s,\"fingerprintHash\":\"%11$s\"},\"clientIP\":\"%12$s\"}";



    public static String OSTID_RESPONSE_CHECK_ACTIVATION_STATUS = "activationStatus";


    /**
     * Attributes Keys in Shared/Transient Data
     */
    public static String OSTID_ERROR_MESSAGE = "ostid_error_message";

    public static String OSTID_CDDC_JSON = "osstid_cddc_json";
    public static String OSTID_CDDC_HASH = "osstid_cddc_hash";
    public static String OSTID_CDDC_IP = "osstid_cddc_ip";
    public static String OSTID_CDDC_HAS_PUSHED_JS = "ostid_cddc_has_pushed_js";
    public static String OSTID_USERNAME_IN_SHARED_STATE = "ostid_username_in_shared_state";
//    public static String OSTID_PASSWORD_IN_SHARED_STATE = "ostid_password_in_shared_state";

    public static String OSTID_DIGI_SERIAL = "osstid_digi_serial";
    public static String OSTID_CRONTO = "osstid_cronto";
    public static String OSTID_CRONTO_STATUS = "ostid_cronto_status";   //true:display, false:hide
    public static String OSTID_CRONTO_HAS_RENDERED = "ostid_cronto_has_rendered";   //true:display, false:hide


//    public static String OSTID_SESSIONIDENTIFIER = "osstid_session_identifier";
    public static String OSTID_SESSIONID = "osstid_session_id";
    public static String OSTID_REQUEST_ID = "ostid_request_id";

    public static String OSTID_EVENT_EXPIRY_DATE = "ostid_event_expiry_date";
    public static String OSTID_CRONTO_MSG = "ostid_cronto_msg";
    public static String OSTID_CRONTO_PUSH_JS = "ostid_cronto_push_js";


    //    public static String OSTID_EVENT_VALIDATION_EXPIRY_DATE = "ostid_event_validation_expiry_date";
    //    public static String OSTID_ACTIVATION_TOKEN_EXPIRY_DATE = "ostid_activation_token_expiry_date";
    //    public static String OSTID_ACTIVATION_CODE = "osstid_activation_code";

    public static String OSTID_LOG_CORRELATION_ID = "log-correlation-id";

    /**
     * Default Values for OneSpan TID Auth Node settings
     */
    public static String OSTID_API_VERSION = "02";
    public static String OSTID_DEFAULT_CRONTO_ALT = "OneSpan TID Cronto Image";
    public static int OSTID_DEFAULT_CRONTO_HEIGHT = 210;
    public static String OSTID_DEFAULT_USERNAME = "username";
    public static String OSTID_DEFAULT_PASSKEY = "password";
    public static String OSTID_DEFAULT_ACCOUNTREF = "accountRef";
    public static String OSTID_DEFAULT_AMOUNT = "amount";
    public static String OSTID_DEFAULT_CURRENCY = "currency";
    public static String OSTID_DEFAULT_TRANSACTIONTYPE = "transactionType";
    public static String OSTID_DEFAULT_CREDITORIBAN = "creditorIBAN";
    public static String OSTID_DEFAULT_CREDITORNAME = "creditorName";
    public static String OSTID_DEFAULT_DEBTORNAME = "debtorName";







    public static int OSTID_DEFAULT_CHECK_ACTIVATION_TIMEOUT = 0;
    public static int OSTID_DEFAULT_ENUM_ERROR_CODE = -1;
    //todo, two expiry the same?
    public static int OSTID_DEFAULT_EVENT_EXPIRY = 300;
    //public static int OSTID_DEFAULT_ACTIVATION_TOKEN_EXPIRY = 300;
    //public static int OSTID_DEFAULT_EVENT_VALIDATION_EXPIRY = 300;
}
