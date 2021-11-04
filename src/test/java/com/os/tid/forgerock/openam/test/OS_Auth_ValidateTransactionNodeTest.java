package com.os.tid.forgerock.openam.test;

import com.google.common.collect.ImmutableMap;
import com.iplanet.sso.SSOException;
import com.os.tid.forgerock.openam.config.Constants;
import com.os.tid.forgerock.openam.nodes.OSConfigurationsService;
import com.os.tid.forgerock.openam.nodes.OS_Auth_ValidateTransactionNode;
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
public class OS_Auth_ValidateTransactionNodeTest {
    @Mock
    private OS_Auth_ValidateTransactionNode.Config config;

    @Mock
    private OSConfigurationsService configurationsService;

    @Mock
    private Realm realm;

    @Mock
    private AnnotatedServiceRegistry annotatedServiceRegistry;

    @BeforeMethod
    public void before() throws SMSException, SSOException {
        initMocks(this);
        given(configurationsService.tenantNameToLowerCase()).willReturn(TestData.TENANT_NAME.toLowerCase());
        given(configurationsService.environment()).willReturn(TestData.ENVIRONMENT);
        given(configurationsService.applicationRef()).willReturn(TestData.APPLICATION_REF);

        given(annotatedServiceRegistry.getRealmSingleton(OSConfigurationsService.class, realm)).willReturn(Optional.of(configurationsService));
    }

