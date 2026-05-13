package com.os.tid.forgerock.openam.test;

import com.os.tid.forgerock.openam.nodes.OS_IDV_IdentityVerificationNode;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext.Builder;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.callback.Callback;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.mockito.MockitoAnnotations.initMocks;

@Test
public class OS_IDV_IdentityVerificationNodeTest {

    @Mock
    private OS_IDV_IdentityVerificationNode.Config config;

    @Mock
    private Realm realm;

    @BeforeMethod
    public void before() {
        initMocks(this);
    }

    /**
     * When shared state already contains a non-empty opaqueId (an in-flight transaction),
     * the re-entry guard must fire and return the "error" outcome immediately, without
     * mutating shared state.
     */
    @Test
    public void testInitiationWithExistingOpaqueId() throws NodeProcessException {
        // Given
        String existingOpaqueId = "550e8400-e29b-41d4-a716-446655440000";

        OS_IDV_IdentityVerificationNode node = new OS_IDV_IdentityVerificationNode(config, realm);

        JsonValue sharedState = json(object(1));
        sharedState.put("opaqueId", existingOpaqueId);

        JsonValue transientState = json(object(1));
        // Empty parameters map — initiation mode (no "transaction" key)
        TreeContext context = getContext(sharedState, transientState, Collections.emptyList());

        // When
        Action result = node.process(context);

        // Then — guard fired, outcome is "error"
        assertThat(result.outcome).isEqualTo("error");

        // Then — opaqueId in shared state was NOT changed by the guard
        assertThat(context.getStateFor(node).get("opaqueId").asString()).isEqualTo(existingOpaqueId);
    }

    /**
     * When shared state does not contain an opaqueId, the re-entry guard must NOT fire.
     * Execution proceeds past the guard and reaches the initiation path (UUID generation,
     * putShared, then the HTTP call). Because the HTTP call will fail with no real endpoint
     * configured, the node returns "error" via the exception handler — but only after
     * writing the new opaqueId to shared state. This confirms the guard was bypassed.
     */
    @Test
    public void testInitiationWithNoOpaqueId() throws NodeProcessException {
        // Given
        OS_IDV_IdentityVerificationNode node = new OS_IDV_IdentityVerificationNode(config, realm);

        JsonValue sharedState = json(object(1));
        // Deliberately no "opaqueId" key in shared state

        JsonValue transientState = json(object(1));
        // Empty parameters map — initiation mode (no "transaction" key)
        TreeContext context = getContext(sharedState, transientState, Collections.emptyList());

        // When — the guard is bypassed; execution reaches UUID generation and putShared("opaqueId", …)
        // before failing at the HTTP call. The exception handler returns "error".
        Action result = node.process(context);

        // Then — a new opaqueId was written to shared state, confirming the guard did not
        // short-circuit execution before the UUID/putShared step.
        JsonValue opaqueIdAfter = context.getStateFor(node).get("opaqueId");
        assertThat(opaqueIdAfter.isString()).isTrue();
        assertThat(opaqueIdAfter.asString()).isNotEmpty();
    }

    private TreeContext getContext(JsonValue sharedState, JsonValue transientState, List<Callback> callbackList) {
        return new TreeContext("managed/user", sharedState, transientState, new Builder().build(), callbackList, null);
    }
}
