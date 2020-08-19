package com.quorum.gauge.services;

import com.quorum.gauge.common.QuorumNetworkProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.authentication.SessionManager;
import org.springframework.vault.authentication.SimpleSessionManager;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.ClientHttpRequestFactoryFactory;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.core.VaultOperations;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.core.VaultVersionedKeyValueOperations;
import org.springframework.vault.core.VaultVersionedKeyValueTemplate;
import org.springframework.vault.support.ClientOptions;
import org.springframework.vault.support.SslConfiguration;
import org.springframework.vault.support.Versioned;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Creates a client to the Hashicorp Vault server and provides methods to read and write secrets
 */
@Service
abstract class HashicorpVaultAbstractService extends AbstractService {

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
        VaultEndpoint vaultEndpoint;

        try {
            URI uri = new URI(properties.getUrl());
            vaultEndpoint = VaultEndpoint.from(uri);
        } catch (URISyntaxException | NoSuchElementException | IllegalArgumentException e) {
            throw new RuntimeException("Provided Hashicorp Vault url is incorrectly formatted", e);
        }

        SslConfiguration sslConfiguration = sslConfiguration(
            properties.getTlsKeyStorePath(),
            properties.getTlsKeyStorePassword(),
            properties.getTlsTrustStorePath(),
            properties.getTlsTrustStorePassword()
        );
        ClientOptions clientOptions = new ClientOptions();
        ClientHttpRequestFactory clientHttpRequestFactory = ClientHttpRequestFactoryFactory.create(clientOptions, sslConfiguration);

        ClientAuthentication clientAuthentication = new TokenAuthentication(properties.getAuthToken());

        SessionManager sessionManager = new SimpleSessionManager(clientAuthentication);
        VaultOperations vaultOperations = new VaultTemplate(vaultEndpoint, clientHttpRequestFactory, sessionManager);

        return new VaultVersionedKeyValueTemplate(vaultOperations, secretEnginePath);
    }

    private SslConfiguration sslConfiguration(String tlsKeyStorePath, String tlsKeyStorePassword, String tlsTrustStorePath, String tlsTrustStorePassword) {
        Path tlsKeyStore = Paths.get(tlsKeyStorePath);
        Path tlsTrustStore = Paths.get(tlsTrustStorePath);

        Resource clientKeyStore = new FileSystemResource(tlsKeyStore.toFile());
        Resource clientTrustStore = new FileSystemResource(tlsTrustStore.toFile());

        SslConfiguration.KeyStoreConfiguration keyStoreConfiguration = SslConfiguration.KeyStoreConfiguration.of(
            clientKeyStore,
            tlsKeyStorePassword.toCharArray()
        );

        SslConfiguration.KeyStoreConfiguration trustStoreConfiguration = SslConfiguration.KeyStoreConfiguration.of(
            clientTrustStore,
            tlsTrustStorePassword.toCharArray()
        );

        return new SslConfiguration(keyStoreConfiguration, trustStoreConfiguration);
    }
}

