package com.os.tid.forgerock.openam.nodes;


import java.io.*;
import java.net.HttpURLConnection;
import java.util.*;
import java.util.regex.Pattern;

import javax.inject.Inject;

import com.sun.identity.authentication.spi.RedirectCallback;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.*;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.util.i18n.PreferredLocales;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URL;
import com.google.inject.assistedinject.Assisted;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.net.URI;
import org.json.JSONObject;
import com.sun.identity.sm.RequiredValueValidator;
/*
 * This code is to be used exclusively in connection with ForgeRock’s software or services.
 * ForgeRock only offers ForgeRock software or services to legal entities who have entered
 * into a binding license agreement with ForgeRock.
 */

@Node.Metadata(outcomeProvider  = OS_IDV_IdentityVerificationNode.OutcomeProvider.class,
        configClass      = OS_IDV_IdentityVerificationNode.Config.class, tags = {"marketplace","trustnetwork"})
public class OS_IDV_IdentityVerificationNode implements Node {

    private String loggerPrefix = "[OneSpan Identity Verification Node]" + OSAuthNodePlugin.logAppender;
    private final Logger logger = LoggerFactory.getLogger(OS_IDV_IdentityVerificationNode.class);
    private final Config config;
    private final Realm realm;

    /**
     * Configuration for the node.
     */
    public interface Config {
        /**
         * The header name for zero-page login that will contain the identity's username.
         */
        @Attribute(order = 100)
        default String authToken() {
            return "";
        }

        /**
         * The header name for zero-page login that will contain the identity's password.
         */
        @Attribute(order = 200)
        default String workflowId() {
            return "";
        }

        /**
         * The group name (or fully-qualified unique identifier) for the group that the identity must be in.
         */
        @Attribute(order = 300)
        default String url() {
            return "";
        }

        @Attribute(order = 400)
        default String XTenant() {
            return "";
        }

        @Attribute(order = 500)
        default String brandId() {
            return "";
        }

        @Attribute(order = 600)
        default String language() {
            return "en";
        }

        @Attribute(order = 700)
        default String failUrl() {
            return "";
        }

        @Attribute(order = 800)
        default String passUrl() {
            return "";
        }
        @Attribute(order = 810)
        default String sessionTimeoutUrl() {
            return "";
        }
        @Attribute(order = 820)
        default String errorUrl() {
            return "";
        }
        @Attribute(order = 830)
        default String defaultUrl() {
            return "";
        }

        @Attribute(order = 900)
        default String role() {
            return "Customer";
        }
        @Attribute(order = 1000)
        default CustomPayload customPayload() {
            return CustomPayload.none;
        }

    }


    /**
     * Create the node using Guice injection. Just-in-time bindings can be used to obtain instances of other classes
     * from the plugin.
     *
     * @param config The service config.
     * @param realm The realm the node is in.
     * @throws NodeProcessException If the configuration was not valid.
     */
    @Inject
    public OS_IDV_IdentityVerificationNode(@Assisted Config config, @Assisted Realm realm) throws NodeProcessException {
        this.config = config;
        this.realm = realm;
    }

