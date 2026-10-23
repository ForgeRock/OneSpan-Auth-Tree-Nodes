package com.os.tid.forgerock.openam.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.os.tid.forgerock.openam.nodes.OSConfigurationsService;
import com.os.tid.forgerock.openam.nodes.OSConfigurationsService.EnvOptions;
import com.os.tid.forgerock.openam.utils.v2.SslUtils;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsExchange;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;

/**
 * Regression test for OPENAM-29243: SslUtils must validate the server certificate chain and hostname,
 * while still presenting the configured client certificate (mTLS).
 */
public class SslUtilsTest {

    private static final String PASS = "changeit";
    private static final String TRUST_STORE_PROP = "javax.net.ssl.trustStore";
    private static final String TRUST_PASS_PROP = "javax.net.ssl.trustStorePassword";
    private static final String TRUST_TYPE_PROP = "javax.net.ssl.trustStoreType";

    private Path dir;
    private KeyStore clientKs;
    private String clientKeyPem;
    private String clientCertPem;
    private File trustedServerP12;
    private File untrustedServerP12;
    private File wrongHostServerP12;
    private File trustStoreFile;

    private String prevTrustStore;
    private String prevTrustPass;
    private String prevTrustType;

    private HttpsServer server;
    private final AtomicReference<String> seenClientDn = new AtomicReference<>();

    @BeforeClass
    public void setUp() throws Exception {
        dir = Files.createTempDirectory("sslutils-test");

        File clientP12 = keytoolSelfSigned("client", "CN=onespan-test-client", "dns:client.test");
        trustedServerP12 = keytoolSelfSigned("trusted-server", "CN=localhost", "dns:localhost,ip:127.0.0.1");
        untrustedServerP12 = keytoolSelfSigned("untrusted-server", "CN=localhost", "dns:localhost,ip:127.0.0.1");
        wrongHostServerP12 = keytoolSelfSigned("wronghost-server", "CN=other.example.com", "dns:other.example.com");

        clientKs = load(clientP12);
        Key clientKey = clientKs.getKey("client", PASS.toCharArray());
        assertThat(clientKey.getFormat()).isEqualTo("PKCS#8");
        clientKeyPem = pem("PRIVATE KEY", clientKey.getEncoded());
        clientCertPem = pem("CERTIFICATE", clientKs.getCertificate("client").getEncoded());

        // JVM default trust store for the client under test: trusts trusted-server and wronghost-server, but not untrusted-server
        KeyStore trust = KeyStore.getInstance("PKCS12");
        trust.load(null, null);
        trust.setCertificateEntry("trusted-server", load(trustedServerP12).getCertificate("trusted-server"));
        trust.setCertificateEntry("wronghost-server", load(wrongHostServerP12).getCertificate("wronghost-server"));
        trustStoreFile = dir.resolve("truststore.p12").toFile();
        try (FileOutputStream out = new FileOutputStream(trustStoreFile)) {
            trust.store(out, PASS.toCharArray());
        }

        prevTrustStore = System.getProperty(TRUST_STORE_PROP);
        prevTrustPass = System.getProperty(TRUST_PASS_PROP);
        prevTrustType = System.getProperty(TRUST_TYPE_PROP);
        System.setProperty(TRUST_STORE_PROP, trustStoreFile.getAbsolutePath());
        System.setProperty(TRUST_PASS_PROP, PASS);
        System.setProperty(TRUST_TYPE_PROP, "PKCS12");
    }

    @AfterMethod(alwaysRun = true)
    public void stopServer() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
        seenClientDn.set(null);
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() {
        restore(TRUST_STORE_PROP, prevTrustStore);
        restore(TRUST_PASS_PROP, prevTrustPass);
        restore(TRUST_TYPE_PROP, prevTrustType);
    }

    @Test
    public void sandboxEnvironmentUsesDefaultClient() {
        assertThat(SslUtils.getSSLConnectionSocketFactory(config(EnvOptions.Sandbox))).isNull();
    }

    @Test
    public void trustedServerWithMutualTlsIsAccepted() throws Exception {
        startServer(trustedServerP12, true);

        String body = get(SslUtils.getSSLConnectionSocketFactory(config(EnvOptions.prod)));

        assertThat(body).isEqualTo("ok");
        assertThat(seenClientDn.get()).contains("CN=onespan-test-client");
    }

