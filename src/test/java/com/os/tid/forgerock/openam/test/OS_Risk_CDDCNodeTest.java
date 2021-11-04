package com.os.tid.forgerock.openam.test;

import com.google.common.collect.ImmutableList;
import com.os.tid.forgerock.openam.config.Constants;
import com.os.tid.forgerock.openam.nodes.OS_Risk_CDDCNode;
import com.sun.identity.authentication.callbacks.HiddenValueCallback;
import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext.Builder;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.callback.Callback;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.mockito.BDDMockito.given;
import static org.mockito.MockitoAnnotations.initMocks;

@Test
public class OS_Risk_CDDCNodeTest {
    @Mock
    private OS_Risk_CDDCNode.Config config;

    @BeforeMethod
    public void before() {
        initMocks(this);
    }

    @Test
    public void testProcessWithNoCallbacks() throws NodeProcessException {
        // Given
        given(config.pushCDDCJsAsCallback()).willReturn(true);
        OS_Risk_CDDCNode node = new OS_Risk_CDDCNode(config);
        TreeContext context = getContext(json(object(1)),json(object(1)),Collections.emptyList());

        // When
        Action result = node.process(context);

        // Then
        assertThat(result.outcome).isEqualTo(null);
        assertThat(result.callbacks).hasSize(6);
        assertThat(result.callbacks.get(0)).isInstanceOf(HiddenValueCallback.class);
        assertThat(result.callbacks.get(1)).isInstanceOf(HiddenValueCallback.class);
        assertThat(result.callbacks.get(2)).isInstanceOf(ScriptTextOutputCallback.class);
        assertThat(result.callbacks.get(3)).isInstanceOf(ScriptTextOutputCallback.class);
        assertThat(result.callbacks.get(4)).isInstanceOf(ScriptTextOutputCallback.class);
        assertThat(result.callbacks.get(5)).isInstanceOf(ScriptTextOutputCallback.class);

    }

    @Test
    public void testProcessWithCallbacks() throws NodeProcessException {
        // Given
        OS_Risk_CDDCNode node = new OS_Risk_CDDCNode(config);
        given(config.pushCDDCJsAsCallback()).willReturn(true);
        TreeContext context = getContext(json(object(1)), json(object(1)), ImmutableList.of(
                new HiddenValueCallback(Constants.OSTID_CDDC_JSON, TestData.TEST_CDDC_JSON),
                new HiddenValueCallback(Constants.OSTID_CDDC_HASH, TestData.TEST_CDDC_HASH)
        ));

        // When
        Action result = node.process(context);

        // Then
        assertThat(result.outcome).isEqualTo("outcome");
        assertThat(result.callbacks.isEmpty());
        assertThat(result.sharedState.keys()).contains(Constants.OSTID_CDDC_JSON);
        assertThat(result.sharedState.keys()).contains(Constants.OSTID_CDDC_HASH);
        assertThat(result.sharedState.keys()).contains(Constants.OSTID_CDDC_IP);
    }

    private TreeContext getContext(JsonValue sharedState, JsonValue transientState, List<Callback> callbackList) {
        return new TreeContext("managed/user", sharedState, transientState, new Builder().build(), callbackList,null);
    }

}
