package com.quorum.gauge;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quorum.gauge.common.QuorumNetworkProperty;
import com.quorum.gauge.core.AbstractSpecImplementation;
import com.quorum.gauge.ext.accountplugin.HashicorpAccountConfigFileJson;
import com.quorum.gauge.ext.accountplugin.HashicorpNewAccountJson;
import com.quorum.gauge.ext.accountplugin.HashicorpNewAccountResponse;
import com.thoughtworks.gauge.Step;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.vault.support.Versioned;
import org.web3j.protocol.admin.methods.response.BooleanResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@Service
public class PluginHashicorpVaultAccountCreation extends AbstractSpecImplementation {

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginHashicorpVaultAccountCreation.class);

    @Step("Calling `plugin@account_newAccount` API in <node> with single parameter <param> returns the new account address and Vault secret URL")
    public void newAccount(QuorumNetworkProperty.Node node, String param) {
        HashicorpNewAccountJson newAccountJson = null;
        try {
            newAccountJson = new ObjectMapper().readValue(param, HashicorpNewAccountJson.class);
        } catch (JsonProcessingException e) {
            fail("invalid newAccount json config: %s", e.getMessage());
        }

        HashicorpNewAccountResponse res = rpcService.call(
            node,
            "plugin@account_newAccount",
            Collections.singletonList(newAccountJson),
            HashicorpNewAccountResponse.class
        ).blockingFirst();

        if (res.hasError()) {
            fail("error making API call: [%d] %s",
                res.getError().getCode(),
                res.getError().getMessage()
            );
        }

        Map<String, String> result = res.getResult();
        assertThat(result).containsOnlyKeys("address", "url");
        assertThat(result.get("address")).isNotEmpty();
        assertThat(result.get("url")).isNotEmpty();

        hashicorpVaultAccountCreationService.setNewAccountJson(newAccountJson);
        hashicorpVaultAccountCreationService.setNewAccountAddress(result.get("address"));
        hashicorpVaultAccountCreationService.setNewAccountUrl(result.get("url"));
    }

    // kvEngineName is a plugin config-level property.  This is set in the plugin config json when using the terraform plugins network.
    @Step("The account address and a private key exist at kv secret engine with name <kvEngineName> and secret name <secretName>")
    public void verifyAccountCreated(String kvEngineName, String secretName) {
        if (!hashicorpVaultAccountCreationService.isNewAccountContextInitialised()) {
            fail("test context not initialised");
        }

        Versioned<Map<String, Object>> response = hashicorpVaultAccountCreationService.readSecret(kvEngineName, secretName);

        int newAccountVersion = Integer.parseInt(
            hashicorpVaultAccountCreationService.getNewAccountUrl().split("version=")[1]
        );
        assertThat(response.getVersion().getVersion()).isEqualTo(newAccountVersion);

        Set<Map.Entry<String, Object>> entrySet = response.getData().entrySet();
        assertThat(entrySet).hasSize(1);

        Map.Entry<String, Object> entry = entrySet.iterator().next();
        String wantAddress = hashicorpVaultAccountCreationService.getNewAccountAddress().replaceFirst("0x", "");
        assertThat(entry.getKey()).isEqualTo(wantAddress);
        assertThat(entry.getValue()).isNotNull();
    }

    @Step("File is created in <node>'s account config directory")
    public void verifyFileCreated(QuorumNetworkProperty.Node node) {
        if (!hashicorpVaultAccountCreationService.isNewAccountContextInitialised()) {
            fail("test context not initialised");
        }

        String acctDirPath = hashicorpVaultAccountCreationService.vaultProperties().getNodeAcctDirs().get(node.getName()).getPluginAcctDir();
        Stream<Path> pathStream = Stream.empty();
        try {
            pathStream = Files.list(Paths.get(acctDirPath));
        } catch (IOException e) {
            fail("unable to check account config directory: %s", e.getMessage());
        }

        List<Path> newAccountFiles = pathStream
            .map(Path::toString)
            .filter(s -> s.endsWith(hashicorpVaultAccountCreationService.getNewAccountAddress().replaceFirst("0x", "")))
            .map(s -> Paths.get(s))
            .collect(Collectors.toList());

        assertThat(newAccountFiles).hasSize(1);

        byte[] newAccountConfigBytes = null;
        try {
            newAccountConfigBytes = Files.readAllBytes(newAccountFiles.get(0));
        } catch (IOException e) {
            fail("unable to read contents of account config file: %s", e.getMessage());
        }

        assertThat(newAccountConfigBytes).isNotNull();
        assertThat(newAccountConfigBytes).isNotEmpty();

        HashicorpAccountConfigFileJson accountConfigFileJson = null;
        try {
            accountConfigFileJson = new ObjectMapper().readValue(newAccountConfigBytes, HashicorpAccountConfigFileJson.class);
        } catch (IOException e) {
            fail("invalid account file json config: %s", e.getMessage());
        }

        assertThat(accountConfigFileJson).isNotNull();

        assertThat(accountConfigFileJson.getAddress()).isEqualTo(hashicorpVaultAccountCreationService.getNewAccountAddress().replaceFirst("0x", ""));
        assertThat(accountConfigFileJson.getVaultAccount().getSecretName()).isEqualTo(hashicorpVaultAccountCreationService.getNewAccountJson().getSecretName());
    }

    @Step("Calling `plugin@account_importRawKey` API in <node> with parameters <rawKey> and <param> returns the imported account address and Vault secret URL")
    public void importRawKey(QuorumNetworkProperty.Node node, String rawKey, String param) {
        HashicorpNewAccountJson newAccountJson = null;
        try {
            newAccountJson = new ObjectMapper().readValue(param, HashicorpNewAccountJson.class);
        } catch (JsonProcessingException e) {
            fail("invalid newAccount json config: %s", e.getMessage());
        }

        List<Object> params = new ArrayList<>();
        params.add(rawKey);
        params.add(newAccountJson);

        HashicorpNewAccountResponse res = rpcService.call(
            node,
            "plugin@account_importRawKey",
            params,
            HashicorpNewAccountResponse.class
        ).blockingFirst();

        if (res.hasError()) {
            fail("error making API call: [%d] %s",
                res.getError().getCode(),
                res.getError().getMessage()
            );
        }

        Map<String, String> result = res.getResult();
        assertThat(result).containsOnlyKeys("address", "url");
        assertThat(result.get("address")).isNotEmpty();
        assertThat(result.get("url")).isNotEmpty();

        hashicorpVaultAccountCreationService.setNewAccountJson(newAccountJson);
        hashicorpVaultAccountCreationService.setNewAccountAddress(result.get("address"));
        hashicorpVaultAccountCreationService.setNewAccountUrl(result.get("url"));
    }

    // kvEngineName is a plugin config-level property.  This is set in the plugin config json when using the terraform plugins network.
    @Step("The account address and private key <rawKey> exist at kv secret engine with name <kvEngineName> and secret name <secretName>")
    public void verifyAccountImported(String privateKey, String kvEngineName, String secretName) {
        if (!hashicorpVaultAccountCreationService.isNewAccountContextInitialised()) {
            fail("test context not initialised");
        }

        Versioned<Map<String, Object>> response = hashicorpVaultAccountCreationService.readSecret(kvEngineName , secretName);

        int newAccountVersion = Integer.parseInt(
            hashicorpVaultAccountCreationService.getNewAccountUrl().split("version=")[1]
        );
        assertThat(response.getVersion().getVersion()).isEqualTo(newAccountVersion);

        Set<Map.Entry<String, Object>> entrySet = response.getData().entrySet();
        assertThat(entrySet).hasSize(1);

        Map.Entry<String, Object> entry = entrySet.iterator().next();
        String wantAddress = hashicorpVaultAccountCreationService.getNewAccountAddress().replaceFirst("0x", "");
        assertThat(entry.getKey()).isEqualTo(wantAddress);
        assertThat(entry.getValue()).isEqualTo(privateKey);
    }

    @Step("Delete all files in <node>'s account config directory")
    public void deleteFiles(QuorumNetworkProperty.Node node) {
        String acctDirPath = hashicorpVaultAccountCreationService.vaultProperties().getNodeAcctDirs().get(node.getName()).getPluginAcctDir();
        Stream<Path> pathStream = Stream.empty();
        try {
            pathStream = Files.list(Paths.get(acctDirPath));
        } catch (IOException e) {
            fail("unable to check account config directory: %s", e.getMessage());
        }
        pathStream.forEach(p -> {
            try {
                Files.delete(p);
            } catch (IOException e) {
                fail("unable to delete file in account config directory: %s", e.getMessage());
            }
        });
        // reload plugin so any deleted accounts are no longer available to node
        BooleanResponse res = rpcService.call(
            node,
            "admin_reloadPlugin",
            Collections.singletonList("account"),
            BooleanResponse.class
        ).blockingFirst();
        assertThat(res.success()).isTrue();
    }
}

