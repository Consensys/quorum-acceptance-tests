package com.quorum.gauge;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quorum.gauge.common.QuorumNetworkProperty;
import com.quorum.gauge.core.AbstractSpecImplementation;
import com.quorum.gauge.ext.accountplugin.HashicorpAccountConfigFileJson;
import com.quorum.gauge.ext.accountplugin.HashicorpNewAccountJson;
import com.quorum.gauge.services.AccountService;
import com.thoughtworks.gauge.Step;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.vault.support.Versioned;
import org.web3j.protocol.admin.methods.response.BooleanResponse;
import org.web3j.protocol.core.methods.request.Transaction;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@Service
public class PluginHashicorpVaultAccountSigning extends AbstractSpecImplementation {

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginHashicorpVaultAccountSigning.class);

    @Step("<node> does not have account 0x6038dc01869425004ca0b8370f6c81cf464213b3")
    public void verifyNodeDoesNotHaveAccount(QuorumNetworkProperty.Node node) {
        List<AccountService.Wallet> wallets = null;
        try {
            wallets = accountService.personalListWallets(node);
        } catch (IOException e) {
            fail("unable to list accounts for node %s: %s", node.toString(), e.getMessage());
        }
        assertThat(wallets).isNotNull();

        List<AccountService.Wallet> filteredWallets = wallets.stream()
            .filter(w -> w.contains("0x6038dc01869425004ca0b8370f6c81cf464213b3"))
            .collect(Collectors.toList());

        assertThat(filteredWallets).isEmpty();
    }

    @Step("<node> has account 0x6038dc01869425004ca0b8370f6c81cf464213b3")
    public void verifyNodeHasAccount(QuorumNetworkProperty.Node node) {
        List<AccountService.Wallet> wallets = null;
        try {
            wallets = accountService.personalListWallets(node);
        } catch (IOException e) {
            fail("unable to list accounts for node %s: %s", node.toString(), e.getMessage());
        }
        assertThat(wallets).isNotNull();

        LOGGER.error("CHRISSY - personal_listWallets:");
        wallets.forEach(w -> LOGGER.error(w.getUrl()));

        List<AccountService.Wallet> filteredWallets = wallets.stream()
            .filter(w -> w.contains("0x6038dc01869425004ca0b8370f6c81cf464213b3"))
            .collect(Collectors.toList());

        assertThat(filteredWallets).hasSize(1);
        assertThat(filteredWallets.get(0).getFailure()).isNull();
    }

    @Step("Add Hashicorp Vault account 0x6038dc01869425004ca0b8370f6c81cf464213b3 to <node> with secret engine path <secretEnginePath> and secret path <secretPath>")
    public void createHashicorpVaultAccount(QuorumNetworkProperty.Node node, String secretEnginePath, String secretPath) {
        // write to Vault
        Map<String, Object> toWrite = new HashMap<>();
        toWrite.put("6038dc01869425004ca0b8370f6c81cf464213b3", "1fe8f1ad4053326db20529257ac9401f2e6c769ef1d736b8c2f5aba5f787c72b");
        Versioned.Metadata metadata = hashicorpVaultSigningService.writeSecret(secretEnginePath, secretPath, toWrite);

        // create account config file
        String pluginAcctDirPath = hashicorpVaultSigningService.vaultProperties().getNodeAcctDirs().get(node.getName()).getPluginAcctDir();
        File acctDir = new File(pluginAcctDirPath);
        File acctConfigFile = null;
        try {
            acctConfigFile = File.createTempFile("6038dc01869425004ca0b8370f6c81cf464213b3--", ".json", acctDir);
        } catch (IOException e) {
            fail("unable to create account config file: %s", e.getMessage());
        }
        assertThat(acctConfigFile).isNotNull();
        assertThat(acctConfigFile).exists();

        HashicorpNewAccountJson acctJson = new HashicorpNewAccountJson();
        acctJson.setSecretEnginePath(secretEnginePath);
        acctJson.setSecretPath(secretPath);
        acctJson.setSecretVersion(metadata.getVersion().getVersion());

        HashicorpAccountConfigFileJson fileContents = new HashicorpAccountConfigFileJson();
        fileContents.setAddress("6038dc01869425004ca0b8370f6c81cf464213b3");
        fileContents.setVaultAccount(acctJson);
        fileContents.setVersion(1);

        try {
            FileOutputStream out = new FileOutputStream(acctConfigFile);
            new ObjectMapper().writeValue(out, fileContents);
            out.close();
        } catch (IOException e) {
            fail("unable to write to account config file: %s", e.getMessage());
        }

        // reload plugin so that new account is available to node
        BooleanResponse res = rpcService.call(
            node,
            "admin_reloadPlugin",
            Collections.singletonList("account"),
            BooleanResponse.class
        ).blockingFirst();
        assertThat(res.success()).isTrue();
    }

    @Step("Add keystore account 0x6038dc01869425004ca0b8370f6c81cf464213b3 with no password to <node>")
    public void createKeystoreAccount(QuorumNetworkProperty.Node node) {
        String keystoreFileContentJson = "{" +
            "\"address\":\"6038dc01869425004ca0b8370f6c81cf464213b3\"," +
            "\"crypto\":{" +
            "\"cipher\":\"aes-128-ctr\"," +
            "\"ciphertext\":\"865a68f1f8795e0b848d25d3366d9dad73c9c5bae2005a19e95467db17e7a76e\"," +
            "\"cipherparams\":{" +
            "\"iv\":\"1a52502748a978d3e66507fa2d1c26a9\"" +
            "}," +
            "\"kdf\":\"scrypt\"," +
            "\"kdfparams\":{" +
            "\"dklen\":32," +
            "\"n\":262144," +
            "\"p\":1," +
            "\"r\":8," +
            "\"salt\":\"6b329ae63a7d5344012d5a72f5953acb45b98e152a80bd5a3026c4422ec595ee\"" +
            "}," +
            "\"mac\":\"e543e25d52f5d7e9925830ae8d7c58f234660d3a1f01d56a5b6860557292247f\"" +
            "}," +
            "\"id\":\"fe708ca7-978b-407d-a41d-146f2b710eef\"," +
            "\"version\":3" +
            "}";

        ObjectMapper mapper = new ObjectMapper();
        JsonNode keystoreFileContent = null;
        try {
            keystoreFileContent = mapper.readTree(keystoreFileContentJson);
        } catch (JsonProcessingException e) {
            fail("unable to read keystore file contents json: %s", e.getMessage());
        }

        String keystoreAcctDirPath = hashicorpVaultSigningService.vaultProperties().getNodeAcctDirs().get(node.getName()).getKeystoreAcctDir();
        LOGGER.error("CHRISSY - writing to keystore account directory {}", keystoreAcctDirPath);
        File keystoreFile = null;
        try {
            keystoreFile = File.createTempFile("6038dc01869425004ca0b8370f6c81cf464213b3--", ".json", new File(keystoreAcctDirPath));
            LOGGER.error("CHRISSY - writing keystore account file {}", keystoreFile.getAbsolutePath());
            mapper.writeValue(keystoreFile, keystoreFileContent);
        } catch (IOException e) {
            fail("unable to create keystore file: %s", e.getMessage());
        }
        assertThat(keystoreFile).exists();
        LOGGER.error("CHRISSY - keystorefile exists = true");

        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            fail("error whilst waiting for node to update keystore accounts: %s", e.getMessage());
        }
    }

    @Step("Remove Hashicorp Vault account 0x6038dc01869425004ca0b8370f6c81cf464213b3 from <node>")
    public void removeHashicorpVaultAccount(QuorumNetworkProperty.Node node) {
        String pluginAcctDirPath = hashicorpVaultSigningService.vaultProperties().getNodeAcctDirs().get(node.getName()).getPluginAcctDir();
        Path acctDir = Paths.get(pluginAcctDirPath);

        Stream<Path> acctFiles = null;
        try {
            acctFiles = Files.list(acctDir);
        } catch (IOException e) {
            fail("unable to list files in account directory: %s", e.getMessage());
        }
        assertThat(acctFiles).isNotNull();

        acctFiles.filter(p -> p.getFileName().toString().contains("6038dc01869425004ca0b8370f6c81cf464213b3"))
            .forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException e) {
                    fail("unable to delete file %s: %s", p.toString(), e.getMessage());
                }
            });

        // reload plugin so that the account is no longer available to node
        BooleanResponse res = rpcService.call(
            node,
            "admin_reloadPlugin",
            Collections.singletonList("account"),
            BooleanResponse.class
        ).blockingFirst();
        assertThat(res.success()).isTrue();
    }

    @Step("Remove keystore account 0x6038dc01869425004ca0b8370f6c81cf464213b3 from <node>")
    public void removeKeystoreAccount(QuorumNetworkProperty.Node node) {
        String keystoreAcctDirPath = hashicorpVaultSigningService.vaultProperties().getNodeAcctDirs().get(node.getName()).getKeystoreAcctDir();

        Stream<Path> acctFiles = null;
        try {
            acctFiles = Files.list(Paths.get(keystoreAcctDirPath));
        } catch (IOException e) {
            fail("unable to list files in keystore directory: %s", e.getMessage());
        }
        assertThat(acctFiles).isNotNull();

        acctFiles.filter(p -> p.getFileName().toString().contains("6038dc01869425004ca0b8370f6c81cf464213b3"))
            .forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException e) {
                    fail("unable to delete file %s: %s", p.toString(), e.getMessage());
                }
            });

        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            fail("error whilst waiting for node to update keystore accounts: %s", e.getMessage());
        }
    }

    @Step("<node> gets the expected result when signing a known transaction with account 0x6038dc01869425004ca0b8370f6c81cf464213b3")
    public void compareSignTransaction(QuorumNetworkProperty.Node node) {
        try {
            final String expected = "0xf84f80808347b76080808300000038a0eafd7c6c274313283754fbc32cb7f6ef2b0510812f185e3c387ab5ed04be5406a03e7934d715a40479df9820987b4bb2ed44404d0181371e57ba878045e39770f9";

            final Transaction toSign = hashicorpVaultSigningService.toSign(node, "0x6038dc01869425004ca0b8370f6c81cf464213b3");
            final Map<String, Object> result = transactionService.personalSignTransaction(node, toSign, "");

            assertThat(result).isNotNull();
            final String signed = (String) result.get("raw");
            assertThat(signed).isNotNull();

            assertThat(signed).isEqualTo(expected);
        } catch (IOException e) {
            fail("unable to sign transaction: %s", e.getMessage());
        }
    }

    @Step("<node> gets the expected result when signing known arbitrary data with account 0x6038dc01869425004ca0b8370f6c81cf464213b3")
    public void compareSign(QuorumNetworkProperty.Node node) {
        try {
            final String expected = "0xead3d9a19ac3fb4003c50f2d85e27072dac4e78b77903d6061d8619ba671db0551ab3e72790a7d0c722a3c6ee070a75fa08bb04b7d3ae2ca0be1963bbbdf94c41c";

            final String signed = transactionService.personalSign(node, "0xaaaaaa", "0x6038dc01869425004ca0b8370f6c81cf464213b3", "");
            assertThat(signed).isNotNull();
            assertThat(signed).isEqualTo(expected);
        } catch (IOException e) {
            fail("unable to sign transaction: %s", e.getMessage());
        }
    }
}
