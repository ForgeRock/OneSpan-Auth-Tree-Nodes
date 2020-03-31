package com.os.tid.forgerock.openam.test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.os.tid.forgerock.openam.config.Constants;
import com.os.tid.forgerock.openam.nodes.OSTIDCDDCNode;
import com.os.tid.forgerock.openam.nodes.OSTIDCheckSessionStatusNode;
import com.os.tid.forgerock.openam.nodes.OSTID_DEMO_TransactionCollector;
import com.sun.identity.authentication.callbacks.HiddenValueCallback;
import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext.Builder;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.util.i18n.PreferredLocales;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.MockitoAnnotations.initMocks;

@Test
public class OSTID_DEMO_TransactionCollectorTest {
    private final String BUNDLE = "com/os/tid/forgerock/openam/nodes/OSTID_DEMO_TransactionCollector";

    @Mock
    private OSTID_DEMO_TransactionCollector.Config config;

    @BeforeMethod
    public void before() {
        initMocks(this);
    }

    @Test
    public void testProcessWithoutInput() throws NodeProcessException {
        // Given
        given(config.passKeyRequired()).willReturn(false);
        OSTID_DEMO_TransactionCollector node = new OSTID_DEMO_TransactionCollector(config);
        TreeContext context = getContext(json(object(1)),json(object(1)),Collections.emptyList());

        // When
        Action result = node.process(context);

        // Then
        assertThat(result.outcome).isEqualTo(null);
        assertThat(result.callbacks).hasSize(7);
        assertThat(result.callbacks.get(0)).isInstanceOf(NameCallback.class);
        assertThat(result.callbacks.get(1)).isInstanceOf(NameCallback.class);
        assertThat(result.callbacks.get(2)).isInstanceOf(NameCallback.class);
        assertThat(result.callbacks.get(3)).isInstanceOf(NameCallback.class);
        assertThat(result.callbacks.get(4)).isInstanceOf(NameCallback.class);
        assertThat(result.callbacks.get(5)).isInstanceOf(NameCallback.class);
        assertThat(result.callbacks.get(6)).isInstanceOf(NameCallback.class);
    }

    @Test
    public void testProcessWithInput() throws NodeProcessException {
        // Given
        given(config.passKeyRequired()).willReturn(false);
        OSTID_DEMO_TransactionCollector node = new OSTID_DEMO_TransactionCollector(config);
        ResourceBundle bundle = new PreferredLocales().getBundleInPreferredLocale(BUNDLE,OSTID_DEMO_TransactionCollector.class.getClassLoader());
        NameCallback nameCallback1 = new NameCallback(bundle.getString("callback.username"));
        nameCallback1.setName(TestData.TEST_USERNAME);

        NameCallback nameCallback2 = new NameCallback(bundle.getString("callback.transactionType"));
        nameCallback2.setName("ExternalTransfer");

        NameCallback nameCallback3 = new NameCallback(bundle.getString("callback.accountRef"));
        nameCallback3.setName("Ref123123");

        NameCallback nameCallback4 = new NameCallback(bundle.getString("callback.amount"));
        nameCallback4.setName("66.66");

        NameCallback nameCallback5 = new NameCallback(bundle.getString("callback.creditorIBAN"));
        nameCallback5.setName("IBAN123123");

        NameCallback nameCallback6 = new NameCallback(bundle.getString("callback.currency"));
        nameCallback6.setName("CAD");

        NameCallback nameCallback7 = new NameCallback(bundle.getString("callback.creditorName"));
        nameCallback7.setName("John Smith");

        TreeContext context = getContext(json(object(1)),json(object(1)),ImmutableList.of(
                nameCallback1,
                nameCallback2,
                nameCallback3,
                nameCallback4,
                nameCallback5,
                nameCallback6,
                nameCallback7
        ));

        // When
        Action result = node.process(context);

        // Then
        assertThat(result.outcome).isEqualTo("outcome");
        assertThat(result.callbacks).isEmpty();
    }

