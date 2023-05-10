package com.os.tid.forgerock.openam.config;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.os.tid.forgerock.openam.nodes.OSConfigurationsService.EnvOptions;

public class Constants {

    private Constants() {
    }

    /**
     * OneSpan TID API Endpoints
     */
    //API Endpoints
    public static final String OSTID_API_ADAPTIVE_USER_REGISTER = "/v1/users/register";
    public static final String OSTID_API_ADAPTIVE_USER_UNREGISTER = "/v1/users/%1$s@%2$s/unregister";
    public static final String OSTID_API_ADAPTIVE_USER_LOGIN = "/v1/users/%1$s@%2$s/login";
    public static final String OSTID_API_ADAPTIVE_EVENT_VALIDATION = "/v1/users/%1$s@%2$s/events/validate";
    public static final String OSTID_API_ADAPTIVE_SEND_TRANSACTION = "/v1/users/%1$s@%2$s/transactions/validate";
    public static final String OSTID_API_ADAPTIVE_ADD_DEVICE = "/v1/registrations/%1$s/add-device";
    public static final String OSTID_API_ADAPTIVE_GENERATE_CHALLENGE = "/v1/users/%1$s@%2$s/generate-challenge";
    public static final String OSTID_API_ADAPTIVE_ACTIVATE_DEVICE = "/v1/registrations/%1$s/activate";
    public static final String OSTID_API_CHECK_ACTIVATION = "/v1/registrations/check-status";
    public static final String OSTID_API_CHECK_SESSION_STATUS = "/v1/sessions/%1$s";
    public static final String OSTID_API_ADAPTIVE_CRTONTO_RENDER = "/v1/visualcodes/render?format=%1$s&message=%2$s";
    public static final String OSTID_API_RISK_SEND_TRANSACTION = "/v1/transactions";
    public static final String OSTID_API_VDP_USER_REGISTER = "/v1/users/%1$s@%2$s";
    public static final String OSTID_API_VDP_GET_VIR10_AUTHENTICATORS = "/v1/authenticators?type=VIR10&assigned=false&offset=0&limit=20";
    public static final String OSTID_API_VDP_ASSIGN_AUTHENTICATOR = "/v1/authenticators/%1$s/assign";
    public static final String OSTID_API_VDP_GET_USER = "/v1/users/%1$s@%2$s";
    public static final String OSTID_API_VDP_GET_AUTHENTICATOR = "/v1/authenticators?serialNumber=%1$s&domain=%2$s&type=VIR10&assigned=true&offset=0&limit=20";
    public static final String OSTID_API_VDP_GENERATE_VOTP = "/v1/authenticators/%1$s/applications/PASSWORD/generate-votp";

    //deprecated API Endpoints
    public static final String OSTID_API_USER_REGISTER = "/userregister/v1/";
    public static final String OSTID_API_USER_UNREGISTER = "/userunregister/v1/";
    public static final String OSTID_API_CRTONTO_RENDER = "/visualcode/v1/render?format=%1$s&message=%2$s";
    public static final String OSTID_API_EVENT_VALIDATION = "/eventvalidation/v2";
    public static final String OSTID_API_LOGIN = "/login/v2";
    public static final String OSTID_API_TRANSACTION = "/transaction/v2";
    public static final String OSTID_API_DEMO_COMMANDS = "/back/commands";