    @Test
    public void testProcessMissingData() throws NodeProcessException{
        // Given
        //config
        given(config.userNameInSharedData()).willReturn(Constants.OSTID_DEFAULT_USERNAME);
        given(config.objectType()).willReturn(OS_Auth_ValidateTransactionNode.ObjectType.AdaptiveTransactionValidationInput);
        given(config.visualCodeMessageOptions()).willReturn(OS_Auth_ValidateTransactionNode.VisualCodeMessageOptions.sessionID);
        given(config.dataToSign()).willReturn(OS_Auth_ValidateTransactionNode.DataToSign.transactionMessage);
        given(config.orchestrationDelivery()).willReturn(OS_Auth_ValidateTransactionNode.OrchestrationDelivery.none);
        given(config.adaptiveAttributes()).willReturn(ImmutableMap.<String, String>builder()
                .put("accountRef", "accountRef")
                .put("amount", "amount")
                .put("currency", "currency")
                .put("transactionType", "transactionType")
                .put("creditorBank", "creditorBank")
                .put("creditorIBAN", "creditorIBAN")
                .put("creditorName", "creditorName")
                .put("debtorIBAN", "debtorIBAN")
                .build());
        OS_Auth_ValidateTransactionNode node = new OS_Auth_ValidateTransactionNode(config, realm, annotatedServiceRegistry);

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
        given(config.userNameInSharedData()).willReturn(Constants.OSTID_DEFAULT_USERNAME);
        given(config.objectType()).willReturn(OS_Auth_ValidateTransactionNode.ObjectType.AdaptiveTransactionValidationInput);
        given(config.visualCodeMessageOptions()).willReturn(OS_Auth_ValidateTransactionNode.VisualCodeMessageOptions.sessionID);
        given(config.dataToSign()).willReturn(OS_Auth_ValidateTransactionNode.DataToSign.transactionMessage);
        given(config.orchestrationDelivery()).willReturn(OS_Auth_ValidateTransactionNode.OrchestrationDelivery.none);
        given(config.adaptiveAttributes()).willReturn(ImmutableMap.<String, String>builder()
                .put("accountRef", "accountRef")
                .put("amount", "amount")
                .put("currency", "currency")
                .put("transactionType", "transactionType")
                .put("creditorBank", "creditorBank")
                .put("creditorIBAN", "creditorIBAN")
                .put("creditorName", "creditorName")
                .put("debtorIBAN", "debtorIBAN")
                .build());

        OS_Auth_ValidateTransactionNode node = new OS_Auth_ValidateTransactionNode(config, realm, annotatedServiceRegistry);

        //tree context
        JsonValue sharedState = json(object(1));
        /**
         *                     .put("accountRef", "accountRef")
         *                     .put("amount", "amount")
         *                     .put("currency", "currency")
         *                     .put("transactionType", "transactionType")
         *                     .put("creditorBank", "creditorBank")
         *                     .put("creditorIBAN", "creditorIBAN")
         *                     .put("creditorName", "creditorName")
         *                     .put("debtorIBAN", "debtorIBAN")
         */
        sharedState.put(Constants.OSTID_DEFAULT_USERNAME,TestData.TEST_USERNAME);
        sharedState.put(Constants.OSTID_CDDC_JSON,TestData.TEST_CDDC_JSON);
        sharedState.put(Constants.OSTID_CDDC_HASH,TestData.TEST_CDDC_HASH);
        sharedState.put(Constants.OSTID_CDDC_IP,TestData.TEST_CDDC_IP);
        sharedState.put(Constants.OSTID_DEFAULT_TRANSACTIONTYPE,"ExternalTransfer");
        sharedState.put(Constants.OSTID_DEFAULT_CURRENCY,"CAD");
        sharedState.put(Constants.OSTID_DEFAULT_AMOUNT,"66.66");
        sharedState.put(Constants.OSTID_DEFAULT_CREDITORIBAN,"IBAN123123");
        sharedState.put(Constants.OSTID_DEFAULT_DEBTORIBAN,"IBAN234234");
        sharedState.put(Constants.OSTID_DEFAULT_CREDITORBANK,"ABC Bank");
        sharedState.put(Constants.OSTID_DEFAULT_ACCOUNTREF,"Ref123123");
        sharedState.put(Constants.OSTID_DEFAULT_CREDITORNAME,"John Smith");
        TreeContext context = getContext(sharedState,json(object(1)),Collections.emptyList());

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

    @Test
    public void testProcessWithOptionalAttributesSuccess() throws NodeProcessException{
        // Given
        //config
        given(config.userNameInSharedData()).willReturn(Constants.OSTID_DEFAULT_USERNAME);
        given(config.objectType()).willReturn(OS_Auth_ValidateTransactionNode.ObjectType.AdaptiveTransactionValidationInput);
        given(config.visualCodeMessageOptions()).willReturn(OS_Auth_ValidateTransactionNode.VisualCodeMessageOptions.sessionID);
        given(config.dataToSign()).willReturn(OS_Auth_ValidateTransactionNode.DataToSign.transactionMessage);
        given(config.optionalAttributes()).willReturn(ImmutableMap.of(
                "mobilePhoneNumber","mobile_phone_number",
                "emailAddress","email_address"
        ));
        given(config.orchestrationDelivery()).willReturn(OS_Auth_ValidateTransactionNode.OrchestrationDelivery.none);
        given(config.adaptiveAttributes()).willReturn(ImmutableMap.<String, String>builder()
                .put("accountRef", "accountRef")
                .put("amount", "amount")
                .put("currency", "currency")
                .put("transactionType", "transactionType")
                .put("creditorBank", "creditorBank")
                .put("creditorIBAN", "creditorIBAN")
                .put("creditorName", "creditorName")
                .put("debtorIBAN", "debtorIBAN")
                .build());

        OS_Auth_ValidateTransactionNode node = new OS_Auth_ValidateTransactionNode(config, realm, annotatedServiceRegistry);

        //tree context
        JsonValue sharedState = json(object(1));
        sharedState.put(Constants.OSTID_DEFAULT_USERNAME,TestData.TEST_USERNAME);
        sharedState.put(Constants.OSTID_CDDC_JSON,TestData.TEST_CDDC_JSON);
        sharedState.put(Constants.OSTID_CDDC_HASH,TestData.TEST_CDDC_HASH);
        sharedState.put(Constants.OSTID_CDDC_IP,TestData.TEST_CDDC_IP);
        sharedState.put(Constants.OSTID_DEFAULT_TRANSACTIONTYPE,"ExternalTransfer");
        sharedState.put(Constants.OSTID_DEFAULT_CURRENCY,"CAD");
        sharedState.put(Constants.OSTID_DEFAULT_AMOUNT,"66.66");
        sharedState.put(Constants.OSTID_DEFAULT_CREDITORIBAN,"IBAN123123");
        sharedState.put(Constants.OSTID_DEFAULT_DEBTORIBAN,"IBAN234234");
        sharedState.put(Constants.OSTID_DEFAULT_CREDITORBANK,"ABC Bank");
        sharedState.put(Constants.OSTID_DEFAULT_ACCOUNTREF,"Ref123123");
        sharedState.put(Constants.OSTID_DEFAULT_CREDITORNAME,"John Smith");
        sharedState.put("mobile_phone_number",TestData.TEST_MOBILE_PHONE);
        sharedState.put("email_address",TestData.TEST_EMAIL_ADDRESS);
        TreeContext context = getContext(sharedState,json(object(1)),Collections.emptyList());

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
