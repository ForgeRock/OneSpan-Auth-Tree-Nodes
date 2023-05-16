package com.os.tid.forgerock.openam.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.bind.DatatypeConverter;

import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.os.tid.forgerock.openam.config.Constants;
import com.os.tid.forgerock.openam.nodes.OSConfigurationsService;

public class SslUtils {
    private static final Logger logger = LoggerFactory.getLogger("amAuth");
    private static final String loggerPrefix = "[OneSpan Auth SslUtils][Marketplace] ";

    private SslUtils() {
    }

    public static SSLConnectionSocketFactory getSSLConnectionSocketFactory(OSConfigurationsService serviceConfig) {
        String environment = Constants.OSTID_ENV_MAP.get(serviceConfig.environment());
    	if("sdb".equals(environment)) {
    		return null;
    	}else {
    		try {
				return getSSLConnectionSocketFactory(serviceConfig.privateKey(), serviceConfig.publicKey());
			} catch (Exception e) {
				String stackTrace = org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(e);
				logger.error(loggerPrefix + "Exception occurred: " + stackTrace);
				return null;
			}
    	}
    }
    
    private static SSLConnectionSocketFactory getSSLConnectionSocketFactory(String privateKeyPem, String certificatePem) throws Exception {
		SSLContext sslContext = createSSLContext(privateKeyPem, certificatePem);
		SSLConnectionSocketFactory sslConSocFactory = new SSLConnectionSocketFactory(sslContext, (hostname, session) -> true);
    	return sslConSocFactory;
    }

    private static SSLContext createSSLContext(String privateKeyPem, String certificatePem) throws Exception {
        final SSLContext context = SSLContext.getInstance("TLS");
        final KeyStore keystore = createKeyStore(privateKeyPem, certificatePem);
        final KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(keystore, new char[0]);
        final KeyManager[] km = kmf.getKeyManagers();
        context.init(km, new TrustManager[]{new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(
                    X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(
                    X509Certificate[] certs, String authType) {
            }
        }}, new SecureRandom());
        return context;
    }

    /**
     * Create a KeyStore from standard PEM files
     * 
     * @param privateKeyPem the private key PEM file
     * @param certificatePem the certificate(s) PEM file
     * @param the password to set to protect the private key
     */
    private static KeyStore createKeyStore(String privateKeyPem, String certificatePem)
            throws Exception, KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        final X509Certificate[] cert = createCertificates(certificatePem);
        final KeyStore keystore = KeyStore.getInstance("JKS");
        keystore.load(null);
        // Import private key
        final PrivateKey key = createPrivateKey(privateKeyPem);
        keystore.setKeyEntry(Constants.OSTID_KEYSTORE_ALIAS, key, new char[0], cert);
        return keystore;
    }

    private static PrivateKey createPrivateKey(String privateKeyPem) throws Exception {
    	String content = privateKeyPem
    			.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("\n", "")
                .trim(); 

        final byte[] bytes = DatatypeConverter.parseBase64Binary(content);
        return generatePrivateKeyFromDER(bytes);
    }

    private static X509Certificate[] createCertificates(String certificatePem) throws Exception {
        final List<X509Certificate> result = new ArrayList<X509Certificate>();
    	String content = certificatePem
    			.replace("-----BEGIN CERTIFICATE-----", "")
                .replace("-----END CERTIFICATE-----", "")
                .replace("\n", "")
                .trim(); 
	    final byte[] bytes = DatatypeConverter.parseBase64Binary(content);
	    X509Certificate cert = generateCertificateFromDER(bytes);
	    result.add(cert);
        return result.toArray(new X509Certificate[result.size()]);
    }

    private static RSAPrivateKey generatePrivateKeyFromDER(byte[] keyBytes) throws InvalidKeySpecException, NoSuchAlgorithmException {
        final PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        final KeyFactory factory = KeyFactory.getInstance("RSA");
        return (RSAPrivateKey) factory.generatePrivate(spec);
    }

    private static X509Certificate generateCertificateFromDER(byte[] certBytes) throws CertificateException {
        final CertificateFactory factory = CertificateFactory.getInstance("X.509");
        return (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(certBytes));
    }
   

}
