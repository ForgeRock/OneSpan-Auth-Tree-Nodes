package com.os.tid.forgerock.openam.test;

import com.iplanet.sso.SSOException;
import com.os.tid.forgerock.openam.config.Constants;
import com.os.tid.forgerock.openam.nodes.OS_Auth_CheckActivationNode;
import com.os.tid.forgerock.openam.utils.DateUtils;
import com.sun.identity.sm.SMSException;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext.Builder;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.callback.Callback;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

@Test
public class OS_Auth_CheckActivationNodeTest extends OS_Auth_UserRegisterNodeTest {
    @BeforeMethod
    public void before() throws SMSException, SSOException {
        super.before();
    }

    @Test
    public void testCheckActivateProcessMissingData() throws NodeProcessException{
        // Given
        OS_Auth_CheckActivationNode node = new OS_Auth_CheckActivationNode(realm, annotatedServiceRegistry);

        //tree context
        JsonValue sharedState = json(object(1));
        sharedState.put(Constants.OSTID_USERNAME_IN_SHARED_STATE,Constants.OSTID_DEFAULT_USERNAME);
        sharedState.put(Constants.OSTID_EVENT_EXPIRY_DATE, DateUtils.getMilliStringAfterCertainSecs(Constants.OSTID_DEFAULT_EVENT_EXPIRY * 1000));

        JsonValue transientState = json(object(1));
        TreeContext context = getContext(sharedState,transientState,Collections.emptyList());

        // When
        Action result = node.process(context);
        // Then
        assertThat(result.outcome).isEqualTo("error");
        assertThat(result.callbacks.isEmpty());
        assertThat(result.sharedState.keys()).contains(Constants.OSTID_ERROR_MESSAGE);
    }

    @Test
    public void testCheckActivateProcessEventExpired() throws NodeProcessException{
        // Given
        OS_Auth_CheckActivationNode node = new OS_Auth_CheckActivationNode(realm, annotatedServiceRegistry);

        //tree context
        JsonValue sharedState = json(object(1));
        sharedState.put(Constants.OSTID_USERNAME_IN_SHARED_STATE,Constants.OSTID_DEFAULT_USERNAME);
        sharedState.put(Constants.OSTID_DEFAULT_USERNAME,TestData.TEST_USERNAME);
        sharedState.put(Constants.OSTID_EVENT_EXPIRY_DATE, DateUtils.getMilliStringAfterCertainSecs(-10 * 1000));

        JsonValue transientState = json(object(1));
        TreeContext context = getContext(sharedState,transientState,Collections.emptyList());

        // When
        Action result = node.process(context);
        // Then
        assertThat(result.outcome).isEqualTo("timeout");
        assertThat(result.callbacks.isEmpty());
        assertThat(result.sharedState.keys()).contains(Constants.OSTID_ERROR_MESSAGE);
    }

    @Test
    public void testCheckActivateProcessUnknownStatus() throws NodeProcessException{
        // Given
        OS_Auth_CheckActivationNode node = new OS_Auth_CheckActivationNode(realm, annotatedServiceRegistry);

        //tree context
        JsonValue sharedState = json(object(1));
        sharedState.put(Constants.OSTID_USERNAME_IN_SHARED_STATE,Constants.OSTID_DEFAULT_USERNAME);
        sharedState.put(Constants.OSTID_DEFAULT_USERNAME,"unknown_username");
        sharedState.put(Constants.OSTID_EVENT_EXPIRY_DATE, DateUtils.getMilliStringAfterCertainSecs(Constants.OSTID_DEFAULT_EVENT_EXPIRY * 1000));

        JsonValue transientState = json(object(1));
        TreeContext context = getContext(sharedState,transientState,Collections.emptyList());

        // When
        Action result = node.process(context);
        // Then
        assertThat(result.outcome).isEqualTo("unknown");
        assertThat(result.callbacks.isEmpty());
        assertThat(result.sharedState.keys()).contains(Constants.OSTID_ERROR_MESSAGE);
    }

    @Test
    public void testCheckActivateProcessPending() throws NodeProcessException{
        //step1: register a user
        testProcessSuccess();

        // Given
        OS_Auth_CheckActivationNode node = new OS_Auth_CheckActivationNode(realm, annotatedServiceRegistry);

        //tree context
        JsonValue sharedState = json(object(1));
        sharedState.put(Constants.OSTID_USERNAME_IN_SHARED_STATE,Constants.OSTID_DEFAULT_USERNAME);
        sharedState.put(Constants.OSTID_DEFAULT_USERNAME,TestData.TEST_USERNAME);
        sharedState.put(Constants.OSTID_EVENT_EXPIRY_DATE, DateUtils.getMilliStringAfterCertainSecs(Constants.OSTID_DEFAULT_EVENT_EXPIRY * 1000));

        TreeContext context = getContext(sharedState,json(object(1)),Collections.emptyList());

        // When
        Action result = node.process(context);
        // Then
        assertThat(result.outcome).isEqualTo("pending");
        assertThat(result.callbacks.isEmpty());
    }


    private TreeContext getContext(JsonValue sharedState, JsonValue transientState, List<Callback> callbackList) {
        return new TreeContext("managed/user", sharedState, transientState, new Builder().build(), callbackList,null);
    }
}
