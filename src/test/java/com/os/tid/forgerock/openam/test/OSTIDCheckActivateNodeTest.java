package com.os.tid.forgerock.openam.test;

import com.google.common.collect.ImmutableMap;
import com.iplanet.sso.SSOException;
import com.os.tid.forgerock.openam.config.Constants;
import com.os.tid.forgerock.openam.nodes.OSTIDCheckActivateNode;
import com.os.tid.forgerock.openam.nodes.OSTIDConfigurationsService;
import com.os.tid.forgerock.openam.nodes.OSTIDLoginNode;
import com.os.tid.forgerock.openam.utils.DateUtils;
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
public class OSTIDCheckActivateNodeTest extends OSTIDUserRegisterNodeTest{
    @BeforeMethod
    public void before() throws SMSException, SSOException {
        super.before();
    }

    @Test
    public void testCheckActivateProcessMissingData() throws NodeProcessException{
        // Given
        OSTIDCheckActivateNode node = new OSTIDCheckActivateNode(realm, annotatedServiceRegistry);

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
        OSTIDCheckActivateNode node = new OSTIDCheckActivateNode(realm, annotatedServiceRegistry);

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
        OSTIDCheckActivateNode node = new OSTIDCheckActivateNode(realm, annotatedServiceRegistry);

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
        OSTIDCheckActivateNode node = new OSTIDCheckActivateNode(realm, annotatedServiceRegistry);

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
        return new TreeContext(sharedState, transientState, new Builder().build(), callbackList);
    }
}
