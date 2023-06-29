package com.os.tid.forgerock.openam.test;

import com.os.tid.forgerock.openam.nodes.OSConfigurationsService;
import com.os.tid.forgerock.openam.utils.StringUtils;

public class TestData {
    private TestData() {
    }

    public static final String APPLICATION_REF = "ForgeRock Identity Cloud";
    public static final String TENANT_NAME = "tylergelinas-forgero";
    public static final OSConfigurationsService.EnvOptions ENVIRONMENT = OSConfigurationsService.EnvOptions.sdb;
    public static final String TEST_USERNAME = "tyler4";
    public static final String TEST_PASS_KEY = "Test1234";
    public static final String TEST_CDDC_JSON = "{\"browser\":{\"userAgent\":\"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.130 Safari/537.36\"},\"viewport\":{\"width\":1903,\"height\":969},\"support\":{\"ajax\":true,\"boxModel\":null,\"changeBubbles\":true,\"checkClone\":true,\"checkOn\":true,\"cors\":true,\"cssFloat\":true,\"hrefNormalized\":true,\"htmlSerialize\":true,\"leadingWhitespace\":true,\"noCloneChecked\":true,\"noCloneEvent\":true,\"opacity\":true,\"optDisabled\":true,\"style\":true,\"submitBubbles\":true,\"tbody\":true},\"computer\":{\"screenWidth\":1920,\"screenHeight\":1080,\"OS\":\"Microsoft Windows\",\"platform\":\"Win32\"},\"additional\":{}}";
    public static final String TEST_CDDC_HASH = "e96dadc9651f5fe8f071110eb174fe8e7a17a9d7a96b3b1980c13e5b4af3a4d7";
    public static final String TEST_CDDC_IP = "192.168.1.1";
    public static final String TEST_MOBILE_PHONE = "+15145718888";
    public static final String TEST_EMAIL_ADDRESS = "test_ostid_user1@mailinator.com";
    public static final String TEST_COMMAND_STORAGE_URL = "https://osgs-firestore-to-rest.herokuapp.com/{tenantName}/{sessionID}/commands";
    public static final String TEST_SESSION_ID = "38663233383530352d623966382d343631342d393762392d373833303463323962303836";
    public static final String TEST_CRONTO_MSG = StringUtils.stringToHex("02;user11021;111;hanyuchen9102-gmail;3zE6RNH5;hanyuchen9102-gmail");

}
