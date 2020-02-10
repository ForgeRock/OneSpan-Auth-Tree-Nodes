package com.os.tid.forgerock.openam.test;

import com.os.tid.forgerock.openam.config.Constants;
import com.os.tid.forgerock.openam.nodes.OSTID_DEMO_ErrorDisplayNode;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext.Builder;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.TextOutputCallback;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.mockito.MockitoAnnotations.initMocks;

@Test
public class OSTID_DEMO_ErrorDisplayNodeTest {

    @BeforeMethod
    public void before() {
        initMocks(this);
    }

    @Test
    public void testProcessErrorDisplay() {
        // Given
        OSTID_DEMO_ErrorDisplayNode node = new OSTID_DEMO_ErrorDisplayNode();
        JsonValue sharedState = json(object(1));
        sharedState.put(Constants.OSTID_ERROR_MESSAGE,"some dummy error message!");
        TreeContext context = getContext(sharedState,json(object(1)),Collections.emptyList());

        // When
        Action result = node.process(context);

        // Then
        assertThat(result.outcome).isEqualTo(null);
        assertThat(result.callbacks).hasSize(1);
        assertThat(result.callbacks.get(0)).isInstanceOf(TextOutputCallback.class);
    }

    @Test
    public void testProcessNext() {
        // Given
        OSTID_DEMO_ErrorDisplayNode node = new OSTID_DEMO_ErrorDisplayNode();
        TreeContext context = getContext(json(object(1)),json(object(1)),Collections.emptyList());

        // When
        Action result = node.process(context);

        // Then
        assertThat(result.outcome).isEqualTo("outcome");
        assertThat(result.callbacks).isEmpty();
    }

    private TreeContext getContext(JsonValue sharedState, JsonValue transientState, List<Callback> callbackList) {
        return new TreeContext(sharedState, transientState, new Builder().build(), callbackList);
    }

}
