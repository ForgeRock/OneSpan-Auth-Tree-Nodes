package com.os.tid.forgerock.openam.test;

import com.google.common.collect.ImmutableMap;
import com.iplanet.sso.SSOException;
import com.os.tid.forgerock.openam.config.Constants;
import com.os.tid.forgerock.openam.nodes.OSConfigurationsService;
import com.os.tid.forgerock.openam.nodes.OS_Auth_UserLoginNode;
import com.sun.identity.sm.SMSException;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext.Builder;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.sm.AnnotatedServiceRegistry;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.callback.Callback;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.mockito.BDDMockito.given;
import static org.mockito.MockitoAnnotations.initMocks;

@Test
public class OS_Auth_UserLoginNodeTest {

    @Mock
    private OS_Auth_UserLoginNode.Config config;

    @Mock
    protected OSConfigurationsService configurationsService;

    @Mock
    protected Realm realm;

    @Mock
    protected AnnotatedServiceRegistry annotatedServiceRegistry;

    @BeforeMethod
    public void before() throws SMSException, SSOException {
        initMocks(this);
        given(configurationsService.environment()).willReturn(TestData.ENVIRONMENT);
        given(configurationsService.applicationRef()).willReturn(TestData.APPLICATION_REF);

        given(annotatedServiceRegistry.getRealmSingleton(OSConfigurationsService.class, realm)).willReturn(Optional.of(configurationsService));
    }

    @Test
    public void testProcessMissingData() throws NodeProcessException{
        // Given
        //config
        given(config.userNameInSharedData()).willReturn(Constants.OSTID_DEFAULT_USERNAME);
        given(config.passwordInTransientState()).willReturn(Constants.OSTID_DEFAULT_PASSKEY);
        given(config.orchestrationDelivery()).willReturn(OS_Auth_UserLoginNode.OrchestrationDelivery.none);
        given(config.visualCodeMessageOptions()).willReturn(OS_Auth_UserLoginNode.VisualCodeMessageOptions.sessionID);
        given(config.objectType()).willReturn(OS_Auth_UserLoginNode.ObjectType.AdaptiveLoginInput);

        OS_Auth_UserLoginNode node = new OS_Auth_UserLoginNode(config, realm, annotatedServiceRegistry);

        //tree context
        JsonValue sharedState = json(object(1));
        JsonValue transientState = json(object(1));
        TreeContext context = getContext(sharedState,transientState,Collections.emptyList());

        // When
        Action result = node.process(context);
        // Then
        assertThat(result.outcome).isEqualTo("Error");
        assertThat(result.callbacks.isEmpty());
        assertThat(result.sharedState.keys()).contains(Constants.OSTID_ERROR_MESSAGE);
    }

    @Test
    public Action testProcessSuccess() throws NodeProcessException{
        // Given
        //config
        given(config.userNameInSharedData()).willReturn(Constants.OSTID_DEFAULT_USERNAME);
//        given(config.passwordInTransientState()).willReturn(Constants.OSTID_DEFAULT_PASSKEY);
        given(config.orchestrationDelivery()).willReturn(OS_Auth_UserLoginNode.OrchestrationDelivery.none);
        given(config.visualCodeMessageOptions()).willReturn(OS_Auth_UserLoginNode.VisualCodeMessageOptions.sessionID);
        given(config.objectType()).willReturn(OS_Auth_UserLoginNode.ObjectType.AdaptiveLoginInput);
        given(config.credentialsType()).willReturn(OS_Auth_UserLoginNode.CredentialsType.none);
        given(config.timeout()).willReturn(Constants.OSTID_DEFAULT_EVENT_EXPIRY);

        OS_Auth_UserLoginNode node = new OS_Auth_UserLoginNode(config, realm, annotatedServiceRegistry);

        //tree context
        JsonValue sharedState = json(object(1));
        sharedState.put(Constants.OSTID_DEFAULT_USERNAME,TestData.TEST_USERNAME);
        sharedState.put(Constants.OSTID_CDDC_JSON,TestData.TEST_CDDC_JSON);
        sharedState.put(Constants.OSTID_CDDC_HASH,TestData.TEST_CDDC_HASH);
        sharedState.put(Constants.OSTID_CDDC_IP,TestData.TEST_CDDC_IP);
        JsonValue transientState = json(object(1));
//        transientState.put(Constants.OSTID_DEFAULT_PASSKEY,TestData.TEST_PASS_KEY);
        TreeContext context = getContext(sharedState,transientState,Collections.emptyList());
        // When
        Action result = node.process(context);
        // Then
        assertThat(result.outcome).isEqualTo("StepUp");
        assertThat(result.callbacks.isEmpty());
        assertThat(result.sharedState.keys()).contains(Constants.OSTID_EVENT_EXPIRY_DATE);
        assertThat(result.sharedState.keys()).contains(Constants.OSTID_SESSIONID);
        assertThat(result.sharedState.keys()).contains(Constants.OSTID_REQUEST_ID);
        assertThat(result.sharedState.keys()).contains(Constants.OSTID_IRM_RESPONSE);
        assertThat(result.sharedState.keys()).contains(Constants.OSTID_COMMAND);
        assertThat(result.sharedState.keys()).contains(Constants.OSTID_CRONTO_MSG);

        return result;
    }