    //JSON payload
    public static final String OSTID_JSON_ADAPTIVE_USER_REGISTER = "{%6$s%7$s%8$s%9$s%10$s\"objectType\":\"%1$s\",\"userID\":\"%2$s\",\"clientIP\":\"%3$s\",\"cddc\":{\"browserCDDC\":{\"fingerprintHash\":\"%4$s\",\"fingerprintRaw\":%5$s}}}";
    public static final String OSTID_JSON_ADAPTIVE_APPLICATIONREF = "\"applicationRef\":\"%1$s\",";
    public static final String OSTID_JSON_ADAPTIVE_SESSIONID = "\"sessionID\":\"%1$s\",";
    public static final String OSTID_JSON_ADAPTIVE_REQUESTID = "\"requestID\":\"%1$s\",";
    public static final String OSTID_JSON_ADAPTIVE_ORCHESTRATIONDELIVERY = "\"orchestrationDelivery\":[%1$s],";
    public static final String OSTID_JSON_ADAPTIVE_TIMEOUT = "\"timeout\":%1$s,";
    public static final String OSTID_JSON_ADAPTIVE_CREDENTIALS_AUTHENTICATOR = "\"credentials\":{\"authenticator\":{\"OTP\":\"%1$s\"}},";
    public static final String OSTID_JSON_ADAPTIVE_CREDENTIALS_FIDOAUTHENTICATOR = "\"credentials\":{\"fidoAuthenticator\":{\"authenticationResponse\":\"%1$s\"}},";
    public static final String OSTID_JSON_ADAPTIVE_CREDENTIALS_FIDOAUTHENTICATOR_2 = "{\"fidoAuthentication\":{\"fidoProtocol\":\"%1$s\"},";
    public static final String OSTID_JSON_ADAPTIVE_CREDENTIALS_PASSKEY = "\"credentials\":{\"passKey\":\"%1$s\"},";
    public static final String OSTID_JSON_ADAPTIVE_USER_REGISTER_RELATIONSHIPREF = "\"relationshipRef\":\"%1$s\",";
    public static final String OSTID_JSON_ADAPTIVE_USER_REGISTER_ACTIVATIONTYPE = "\"activationType\":\"%1$s\",";
    public static final String OSTID_JSON_ADAPTIVE_ACTIVATE_DEVICE = "{\"signature\":\"%1$s\"}";
    public static final String OSTID_JSON_ADAPTIVE_ADD_DEVICE = "{\"deviceCode\":\"%1$s\"}";
    public static final String OSTID_JSON_ADAPTIVE_USER_LOGIN = "{%8$s%7$s%2$s%3$s%4$s%5$s%6$s\"objectType\":\"%1$s\"}";
    public static final String OSTID_JSON_ADAPTIVE_EVENT_VALIDATION = "{%8$s%7$s%2$s%3$s%4$s%5$s%6$s\"eventType\":\"%1$s\"}";
    public static final String OSTID_JSON_ADAPTIVE_SEND_TRANSACTION = "{%2$s%3$s%4$s%5$s\"objectType\":\"%1$s\"}";
    public static final String OSTID_JSON_ADAPTIVE_USER_LOGIN_IAA = "\"clientIP\":\"%1$s\",\"cddc\":{\"browserCDDC\":{\"fingerprintHash\":\"%2$s\",\"fingerprintRaw\":%3$s}},\"relationshipRef\":\"%4$s\",\"sessionID\":\"%5$s\",\"applicationRef\":\"%6$s\",";
    public static final String OSTID_JSON_ADAPTIVE_GENERATE_CHALLENGE = "{\"length\":\"%1$s\",\"checkDigit\":\"%2$s\"}";
    public static final String OSTID_JSON_ADAPTIVE_DATATOSIGN_FIDO = "\"data\":{\"fido\":{%3$s\"fidoProtocol\":\"%1$s\",\"authenticationResponse\":\"%2$s\"}},";
    public static final String OSTID_JSON_ADAPTIVE_DATATOSIGN_STANDARD = "\"data\":{\"standard\":{\"dataFields\":[%1$s],\"signature\":\"%2$s\"}},";
    public static final String OSTID_JSON_ADAPTIVE_DATATOSIGN_SECURECHANNEL = "\"data\":{\"secureChannel\":{\"requestID\":\"%1$s\",\"signature\":\"%2$s\"}},";
    public static final String OSTID_JSON_ADAPTIVE_DATATOSIGN_TRANSACTIONMESSAGE = "\"data\":{\"transactionMessage\":{\"dataFields\":[%1$s]}},";
    public static final String OSTID_JSON_ADAPTIVE_DATATOSIGN_TRANSACTIONMESSAGE_DATAFIELDS = "{\"key\":{\"text\":\"%1$s\"},\"value\":{\"text\":\"%2$s\"}}";
    public static final String OSTID_JSON_RISK_SEND_TRANSACTION =  "{%1$s\"clientIP\":\"%2$s\",\"cddc\":{\"browserCDDC\":{\"fingerprintHash\":\"%3$s\",\"fingerprintRaw\":%4$s}},\"relationshipRef\":\"%7$s\",\"sessionID\":\"%5$s\",\"applicationRef\":\"%6$s\"}";

