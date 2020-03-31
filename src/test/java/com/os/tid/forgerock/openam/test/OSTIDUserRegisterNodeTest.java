package com.os.tid.forgerock.openam.test;

import com.google.common.collect.ImmutableMap;
import com.iplanet.sso.SSOException;
import com.os.tid.forgerock.openam.config.Constants;
import com.os.tid.forgerock.openam.nodes.OSTIDConfigurationsService;
import com.os.tid.forgerock.openam.nodes.OSTIDUserRegisterNode;
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
public class OSTIDUserRegisterNodeTest {

    @Mock
    private OSTIDUserRegisterNode.Config config;

    @Mock
    protected OSTIDConfigurationsService configurationsService;

    @Mock
    protected Realm realm;

    @Mock
    protected AnnotatedServiceRegistry annotatedServiceRegistry;

    @BeforeMethod
    public void before() throws SMSException, SSOException {
        initMocks(this);
        given(configurationsService.tenantNameToLowerCase()).willReturn(TestData.TENANT_NAME.toLowerCase());
        given(configurationsService.environment()).willReturn(TestData.ENVIRONMENT);
        given(annotatedServiceRegistry.getRealmSingleton(OSTIDConfigurationsService.class, realm)).willReturn(Optional.of(configurationsService));
    }

    @Test
    public void testProcessMissingData() throws NodeProcessException{
        // Given
        //config
        given(config.nodeFunction()).willReturn(OSTIDUserRegisterNode.NodeFunction.UserRegister);
        given(config.userNameInSharedData()).willReturn(Constants.OSTID_DEFAULT_USERNAME);
        given(config.passwordInTransientState()).willReturn(Constants.OSTID_DEFAULT_PASSKEY);

        OSTIDUserRegisterNode node = new OSTIDUserRegisterNode(config, realm, annotatedServiceRegistry);

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
    public void testProcessSuccess() throws NodeProcessException{
        // Given
        //config
        given(config.nodeFunction()).willReturn(OSTIDUserRegisterNode.NodeFunction.UserRegister);
        given(config.userNameInSharedData()).willReturn(Constants.OSTID_DEFAULT_USERNAME);
        given(config.passwordInTransientState()).willReturn(Constants.OSTID_DEFAULT_PASSKEY);

        OSTIDUserRegisterNode node = new OSTIDUserRegisterNode(config, realm, annotatedServiceRegistry);

        //tree context
        JsonValue sharedState = json(object(1));
        sharedState.put(Constants.OSTID_DEFAULT_USERNAME,TestData.TEST_USERNAME);
        sharedState.put(Constants.OSTID_CDDC_JSON,TestData.TEST_CDDC_JSON);
        sharedState.put(Constants.OSTID_CDDC_HASH,TestData.TEST_CDDC_HASH);
        sharedState.put(Constants.OSTID_CDDC_IP,TestData.TEST_CDDC_IP);
        JsonValue transientState = json(object(1));
        transientState.put(Constants.OSTID_DEFAULT_PASSKEY,TestData.TEST_PASS_KEY);
        TreeContext context = getContext(sharedState,transientState,Collections.emptyList());

        // When
        Action result = node.process(context);
        // Then
        assertThat(result.outcome).isEqualTo("Success");
        assertThat(result.callbacks.isEmpty());
        assertThat(result.sharedState.keys()).contains(Constants.OSTID_SESSIONID);
        assertThat(result.sharedState.keys()).contains(Constants.OSTID_ACTIVATION_CODE);
        assertThat(result.sharedState.keys()).contains(Constants.OSTID_CRONTO_MSG);
        assertThat(result.sharedState.keys()).contains(Constants.OSTID_DIGI_SERIAL);
        assertThat(result.sharedState.keys()).contains(Constants.OSTID_EVENT_EXPIRY_DATE);
    }

    @Test
    public void testProcessWithOptionalAttributesSuccess() throws NodeProcessException{
        // Given
        //config
        given(config.nodeFunction()).willReturn(OSTIDUserRegisterNode.NodeFunction.UserRegister);
        given(config.userNameInSharedData()).willReturn(Constants.OSTID_DEFAULT_USERNAME);
        given(config.passwordInTransientState()).willReturn(Constants.OSTID_DEFAULT_PASSKEY);
        given(config.optionalAttributes()).willReturn(ImmutableMap.of(
                "mobile_phone_number","mobilePhoneNumber",
                "email_address","emailAddress"
        ));


        OSTIDUserRegisterNode node = new OSTIDUserRegisterNode(config, realm, annotatedServiceRegistry);

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
        assertThat(result.outcome).isEqualTo("Success");
        assertThat(result.callbacks.isEmpty());
        assertThat(result.sharedState.keys()).contains(Constants.OSTID_SESSIONID);
        assertThat(result.sharedState.keys()).contains(Constants.OSTID_ACTIVATION_CODE);
        assertThat(result.sharedState.keys()).contains(Constants.OSTID_CRONTO_MSG);
        assertThat(result.sharedState.keys()).contains(Constants.OSTID_DIGI_SERIAL);
        assertThat(result.sharedState.keys()).contains(Constants.OSTID_EVENT_EXPIRY_DATE);
    }

    private TreeContext getContext(JsonValue sharedState, JsonValue transientState, List<Callback> callbackList) {
        return new TreeContext(sharedState, transientState, new Builder().build(), callbackList);
    }



}