    @Test
    public void testProcessWithOptionalAttributesSuccess() throws NodeProcessException{
        // Given
        //config
        given(config.userNameInSharedData()).willReturn(Constants.OSTID_DEFAULT_USERNAME);
        given(config.passwordInTransientState()).willReturn(Constants.OSTID_DEFAULT_PASSKEY);
        given(config.credentialsType()).willReturn(OS_Auth_UserLoginNode.CredentialsType.passKey);
        given(config.orchestrationDelivery()).willReturn(OS_Auth_UserLoginNode.OrchestrationDelivery.none);
        given(config.visualCodeMessageOptions()).willReturn(OS_Auth_UserLoginNode.VisualCodeMessageOptions.sessionID);
        given(config.objectType()).willReturn(OS_Auth_UserLoginNode.ObjectType.AdaptiveLoginInput);
        given(config.optionalAttributes()).willReturn(ImmutableMap.of(
                "mobilePhoneNumber","mobile_phone_number",
                "emailAddress","email_address"
        ));

        OS_Auth_UserLoginNode node = new OS_Auth_UserLoginNode(config, realm, annotatedServiceRegistry);

        //tree context
        JsonValue sharedState = json(object(1));
        sharedState.put(Constants.OSTID_DEFAULT_USERNAME,TestData.TEST_USERNAME);
        sharedState.put(Constants.OSTID_CDDC_JSON,TestData.TEST_CDDC_JSON);
        sharedState.put(Constants.OSTID_CDDC_HASH,TestData.TEST_CDDC_HASH);
        sharedState.put(Constants.OSTID_CDDC_IP,TestData.TEST_CDDC_IP);
        sharedState.put("mobile_phone_number",TestData.TEST_MOBILE_PHONE);
        sharedState.put("email_address",TestData.TEST_EMAIL_ADDRESS);
        JsonValue transientState = json(object(1));
        transientState.put(Constants.OSTID_DEFAULT_PASSKEY,TestData.TEST_PASS_KEY);
        TreeContext context = getContext(sharedState,transientState,Collections.emptyList());

        // When
        Action result = node.process(context);
        // Then
        assertThat(result.outcome).isEqualTo("StepUp");
        assertThat(result.callbacks.isEmpty());
        assertThat(result.sharedState.keys()).contains(Constants.OSTID_EVENT_EXPIRY_DATE);
        assertThat(result.sharedState.keys()).contains(Constants.OSTID_SESSIONID);
        assertThat(result.sharedState.keys()).contains(Constants.OSTID_REQUEST_ID);
        assertThat(result.sharedState.keys()).contains(Constants.OSTID_IRM_RESPONSE);
        assertThat(result.sharedState.keys()).contains(Constants.OSTID_COMMAND);
        assertThat(result.sharedState.keys()).contains(Constants.OSTID_CRONTO_MSG);
    }

    private TreeContext getContext(JsonValue sharedState, JsonValue transientState, List<Callback> callbackList) {
        return new TreeContext("managed/user", sharedState, transientState, new Builder().build(), callbackList,null);
    }
}
