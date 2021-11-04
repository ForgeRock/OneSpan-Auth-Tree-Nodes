package com.os.tid.forgerock.openam.test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.os.tid.forgerock.openam.nodes.OS_Sample_TransactionCollector;
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
public class OS_Sample_TransactionCollectorTest {
    private final String BUNDLE = "com/os/tid/forgerock/openam/nodes/OS_Sample_TransactionCollector";

    @Mock
    private OS_Sample_TransactionCollector.Config config;

    @BeforeMethod
    public void before() {
        initMocks(this);
    }

    @Test
    public void testProcessWithoutInput() throws NodeProcessException {
        // Given
        given(config.passKeyRequired()).willReturn(false);
        OS_Sample_TransactionCollector node = new OS_Sample_TransactionCollector(config);
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


    /**
     *                 put(bundle.getString("callback.username"),Constants.OSTID_DEFAULT_USERNAME);
     *                 put(bundle.getString("callback.amount"),Constants.OSTID_DEFAULT_AMOUNT);
     *                 put(bundle.getString("callback.currency"),Constants.OSTID_DEFAULT_CURRENCY);
     *                 put(bundle.getString("callback.transactionType"),Constants.OSTID_DEFAULT_TRANSACTIONTYPE);
     *                 put(bundle.getString("callback.accountRef"),Constants.OSTID_DEFAULT_ACCOUNTREF);
     *                 put(bundle.getString("callback.creditorName"),Constants.OSTID_DEFAULT_CREDITORNAME);
     *                 put(bundle.getString("callback.creditorIBAN"),Constants.OSTID_DEFAULT_CREDITORIBAN);
     *                 put(bundle.getString("callback.creditorBank"),Constants.OSTID_DEFAULT_CREDITORBANK);
     *                 put(bundle.getString("callback.debtorIBAN"),Constants.OSTID_DEFAULT_DEBTORIBAN);
     */
    @Test
    public void testProcessWithInput() throws NodeProcessException {
        // Given
        given(config.passKeyRequired()).willReturn(false);
        OS_Sample_TransactionCollector node = new OS_Sample_TransactionCollector(config);
        ResourceBundle bundle = new PreferredLocales().getBundleInPreferredLocale(BUNDLE, OS_Sample_TransactionCollector.class.getClassLoader());
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

        NameCallback nameCallback8 = new NameCallback(bundle.getString("callback.creditorBank"));
        nameCallback8.setName("ABC Bank");

        NameCallback nameCallback9 = new NameCallback(bundle.getString("callback.debtorIBAN"));
        nameCallback9.setName("IBAN234234");

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

    @Test
    public void testProcessWithPassKey() throws NodeProcessException {
        // Given
        given(config.passKeyRequired()).willReturn(true);
        OS_Sample_TransactionCollector node = new OS_Sample_TransactionCollector(config);
        ResourceBundle bundle = new PreferredLocales().getBundleInPreferredLocale(BUNDLE, OS_Sample_TransactionCollector.class.getClassLoader());
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

        NameCallback nameCallback8 = new NameCallback(bundle.getString("callback.creditorBank"));
        nameCallback8.setName("ABC Bank");

        NameCallback nameCallback9 = new NameCallback(bundle.getString("callback.debtorIBAN"));
        nameCallback9.setName("IBAN234234");
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
                nameCallback8,
                nameCallback9,
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
        OS_Sample_TransactionCollector node = new OS_Sample_TransactionCollector(config);

        //tree context
        ResourceBundle bundle = new PreferredLocales().getBundleInPreferredLocale(BUNDLE, OS_Sample_TransactionCollector.class.getClassLoader());
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

        NameCallback nameCallback8 = new NameCallback(bundle.getString("callback.creditorBank"));
        nameCallback8.setName("ABC Bank");

        NameCallback nameCallback9 = new NameCallback(bundle.getString("callback.debtorIBAN"));
        nameCallback9.setName("IBAN234234");

        NameCallback nameCallback10 = new NameCallback("mobilePhoneNumber");
        nameCallback10.setName(TestData.TEST_MOBILE_PHONE);

        NameCallback nameCallback11 = new NameCallback("emailAddress");
        nameCallback11.setName(TestData.TEST_EMAIL_ADDRESS);
        TreeContext context = getContext(json(object(1)),json(object(1)),ImmutableList.of(
                nameCallback1,
                nameCallback2,
                nameCallback3,
                nameCallback4,
                nameCallback5,
                nameCallback6,
                nameCallback7,
                nameCallback8,
                nameCallback9,
                nameCallback10,
                nameCallback11
        ));

        // When
        Action result = node.process(context);

        // Then
        assertThat(result.outcome).isEqualTo("outcome");
        assertThat(result.callbacks).isEmpty();
    }

    private TreeContext getContext(JsonValue sharedState, JsonValue transientState, List<Callback> callbackList) {
        return new TreeContext("managed/user", sharedState, transientState, new Builder().build(), callbackList,null);
    }

}
