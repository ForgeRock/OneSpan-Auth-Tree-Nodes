package com.os.tid.forgerock.openam.test;

import com.iplanet.sso.SSOException;
import com.os.tid.forgerock.openam.config.Constants;
import com.os.tid.forgerock.openam.nodes.OSConfigurationsService;
import com.os.tid.forgerock.openam.nodes.OS_Sample_StoreCommandNode;
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
public class OS_Sample_StoreCommandNodeTest {
    @Mock
    protected OSConfigurationsService configurationsService;

    @Mock
    protected Realm realm;

    @Mock
    protected AnnotatedServiceRegistry annotatedServiceRegistry;

    @Mock
    private OS_Sample_StoreCommandNode.Config config;

    @BeforeMethod
    public void before() throws SMSException, SSOException {
        initMocks(this);
        given(configurationsService.environment()).willReturn(TestData.ENVIRONMENT);
        given(configurationsService.applicationRef()).willReturn(TestData.APPLICATION_REF);

        given(annotatedServiceRegistry.getRealmSingleton(OSConfigurationsService.class, realm)).willReturn(Optional.of(configurationsService));
        given(config.javascript()).willReturn(TestData.TEST_COMMAND_STORAGE_URL);
    }

    @Test
    public void testProcessMissingData() throws NodeProcessException{
        // Given
        OS_Sample_StoreCommandNode node = new OS_Sample_StoreCommandNode(config,realm, annotatedServiceRegistry);

        //tree context
        TreeContext context = getContext(json(object(1)),json(object(1)),Collections.emptyList());

        // When
        Action result = node.process(context);
        // Then
        assertThat(result.outcome).isEqualTo("Error");
        assertThat(result.callbacks.isEmpty());
        assertThat(result.sharedState.keys()).contains(Constants.OSTID_ERROR_MESSAGE);
    }


    /**
     *             Map<String, String> placeholders = new HashMap<String, String>() {{
     *                 put("tenantName", tenantName);
     *                 put("sessionIdentifier", StringUtils.hexToString(ostid_sessionid.asString()));
     *                 put("sessionID", ostid_sessionid.asString());
     *                 put("requestID", requestId);
     *                 put("username", sharedState.get(Constants.OSTID_DEFAULT_USERNAME).isString() ? sharedState.get(Constants.OSTID_DEFAULT_USERNAME).asString() : "username");
     *                 put("hexRequestID", StringUtils.stringToHex(requestId));
     *             }};
     */
    @Test
    public void testProcessSuccess() throws NodeProcessException{
        // Given
        OS_Sample_StoreCommandNode node = new OS_Sample_StoreCommandNode(config,realm, annotatedServiceRegistry);

        //tree context
        JsonValue sharedState = json(object(1));
        sharedState.put(Constants.OSTID_SESSIONID,TestData.TEST_SESSION_ID);
        sharedState.put(Constants.OSTID_REQUEST_ID,"test_request_id");
        sharedState.put(Constants.OSTID_IRM_RESPONSE,0);
        sharedState.put(Constants.OSTID_COMMAND,"test_command");

        TreeContext context = getContext(sharedState,json(object(1)),Collections.emptyList());

        // When
        Action result = node.process(context);
        // Then
        assertThat(result.outcome).isEqualTo("Success");
        assertThat(result.callbacks.isEmpty());
    }


    private TreeContext getContext(JsonValue sharedState, JsonValue transientState, List<Callback> callbackList) {
        return new TreeContext("managed/user", sharedState, transientState, new Builder().build(), callbackList,null);
    }



}