    @Test
    public void testProcessWithPassKey() throws NodeProcessException {
        // Given
        given(config.passKeyRequired()).willReturn(true);
        OSTID_DEMO_TransactionCollector node = new OSTID_DEMO_TransactionCollector(config);
        ResourceBundle bundle = new PreferredLocales().getBundleInPreferredLocale(BUNDLE,OSTID_DEMO_TransactionCollector.class.getClassLoader());
        NameCallback nameCallback1 = new NameCallback(bundle.getString("callback.username"));
        nameCallback1.setName(TestData.TEST_USERNAME);

        NameCallback nameCallback2 = new NameCallback(bundle.getString("callback.transactionType"));
        nameCallback2.setName("ExternalTransfer");

        NameCallback nameCallback3 = new NameCallback(bundle.getString("callback.accountRef"));
        nameCallback3.setName("Ref123123");

        NameCallback nameCallback4 = new NameCallback(bundle.getString("callback.amount"));
        nameCallback4.setName("66.66");

        NameCallback nameCallback5 = new NameCallback(bundle.getString("callback.creditorIBAN"));
        nameCallback5.setName("IBAN123123");

        NameCallback nameCallback6 = new NameCallback(bundle.getString("callback.currency"));
        nameCallback6.setName("CAD");

        NameCallback nameCallback7 = new NameCallback(bundle.getString("callback.creditorName"));
        nameCallback7.setName("John Smith");
        PasswordCallback passwordCallback = new PasswordCallback(bundle.getString("callback.password"),false);
        passwordCallback.setPassword("Test1234".toCharArray());

        TreeContext context = getContext(json(object(1)),json(object(1)),ImmutableList.of(
                nameCallback1,
                nameCallback2,
                nameCallback3,
                nameCallback4,
                nameCallback5,
                nameCallback6,
                nameCallback7,
                passwordCallback
        ));

        // When
        Action result = node.process(context);

        // Then
        assertThat(result.outcome).isEqualTo("outcome");
        assertThat(result.callbacks).isEmpty();
    }

    @Test
    public void testProcessWithOptionalAttributes() throws NodeProcessException {
        // Given
        //config
        given(config.passKeyRequired()).willReturn(false);
        given(config.optionalAttributes()).willReturn(ImmutableSet.of(
                "mobilePhoneNumber",
                "emailAddress"
        ));
        OSTID_DEMO_TransactionCollector node = new OSTID_DEMO_TransactionCollector(config);

        //tree context
        ResourceBundle bundle = new PreferredLocales().getBundleInPreferredLocale(BUNDLE,OSTID_DEMO_TransactionCollector.class.getClassLoader());
        NameCallback nameCallback1 = new NameCallback(bundle.getString("callback.username"));
        nameCallback1.setName(TestData.TEST_USERNAME);

        NameCallback nameCallback2 = new NameCallback(bundle.getString("callback.transactionType"));
        nameCallback2.setName("ExternalTransfer");

        NameCallback nameCallback3 = new NameCallback(bundle.getString("callback.accountRef"));
        nameCallback3.setName("Ref123123");

        NameCallback nameCallback4 = new NameCallback(bundle.getString("callback.amount"));
        nameCallback4.setName("66.66");

        NameCallback nameCallback5 = new NameCallback(bundle.getString("callback.creditorIBAN"));
        nameCallback5.setName("IBAN123123");

        NameCallback nameCallback6 = new NameCallback(bundle.getString("callback.currency"));
        nameCallback6.setName("CAD");

        NameCallback nameCallback7 = new NameCallback(bundle.getString("callback.creditorName"));
        nameCallback7.setName("John Smith");

        NameCallback nameCallback8 = new NameCallback("mobilePhoneNumber");
        nameCallback8.setName(TestData.TEST_MOBILE_PHONE);

        NameCallback nameCallback9 = new NameCallback("emailAddress");
        nameCallback9.setName(TestData.TEST_EMAIL_ADDRESS);
        TreeContext context = getContext(json(object(1)),json(object(1)),ImmutableList.of(
                nameCallback1,
                nameCallback2,
                nameCallback3,
                nameCallback4,
                nameCallback5,
                nameCallback6,
                nameCallback7,
                nameCallback8,
                nameCallback9
        ));

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
