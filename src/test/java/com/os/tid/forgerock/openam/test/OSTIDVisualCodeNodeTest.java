package com.os.tid.forgerock.openam.test;

import com.iplanet.sso.SSOException;
import com.os.tid.forgerock.openam.config.Constants;
import com.os.tid.forgerock.openam.nodes.OSConfigurationsService;
import com.os.tid.forgerock.openam.nodes.OS_Auth_VisualCodeNode;
import com.sun.identity.authentication.callbacks.HiddenValueCallback;
import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;
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
public class OSTIDVisualCodeNodeTest {
    @Mock
    private OS_Auth_VisualCodeNode.Config config;

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
    public void testProcessWithoutReturningScripts() throws NodeProcessException {
        // Given
        //config
        given(config.visualCodeMessageOption()).willReturn(OS_Auth_VisualCodeNode.VisualCodeMessageOptions.DemoMobileApp);
        given(config.visualCodeHiddenValueId()).willReturn(Constants.OSTID_CRONTO);
        given(config.renderVisualCodeInCallback()).willReturn(false);
        given(config.visualCodeType()).willReturn(OS_Auth_VisualCodeNode.VisualCodeType.Cronto);
        OS_Auth_VisualCodeNode node = new OS_Auth_VisualCodeNode(config,realm,annotatedServiceRegistry);

        //tree context
        JsonValue sharedState = json(object(1));
        sharedState.put(Constants.OSTID_CRONTO_MSG,TestData.TEST_CRONTO_MSG);
        TreeContext context = getContext(sharedState,Collections.emptyList());

        // When
        Action result = node.process(context);

        // Then
        assertThat(result.outcome).isEqualTo(null);
        assertThat(result.callbacks).hasSize(3);
        assertThat(result.callbacks.get(0)).isInstanceOf(HiddenValueCallback.class);
        assertThat(result.callbacks.get(1)).isInstanceOf(HiddenValueCallback.class);
        assertThat(result.callbacks.get(2)).isInstanceOf(HiddenValueCallback.class);
    }

    @Test
    public void testProcessWithCustomCrontoMsg() throws NodeProcessException {
        // Given
        //config
        given(config.visualCodeMessageOption()).willReturn(OS_Auth_VisualCodeNode.VisualCodeMessageOptions.CustomCrontoMessage);
        given(config.customMessageInSharedState()).willReturn("my_custom_cronto_msg");
        given(config.visualCodeHiddenValueId()).willReturn(Constants.OSTID_CRONTO);
        given(config.renderVisualCodeInCallback()).willReturn(false);
        given(config.visualCodeType()).willReturn(OS_Auth_VisualCodeNode.VisualCodeType.Cronto);
        OS_Auth_VisualCodeNode node = new OS_Auth_VisualCodeNode(config,realm,annotatedServiceRegistry);

        //tree context
        JsonValue sharedState = json(object(1));
        sharedState.put("my_custom_cronto_msg",TestData.TEST_CRONTO_MSG);
        TreeContext context = getContext(sharedState,Collections.emptyList());

        // When
        Action result = node.process(context);

        // Then
        assertThat(result.outcome).isEqualTo(null);
        assertThat(result.callbacks).hasSize(3);
        assertThat(result.callbacks.get(0)).isInstanceOf(HiddenValueCallback.class);
        assertThat(result.callbacks.get(1)).isInstanceOf(HiddenValueCallback.class);
        assertThat(result.callbacks.get(2)).isInstanceOf(HiddenValueCallback.class);
    }

    @Test
    public void testProcessWithScriptCallbacks() throws NodeProcessException {
        // Given
        //config
        given(config.visualCodeMessageOption()).willReturn(OS_Auth_VisualCodeNode.VisualCodeMessageOptions.DemoMobileApp);
        given(config.visualCodeHiddenValueId()).willReturn(Constants.OSTID_CRONTO);
        given(config.renderVisualCodeInCallback()).willReturn(true);
        given(config.domIdRenderVisualCode()).willReturn("dialog");
        given(config.sizeOfVisualCode()).willReturn(Constants.OSTID_DEFAULT_CRONTO_HEIGHT);
        given(config.altTextOfVisualCode()).willReturn(Constants.OSTID_DEFAULT_CRONTO_ALT);
        given(config.textForPleaseScan()).willReturn("Please Scan the Visual Code within:");
        given(config.cssForPleaseScan()).willReturn(":");
        given(config.textForExpired()).willReturn("Your Activation Code has been expired!");
        given(config.cssForExpired()).willReturn(":");
        given(config.visualCodeType()).willReturn(OS_Auth_VisualCodeNode.VisualCodeType.Cronto);
        OS_Auth_VisualCodeNode node = new OS_Auth_VisualCodeNode(config,realm,annotatedServiceRegistry);

        //tree context
        JsonValue sharedState = json(object(1));
        sharedState.put(Constants.OSTID_CRONTO_MSG,TestData.TEST_CRONTO_MSG);
        TreeContext context = getContext(sharedState,Collections.emptyList());

        // When
        Action result = node.process(context);

        // Then
        assertThat(result.outcome).isEqualTo(null);
        assertThat(result.callbacks).hasSize(5);
        assertThat(result.callbacks.get(0)).isInstanceOf(HiddenValueCallback.class);
        assertThat(result.callbacks.get(1)).isInstanceOf(HiddenValueCallback.class);
        assertThat(result.callbacks.get(2)).isInstanceOf(HiddenValueCallback.class);
        assertThat(result.callbacks.get(3)).isInstanceOf(ScriptTextOutputCallback.class);
        assertThat(result.callbacks.get(4)).isInstanceOf(ScriptTextOutputCallback.class);
    }

    private TreeContext getContext(JsonValue sharedState, List<Callback> callbackList) {
        return new TreeContext("managed/user", sharedState, new Builder().build(), callbackList);
    }

}
