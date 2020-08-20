package com.quorum.gauge.services;

import com.quorum.gauge.common.QuorumNetworkProperty;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.authentication.SessionManager;
import org.springframework.vault.authentication.SimpleSessionManager;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.core.VaultOperations;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.core.VaultVersionedKeyValueOperations;
import org.springframework.vault.core.VaultVersionedKeyValueTemplate;
import org.springframework.vault.support.Versioned;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Creates a client to the Hashicorp Vault server and provides methods to read and write secrets
 */
@Service
abstract class HashicorpVaultAbstractService extends AbstractService {

    @Autowired
    private OkHttpClient okHttpClient;

    public QuorumNetworkProperty.HashicorpVaultServerProperty vaultProperties() {
        return Optional.ofNullable(
            networkProperty().getHashicorpVaultServer()
        ).orElseThrow(() -> new RuntimeException("missing Hashicorp Vault server configuration"));
    }

    public Versioned<Map<String, Object>> readSecret(String secretEnginePath, String secretPath) {
        return vaultClient(secretEnginePath, vaultProperties()).get(secretPath);
    }

    public Versioned.Metadata writeSecret(String secretEnginePath, String secretPath, Map<String, Object> nameValuePairs) {
        return vaultClient(secretEnginePath, vaultProperties()).put(secretPath, nameValuePairs);
    }

    private VaultVersionedKeyValueOperations vaultClient(String secretEnginePath, QuorumNetworkProperty.HashicorpVaultServerProperty properties) {
        final VaultEndpoint vaultEndpoint;
        try {
            final URI uri = new URI(properties.getUrl());
            vaultEndpoint = VaultEndpoint.from(uri);
        } catch (URISyntaxException | NoSuchElementException | IllegalArgumentException e) {
            throw new RuntimeException("Provided Hashicorp Vault url is incorrectly formatted", e);
        }

        final OkHttpClient.Builder builder = okHttpClient.newBuilder();
        final SSLContext sslContext;
        try {
            sslContext = SSLContext.getInstance("TLS");
            final KeyManager[] keyManagers = loadKeyStore(
                Paths.get(properties.getTlsKeyStorePath()),
                properties.getTlsKeyStorePassword().toCharArray()
            );
            final TrustManager[] trustManagers = loadTrustStore(
                Paths.get(properties.getTlsTrustStorePath()),
                properties.getTlsTrustStorePassword().toCharArray()
            );

            sslContext.init(keyManagers, trustManagers, new SecureRandom());

            final X509TrustManager trustManager = (X509TrustManager) trustManagers[0];
            builder.sslSocketFactory(sslContext.getSocketFactory(), trustManager);
        } catch (Exception e) {
            throw new RuntimeException("Unable to set up SSL context for Hashicorp Vault client", e);
        }

        final OkHttpClient sslOkHttpClient = builder.build();

        final ClientHttpRequestFactory clientHttpRequestFactory = new OkHttp3ClientHttpRequestFactory(sslOkHttpClient);

        final ClientAuthentication clientAuthentication = new TokenAuthentication(properties.getAuthToken());
        final SessionManager sessionManager = new SimpleSessionManager(clientAuthentication);

        final VaultOperations vaultOperations = new VaultTemplate(vaultEndpoint, clientHttpRequestFactory, sessionManager);

        return new VaultVersionedKeyValueTemplate(vaultOperations, secretEnginePath);
    }

    private static KeyManager[] loadKeyStore(Path keyStoreFile, char[] keyStorePassword) throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException {

    final KeyStore keyStore = KeyStore.getInstance("JKS");

        try (InputStream in = Files.newInputStream(keyStoreFile)) {
            keyStore.load(in, keyStorePassword);
        }

        final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, keyStorePassword);

        return keyManagerFactory.getKeyManagers();
    }

    private static TrustManager[] loadTrustStore(Path trustStoreFile, char[] trustStorePassword) throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {

        final KeyStore trustStore = KeyStore.getInstance("JKS");

        try (InputStream in = Files.newInputStream(trustStoreFile)) {
            trustStore.load(in, trustStorePassword);
        }

        final TrustManagerFactory trustManagerFactory =
            TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);

        return trustManagerFactory.getTrustManagers();
    }
}