    @Test
    public void selfSignedServerNotInTrustStoreIsRejected() throws Exception {
        startServer(untrustedServerP12, true);
        SSLConnectionSocketFactory factory = SslUtils.getSSLConnectionSocketFactory(config(EnvOptions.prod));

        assertThatThrownBy(() -> get(factory)).isInstanceOf(SSLException.class);
        assertThat(seenClientDn.get()).isNull();
    }

    @Test
    public void certificateForDifferentHostnameIsRejected() throws Exception {
        startServer(wrongHostServerP12, true);
        SSLConnectionSocketFactory factory = SslUtils.getSSLConnectionSocketFactory(config(EnvOptions.prod));

        assertThatThrownBy(() -> get(factory)).isInstanceOf(SSLException.class);
        assertThat(seenClientDn.get()).isNull();
    }

    private OSConfigurationsService config(EnvOptions env) {
        OSConfigurationsService cfg = mock(OSConfigurationsService.class);
        when(cfg.environment()).thenReturn(env);
        when(cfg.privateKey()).thenReturn(clientKeyPem);
        when(cfg.publicKey()).thenReturn(clientCertPem);
        return cfg;
    }

    private String get(SSLConnectionSocketFactory factory) throws Exception {
        assertThat(factory).isNotNull();
        try (CloseableHttpClient client = HttpClients.custom().setSSLSocketFactory(factory).build();
             CloseableHttpResponse resp = client.execute(new HttpGet("https://localhost:" + server.getAddress().getPort() + "/"))) {
            return EntityUtils.toString(resp.getEntity(), StandardCharsets.UTF_8);
        }
    }

    private void startServer(File serverP12, boolean needClientAuth) throws Exception {
        KeyStore serverKs = load(serverP12);
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(serverKs, PASS.toCharArray());

        KeyStore clientTrust = KeyStore.getInstance("PKCS12");
        clientTrust.load(null, null);
        clientTrust.setCertificateEntry("client", clientKs.getCertificate("client"));
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(clientTrust);

        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        server = HttpsServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.setHttpsConfigurator(new HttpsConfigurator(ctx) {
            @Override
            public void configure(HttpsParameters params) {
                params.setNeedClientAuth(needClientAuth);
            }
        });
        server.createContext("/", exchange -> {
            try {
                Certificate[] peers = ((HttpsExchange) exchange).getSSLSession().getPeerCertificates();
                seenClientDn.set(((java.security.cert.X509Certificate) peers[0]).getSubjectX500Principal().getName());
            } catch (Exception ignored) {
                // no client certificate presented
            }
            byte[] out = "ok".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, out.length);
            exchange.getResponseBody().write(out);
            exchange.close();
        });
        server.start();
    }

    private File keytoolSelfSigned(String alias, String dn, String san) throws Exception {
        File out = dir.resolve(alias + ".p12").toFile();
        String keytool = Path.of(System.getProperty("java.home"), "bin", "keytool").toString();
        Process p = new ProcessBuilder(keytool, "-genkeypair", "-alias", alias, "-keyalg", "RSA", "-keysize", "2048",
                "-sigalg", "SHA256withRSA", "-validity", "2", "-dname", dn, "-ext", "san=" + san,
                "-storetype", "PKCS12", "-keystore", out.getAbsolutePath(), "-storepass", PASS, "-keypass", PASS)
                .redirectErrorStream(true).start();
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (!p.waitFor(60, TimeUnit.SECONDS) || p.exitValue() != 0) {
            throw new IllegalStateException("keytool failed: " + output);
        }
        return out;
    }

    private static KeyStore load(File p12) throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (InputStream in = new FileInputStream(p12)) {
            ks.load(in, PASS.toCharArray());
        }
        return ks;
    }

    private static String pem(String type, byte[] der) {
        String b64 = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8)).encodeToString(der);
        return "-----BEGIN " + type + "-----\n" + b64 + "\n-----END " + type + "-----\n";
    }

    private static void restore(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }
}
