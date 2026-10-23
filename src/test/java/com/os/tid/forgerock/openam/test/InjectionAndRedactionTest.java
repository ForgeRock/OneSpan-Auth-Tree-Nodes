package com.os.tid.forgerock.openam.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.os.tid.forgerock.openam.utils.RedactUtils;
import com.os.tid.forgerock.openam.utils.v2.StringUtils;

/**
 * Regression tests for OPENAM-29243: credential injection into JSON payloads and URLs, and sensitive values in logs.
 */
public class InjectionAndRedactionTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @DataProvider
    public Object[][] hostileValues() {
        return new Object[][] {
                {"bob\",\"admin\":true,\"x\":\""},
                {"bob\"}"},
                {"back\\slash"},
                {"line\nbreak\r\ttab"},
                {"unicode é中文"},
                {"slash/and%percent"},
                {"{\"nested\":\"json\"}"},
        };
    }

    @Test(dataProvider = "hostileValues")
    public void escapedValueStaysASingleJsonStringField(String hostile) throws Exception {
        String payload = String.format("{\"userID\":\"%s\",\"login\":\"fixed\"}", StringUtils.escapeJson(hostile));

        JsonNode node = MAPPER.readTree(payload);

        assertThat(node.size()).isEqualTo(2);
        assertThat(node.get("userID").asText()).isEqualTo(hostile);
        assertThat(node.get("login").asText()).isEqualTo("fixed");
    }

    @Test
    public void unescapedValueWouldInjectAField() throws Exception {
        String hostile = "bob\",\"admin\":true,\"x\":\"";
        String payload = String.format("{\"userID\":\"%s\"}", hostile);

        assertThat(MAPPER.readTree(payload).has("admin")).isTrue();
    }

    @Test
    public void escapeJsonTreatsNullAsEmpty() {
        assertThat(StringUtils.escapeJson(null)).isEmpty();
    }

    @Test(dataProvider = "hostileValues")
    public void encodedUrlValueIsASinglePathSegmentAndRoundTrips(String hostile) throws Exception {
        String encoded = StringUtils.encodeUrlValue(hostile);

        assertThat(encoded).matches("[A-Za-z0-9._~%]*");
        URI uri = URI.create("https://api.example.com/users/" + encoded + "/status");
        assertThat(uri.getRawPath()).isEqualTo("/users/" + encoded + "/status");
        assertThat(uri.getRawQuery()).isNull();
        assertThat(URLDecoder.decode(encoded.replace("+", "%2B"), StandardCharsets.UTF_8)).isEqualTo(hostile);
    }

    @Test
    public void encodeUrlValueEncodesReservedCharacters() {
        assertThat(StringUtils.encodeUrlValue("a/b?x=1&y=\"}")).isEqualTo("a%2Fb%3Fx%3D1%26y%3D%22%7D");
        assertThat(StringUtils.encodeUrlValue("Az09-._~")).isEqualTo("Az09-._~");
        assertThat(StringUtils.encodeUrlValue(null)).isEmpty();
    }

    @Test
    public void redactJsonMasksCredentialFieldsAtAnyDepth() throws Exception {
        String payload = "{\"userID\":\"bob\",\"OTP\":\"123456\",\"password\":\"s3cret\",\"passKey\":\"pk\","
                + "\"signature\":\"sig\",\"authenticationResponse\":\"auth\",\"deviceCode\":\"dc\","
                + "\"nested\":{\"staticPassword\":\"sp\",\"items\":[{\"otp\":\"654321\"}]}}";

        String redacted = RedactUtils.redactJson(payload);

        for (String secret : new String[] {"123456", "s3cret", "\"pk\"", "\"sig\"", "\"auth\"", "\"dc\"", "\"sp\"", "654321"}) {
            assertThat(redacted).doesNotContain(secret);
        }
        JsonNode node = MAPPER.readTree(redacted);
        assertThat(node.get("userID").asText()).isEqualTo("bob");
        assertThat(node.get("OTP").asText()).isEqualTo(RedactUtils.REDACTED);
        assertThat(node.get("nested").get("staticPassword").asText()).isEqualTo(RedactUtils.REDACTED);
        assertThat(node.get("nested").get("items").get(0).get("otp").asText()).isEqualTo(RedactUtils.REDACTED);
    }

    @Test
    public void redactJsonMasksPayloadEmbeddedInErrorMessage() {
        String message = "POST https://api.example.com/x : {\"userID\":\"bob\",\"password\":\"s3cret\"}";

        String redacted = RedactUtils.redactJson(message);

        assertThat(redacted).doesNotContain("s3cret");
        assertThat(redacted).startsWith("POST https://api.example.com/x : ");
        assertThat(redacted).contains("bob");
    }

    @Test
    public void redactJsonNeverEmitsUnparseableText() {
        String redacted = RedactUtils.redactJson("password=s3cret&otp=123456");

        assertThat(redacted).doesNotContain("s3cret").doesNotContain("123456");
    }

    @Test
    public void redactJsonPassesThroughEmptyInput() {
        assertThat(RedactUtils.redactJson(null)).isNull();
        assertThat(RedactUtils.redactJson("")).isEmpty();
    }
}