    public static final String OSTID_JSON_VDP_USER_REGISTER = "{%1$s\"vdpDeliveryMethod\":\"%2$s\"}";
    public static final String OSTID_JSON_VDP_ASSIGN_AUTHENTICATOR = "{\"domain\":\"%1$s\",\"userID\":\"%2$s\"}";
    public static final String OSTID_JSON_VDP_GENERATE_VOTP = "{%1$s\"deliveryMethod\":\"%2$s\"}";

    
    //deprecated JSON payload
    public static final String OSTID_JSON_USER_REGISTER = "{%7$s%6$s\"login\":\"%1$s\",\"clientIP\":\"%2$s\",\"browserCDDC\":{\"fingerprintHash\":\"%3$s\",\"fingerprintRaw\":%4$s},\"sessionIdentifier\":\"%5$s\",\"applicationRef\":\"%8$s\"}";
    public static final String OSTID_JSON_CHECK_ACTIVATION = "{\"login\":\"%1$s\",\"timeoutSeconds\":\"%2$d\"}";
    public static final String OSTID_JSON_EVENT_VALIDATION = "{%10$s%1$s%2$s\"eventType\":\"%3$s\",\"login\":\"%4$s\",\"clientIP\":\"%5$s\",\"browserCDDC\":{\"fingerprintHash\":\"%6$s\",\"fingerprintRaw\":%7$s},\"sessionID\":\"%8$s\",\"digipassDomain\":\"%9$s\",\"dataToSign\":\"%4$s#%9$s\",\"applicationRef\":\"%11$s\"}";
    public static final String OSTID_JSON_LOGIN = "{%9$s%1$s%2$s\"login\":\"%3$s\",\"clientIP\":\"%4$s\",\"browserCDDC\":{\"fingerprintHash\":\"%5$s\",\"fingerprintRaw\":%6$s},\"sessionID\":\"%7$s\",\"digipassDomain\":\"%8$s\",\"dataToSign\":\"%3$s#%8$s\"}";
    public static final String OSTID_JSON_PASSKEY = "\"passKey\":\"%1$s\",";
    public static final String OSTID_JSON_ADAPTIVE_STATICPWD= "\"staticPassword\":\"%1$s\",";
    public static final String OSTID_JSON_ISNOTIFIED = "\"notificationsActivated\":%1$s,";
    public static final String OSTID_JSON_DEMO_COMMANDS= "{\"commandString\":\"%1$s\",\"irmResponse\":\"%2$s\",\"sessionId\":\"%3$s\"}";
    public static final String OSTID_JSON_TRANSACTIONS = "{%1$s%13$s%14$s\"accountRef\":\"%2$s\",\"amount\":\"%3$s\",\"currency\":\"%4$s\",\"transactionType\":\"%5$s\",\"creditorIBAN\":\"%6$s\",\"creditorName\":\"%7$s\",\"dataToSign\":[\"{\\\"amount\\\": \\\"%3$s\\\",\\\"beneficiary\\\": \\\"%7$s\\\",\\\"currency\\\": \\\"%4$s\\\",\\\"iban\\\": \\\"%6$s\\\"}\"],\"login\":\"%8$s\",\"sessionID\":\"%9$s\",\"browserCDDC\":{\"fingerprintRaw\":%10$s,\"fingerprintHash\":\"%11$s\"},\"clientIP\":\"%12$s\",\"applicationRef\":\"%15$s\"}";

    public static final Map<Integer, String> OSTID_API_CHALLANGE_MAP = ImmutableMap.<Integer, String>builder()
            .put(2, "Challenge")
            .put(3, "ChallengeSMS")
            .put(4, "ChallengeDevice")
            .put(5, "ChallengeDevice2FA")
            .put(8, "ChallengeEmail")
            .put(10, "ChallengeCRDevice2FA")
            .put(11, "ChallengeCronto")
            .put(21, "ChallengeNoPIN")
            .put(22, "ChallengePIN")
            .put(23, "ChallengeFingerprint")
            .put(24, "ChallengeFace")
            .put(25, "ChallengeFido")
            .build();