    static String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder retVal = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            retVal.append(line).append("\n");
        }

        reader.close();
        return retVal.toString();
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        try {

            NodeState ns = context.getStateFor(this);
            Map<String, List<String>> parameters = context.request.parameters;

            if (parameters.containsKey("transaction"))
            {
                if (!parameters.containsKey("opaqueId")) {
                    return Action.goTo("fail").build();
                }
                String transactionId = parameters.get("transaction").get(0);
                ns.putShared("os_transactionId", transactionId);
                String sharedOpaqueId = ns.get("opaqueId").asString();
                ns.remove("opaqueId");
                String opaqueId = parameters.get("opaqueId").get(0);
                if(!opaqueId.equals(sharedOpaqueId)) {
                    return Action.goTo("fail").build();
                }

                //Do shared state stuff with opaqueId

                HttpURLConnection conn = null;
                URL url;

                url = new URL(config.url() + "transactions/" + transactionId +"/detailed-verification-report");
                conn = (HttpURLConnection) url.openConnection();
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.setRequestMethod("GET");

                conn.setRequestProperty("X-Tenant", config.XTenant());
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + config.authToken());
                int responseCode;

                String streamToString;
                streamToString = convertStreamToString(conn.getInputStream());
                responseCode = conn.getResponseCode();

                ns.putShared("verificationResults", streamToString);

                JSONObject jo = new JSONObject(streamToString);
                String result = jo.getString("result");



                if (responseCode == 200 && result.equals("Passed")) {
                    return Action.goTo("pass").build();
                } else if (result.equals("Failed")){
                    return Action.goTo("fail").build();
                }
                return Action.goTo("error").build();

            }

            String opaqueId = String.valueOf(UUID.randomUUID());
            ns.putShared("opaqueId", opaqueId);
            int responseCode = 0;
            String redirectURL = null;

            switch (config.customPayload()) {
                case usersPayload: {
                    HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(60)).build();
                    HttpRequest.Builder requestBuilder;
                    String osBody = ns.get("os_payload_body").asString();
                    JSONObject jsonBody = new JSONObject(osBody);

                    JSONObject bodyObject2 = new JSONObject();
                    bodyObject2.put("role", config.role());

                    ArrayList<JSONObject> tokens = new ArrayList<>();
                    tokens.add(bodyObject2);

                    JSONObject configurationObject = new JSONObject();

                    ArrayList<JSONObject> redirects = new ArrayList<>();

                    JSONObject redirectFailObject = new JSONObject();
                    redirectFailObject.put("id", "REDIRECT_DOCID_FAIL");
                    redirectFailObject.put("url", config.failUrl());

                    JSONObject redirectPassObject = new JSONObject();
                    redirectPassObject.put("id", "REDIRECT_DOCID_PASS");
                    redirectPassObject.put("url", config.passUrl());

                    if(config.sessionTimeoutUrl() != null && !config.sessionTimeoutUrl().isEmpty()) {
                        JSONObject redirectTimeoutObject = new JSONObject();
                        redirectTimeoutObject.put("id", "REDIRECT_SESSION_TIMEOUT");
                        redirectTimeoutObject.put("url", config.sessionTimeoutUrl());
                        redirects.add(redirectTimeoutObject);
                    }

                    if(config.errorUrl() != null && !config.errorUrl().isEmpty()) {
                        JSONObject redirectErrorObject = new JSONObject();
                        redirectErrorObject.put("id", "REDIRECT_ERROR");
                        redirectErrorObject.put("url", config.errorUrl());
                        redirects.add(redirectErrorObject);
                    }
                    if(config.defaultUrl() != null && !config.defaultUrl().isEmpty()) {
                        JSONObject redirectDefaultObject = new JSONObject();
                        redirectDefaultObject.put("id", "REDIRECT_DEFAULT_URL");
                        redirectDefaultObject.put("url", config.defaultUrl());
                        redirects.add(redirectDefaultObject);
                    }

                    redirects.add(redirectFailObject);
                    redirects.add(redirectPassObject);

                    configurationObject.put("redirects", redirects);

                    jsonBody.put("configuration", configurationObject);


                    jsonBody.put("workflow_id", config.workflowId());
                    jsonBody.put("opaque_id", opaqueId);
                    jsonBody.put("language", config.language());
                    jsonBody.put("brand_id", config.brandId());

                    jsonBody.put("tokens", tokens);

                    requestBuilder = HttpRequest.newBuilder().PUT(HttpRequest.BodyPublishers.ofString(jsonBody.toString()));

                    requestBuilder.header("Authorization", "Bearer " + config.authToken());
                    requestBuilder.header("X-Tenant", config.XTenant());
                    requestBuilder.header("Content-Type", "application/json");
                    
                    HttpRequest request = requestBuilder.uri(URI.create(config.url() + "transaction/")).timeout(Duration.ofSeconds(60)).build();

                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                    responseCode =  response.statusCode();

                    JSONObject jo = new JSONObject(response.body());
                    redirectURL = (String) jo.getJSONArray("tokens").getJSONObject(0).getString("accessUrl");
                    break;
                }
                case customPayload : {
                    HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(60)).build();
                    HttpRequest.Builder requestBuilder;
                    String osBody = ns.get("os_payload_body").asString();
                    JSONObject jsonBody = new JSONObject(osBody);
                    jsonBody.put("opaqueId", opaqueId);

                    requestBuilder = HttpRequest.newBuilder().PUT(HttpRequest.BodyPublishers.ofString(jsonBody.toString()));

                    requestBuilder.header("Authorization", "Bearer " + config.authToken());
                    requestBuilder.header("X-Tenant", config.XTenant());
                    requestBuilder.header("Content-Type", "application/json");
                    
                    HttpRequest request = requestBuilder.uri(URI.create(config.url() + "transaction/")).timeout(Duration.ofSeconds(60)).build();

                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                    responseCode =  response.statusCode();

                    JSONObject jo = new JSONObject(response.body());
                    redirectURL = (String) jo.getJSONArray("tokens").getJSONObject(0).getString("accessUrl");
                }
                case none : {
                    HttpURLConnection conn1 = null;
                    URL url;

                    url = new URL(config.url() + "transaction/");


                    conn1 = (HttpURLConnection) url.openConnection();
                    conn1.setDoOutput(true);
                    conn1.setDoInput(true);
                    conn1.setRequestMethod("PUT");
                    conn1.setRequestProperty("X-Tenant", config.XTenant());
                    conn1.setRequestProperty("Content-Type", "application/json");
                    conn1.setRequestProperty("Authorization", "Bearer " + config.authToken());

                    JSONObject bodyObject = new JSONObject();
                    JSONObject userObject = new JSONObject();


                    userObject.put("first_name", ns.get("objectAttributes").get("givenName").asString());
                    userObject.put("last_name", ns.get("objectAttributes").get("sn").asString());
                    userObject.put("role", config.role());
                    userObject.put("phone_number", ns.get("objectAttributes").get("telephoneNumber").asString());
                    if(ns.get("objectAttributes") != null && ns.get("objectAttributes").get("mail") != null) {
                        userObject.put("email", ns.get("objectAttributes").get("mail").asString());
                    }

                    ArrayList<JSONObject> users = new ArrayList<>();
                    users.add(userObject);

                    JSONObject bodyObject2 = new JSONObject();
                    bodyObject2.put("role", config.role());

                    ArrayList<JSONObject> tokens = new ArrayList<>();
                    tokens.add(bodyObject2);

                    JSONObject configurationObject = new JSONObject();

                    ArrayList<JSONObject> redirects = new ArrayList<>();

                    JSONObject redirectFailObject = new JSONObject();
                    redirectFailObject.put("id", "REDIRECT_DOCID_FAIL");
                    redirectFailObject.put("url", config.failUrl());

                    JSONObject redirectPassObject = new JSONObject();
                    redirectPassObject.put("id", "REDIRECT_DOCID_PASS");
                    redirectPassObject.put("url", config.passUrl());

                    JSONObject redirectTimeoutObject = new JSONObject();
                    redirectTimeoutObject.put("id", "REDIRECT_SESSION_TIMEOUT");
                    redirectTimeoutObject.put("url", config.sessionTimeoutUrl());

                    JSONObject redirectErrorObject = new JSONObject();
                    redirectErrorObject.put("id", "REDIRECT_ERROR");
                    redirectErrorObject.put("url", config.errorUrl());

                    JSONObject redirectDefaultObject = new JSONObject();
                    redirectDefaultObject.put("id", "REDIRECT_DEFAULT_URL");
                    redirectDefaultObject.put("url", config.defaultUrl());

                    redirects.add(redirectFailObject);
                    redirects.add(redirectPassObject);
                    redirects.add(redirectTimeoutObject);
                    redirects.add(redirectErrorObject);
                    redirects.add(redirectDefaultObject);

                    configurationObject.put("redirects", redirects);

                    bodyObject.put("configuration", configurationObject);


                    bodyObject.put("workflow_id", config.workflowId());
                    bodyObject.put("opaque_id", opaqueId);
                    bodyObject.put("language", config.language());
                    bodyObject.put("brand_id", config.brandId());
                    bodyObject.put("users", users);

                    bodyObject.put("tokens", tokens);
                    OutputStreamWriter wr1;


                    wr1 = new OutputStreamWriter(conn1.getOutputStream());
                    wr1.write(bodyObject.toString());
                    wr1.flush();

                    String streamToString;
                    streamToString = convertStreamToString(conn1.getInputStream());
                    responseCode = conn1.getResponseCode();
                    JSONObject jo = new JSONObject(streamToString);
                    redirectURL = (String) jo.getJSONArray("tokens").getJSONObject(0).getString("accessUrl");
                    break;
                }
                default : {
                    HttpURLConnection conn1 = null;
                    URL url;

                    url = new URL(config.url() + "transaction/");


                    conn1 = (HttpURLConnection) url.openConnection();
                    conn1.setDoOutput(true);
                    conn1.setDoInput(true);
                    conn1.setRequestMethod("PUT");
                    conn1.setRequestProperty("X-Tenant", config.XTenant());
                    conn1.setRequestProperty("Content-Type", "application/json");
                    conn1.setRequestProperty("Authorization", "Bearer " + config.authToken());

                    JSONObject bodyObject = new JSONObject();
                    JSONObject userObject = new JSONObject();


                    userObject.put("first_name", ns.get("objectAttributes").get("givenName").asString());
                    userObject.put("last_name", ns.get("objectAttributes").get("sn").asString());
                    userObject.put("role", config.role());
                    userObject.put("phone_number", ns.get("objectAttributes").get("telephoneNumber").asString());
                    if(ns.get("objectAttributes") != null && ns.get("objectAttributes").get("mail") != null) {
                        userObject.put("email", ns.get("objectAttributes").get("mail").asString());
                    }

                    ArrayList<JSONObject> users = new ArrayList<>();
                    users.add(userObject);

                    JSONObject bodyObject2 = new JSONObject();
                    bodyObject2.put("role", config.role());

                    ArrayList<JSONObject> tokens = new ArrayList<>();
                    tokens.add(bodyObject2);

                    JSONObject configurationObject = new JSONObject();

                    ArrayList<JSONObject> redirects = new ArrayList<>();

                    JSONObject redirectFailObject = new JSONObject();
                    redirectFailObject.put("id", "REDIRECT_DOCID_FAIL");
                    redirectFailObject.put("url", config.failUrl());

                    JSONObject redirectPassObject = new JSONObject();
                    redirectPassObject.put("id", "REDIRECT_DOCID_PASS");
                    redirectPassObject.put("url", config.passUrl());

                    JSONObject redirectTimeoutObject = new JSONObject();
                    redirectTimeoutObject.put("id", "REDIRECT_SESSION_TIMEOUT");
                    redirectTimeoutObject.put("url", config.sessionTimeoutUrl());

                    JSONObject redirectErrorObject = new JSONObject();
                    redirectErrorObject.put("id", "REDIRECT_ERROR");
                    redirectErrorObject.put("url", config.errorUrl());

                    JSONObject redirectDefaultObject = new JSONObject();
                    redirectDefaultObject.put("id", "REDIRECT_DEFAULT_URL");
                    redirectDefaultObject.put("url", config.defaultUrl());

                    redirects.add(redirectFailObject);
                    redirects.add(redirectPassObject);
                    redirects.add(redirectTimeoutObject);
                    redirects.add(redirectErrorObject);
                    redirects.add(redirectDefaultObject);

                    configurationObject.put("redirects", redirects);

                    bodyObject.put("configuration", configurationObject);


                    bodyObject.put("workflow_id", config.workflowId());
                    bodyObject.put("opaque_id", opaqueId);
                    bodyObject.put("language", config.language());
                    bodyObject.put("brand_id", config.brandId());
                    bodyObject.put("users", users);

                    bodyObject.put("tokens", tokens);
                    OutputStreamWriter wr1;


                    wr1 = new OutputStreamWriter(conn1.getOutputStream());
                    wr1.write(bodyObject.toString());
                    wr1.flush();

                    String streamToString;
                    streamToString = convertStreamToString(conn1.getInputStream());
                    responseCode = conn1.getResponseCode();
                    JSONObject jo = new JSONObject(streamToString);
                    redirectURL = (String) jo.getJSONArray("tokens").getJSONObject(0).getString("accessUrl");
                    break;
                }
            }

            if (responseCode == 200) {
                RedirectCallback redirectCallback = new RedirectCallback(redirectURL, null, "GET");
                redirectCallback.setTrackingCookie(true);
                return Action.send(redirectCallback).build();
            }
            return Action.goTo("error").build();
        } catch(Exception ex) {
            logger.error(loggerPrefix + "Exception occurred: " + ex.getStackTrace());
            context.getStateFor(this).putShared(loggerPrefix + "Exception", new Date() + ": " + ex.getMessage());
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            context.getStateFor(this).putShared(loggerPrefix + "StackTrack", new Date() + ": " + sw.toString());
            return Action.goTo("error").build();
        }
    }
    public static final class OutcomeProvider implements org.forgerock.openam.auth.node.api.OutcomeProvider {
        /**
         * Outcomes Ids for this node.
         */
        static final String SUCCESS_OUTCOME = "pass";

        static final String FAILURE_OUTCOME = "fail";
        static final String ERROR_OUTCOME = "error";
        private static final String BUNDLE = OS_IDV_IdentityVerificationNode.class.getName();

        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {

            ResourceBundle bundle = locales.getBundleInPreferredLocale(BUNDLE, OutcomeProvider.class.getClassLoader());

            List<Outcome> results = new ArrayList<>(
                    Arrays.asList(
                            new Outcome(SUCCESS_OUTCOME, "Pass")
                    )
            );
            results.add(new Outcome(FAILURE_OUTCOME, "Fail"));
            results.add(new Outcome(ERROR_OUTCOME, "Error"));

            return Collections.unmodifiableList(results);
        }
    }

    public enum CustomPayload {
        usersPayload, customPayload, none
    }


}