    public static final Map<EnvOptions,String> OSTID_ENV_MAP = ImmutableMap.<EnvOptions, String>builder()
            .put(EnvOptions.Sandbox, "sdb")
            .put(EnvOptions.Production_NA1, "prod.na1")
            .put(EnvOptions.Production_EU1, "prod.eu1")
            .put(EnvOptions.Production_EU2, "prod.eu2")
            .put(EnvOptions.Staging_NA1, "staging.na1")
            .put(EnvOptions.Staging_EU1, "staging.eu1")
            .put(EnvOptions.Staging_EU2, "staging.eu2")
            .put(EnvOptions.UAT_EU1, "uat.eu1")
            .build();
    
    
    /**
     * Attributes Keys in Shared/Transient State
     */
    public static final String OSTID_CRONTO_FORMULA = "%1$s;%2$s;001;%3$s;%4$s;%5$s";
    public static final String OSTID_RESPONSE_CHECK_ACTIVATION_STATUS = "activationStatus";
    public static final String OSTID_ERROR_MESSAGE = "ostid_error_message";

    public static final String OSTID_CDDC_JSON = "ostid_cddc_json";
    public static final String OSTID_CDDC_HASH = "ostid_cddc_hash";
    public static final String OSTID_CDDC_IP = "ostid_cddc_ip";
    public static final String OSTID_CDDC_HAS_PUSHED_JS = "ostid_cddc_has_pushed_js";
    public static final String OSTID_USERNAME_IN_SHARED_STATE = "ostid_username_in_shared_state";

    public static final String OSTID_DIGI_SERIAL = "ostid_digi_serial";
    public static final String OSTID_REGISTRATION_ID = "ostid_registration_id";
    public static final String OSTID_CRONTO = "ostid_cronto";
    public static final String OSTID_ACTIVATION_CODE = "ostid_activationPassword";
    public static final String OSTID_ACTIVATION_CODE2 = "activationPassword";
    public static final String OSTID_IRM_RESPONSE = "ostid_irm_response";
    public static final String OSTID_COMMAND = "ostid_command";
    public static final String OSTID_CRONTO_STATUS = "ostid_cronto_status";   //true:display, false:hide
    public static final String OSTID_CRONTO_HAS_RENDERED = "ostid_cronto_has_rendered";   //true:display, false:hide
    public static final String OSTID_RISK_RESPONSE_CODE = "ostid_risk_response_code";
    public static final String OSTID_RISK_RESPONSE_CODE2 = "riskResponseCode";
    public static final String OSTID_ACTIVATION_MESSAGE2 = "activationMessage2";

    public static final String OSTID_SESSIONID = "ostid_session_id";
    public static final String OSTID_DEVICE_CODE = "deviceCode";
    public static final String OSTID_SIGNATURE = "signature";
    public static final String OSTID_REQUEST_ID = "ostid_request_id";
    public static final String OSTID_EVENT_EXPIRY_DATE = "ostid_event_expiry_date";
    public static final String OSTID_CRONTO_MSG = "ostid_cronto_msg";
    public static final String OSTID_CRONTO_PUSH_JS = "ostid_cronto_push_js";

    public static final String OSTID_LOG_CORRELATION_ID = "log-correlation-id";

    /**
     * Default Values for OneSpan Auth Tree Nodes
     */
    public static final String OSTID_API_VERSION = "02";
    public static final String OSTID_DEFAULT_CRONTO_ALT = "OneSpan TID Cronto Image";
    public static final int OSTID_DEFAULT_CRONTO_HEIGHT = 210;
    public static final String OSTID_DEFAULT_USERNAME = "username";
    public static final String OSTID_DEFAULT_PASSKEY = "password";
    public static final String OSTID_DEFAULT_ACCOUNTREF = "accountRef";
    public static final String OSTID_DEFAULT_AMOUNT = "amount";
    public static final String OSTID_DEFAULT_CURRENCY = "currency";
    public static final String OSTID_DEFAULT_TRANSACTIONTYPE = "transactionType";
    public static final String OSTID_DEFAULT_CREDITORIBAN = "creditorIBAN";
    public static final String OSTID_DEFAULT_CREDITORNAME = "creditorName";
    public static final String OSTID_DEFAULT_CREDITORBANK = "creditorBank";
    public static final String OSTID_DEFAULT_DEBTORIBAN = "debtorIBAN";
    public static final String OSTID_STATIC_PASSWORD = "staticPassword";

    public static final int OSTID_DEFAULT_CHECK_ACTIVATION_TIMEOUT = 0;
    public static final int OSTID_DEFAULT_EVENT_EXPIRY = 60;
}
