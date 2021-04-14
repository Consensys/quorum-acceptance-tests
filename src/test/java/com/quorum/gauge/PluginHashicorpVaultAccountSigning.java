package com.quorum.gauge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quorum.gauge.common.QuorumNetworkProperty;
import com.quorum.gauge.core.AbstractSpecImplementation;
import com.quorum.gauge.ext.accountplugin.HashicorpAccountConfigFileJson;
import com.quorum.gauge.ext.accountplugin.HashicorpNewAccountJson;
import com.quorum.gauge.services.AccountService;
import com.thoughtworks.gauge.Step;
import org.springframework.stereotype.Service;
import org.springframework.vault.support.Versioned;
import org.web3j.protocol.admin.methods.response.BooleanResponse;
import org.web3j.protocol.core.methods.request.Transaction;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@Service
public class PluginHashicorpVaultAccountSigning extends AbstractSpecImplementation {

    @Step("<node> has account 0x6038dc01869425004ca0b8370f6c81cf464213b3")
    public void verifyNodeHasAccount(QuorumNetworkProperty.Node node) {
        List<AccountService.Wallet> wallets = accountService
            .personalListWallets(node)
            .blockingFirst()
            .getWallets();

        assertThat(wallets).isNotNull();

        List<AccountService.Wallet> filteredWallets = wallets.stream()
            .filter(w -> w.contains("0x6038dc01869425004ca0b8370f6c81cf464213b3"))
            .collect(Collectors.toList());

        assertThat(filteredWallets).hasSize(1);
        assertThat(filteredWallets.get(0).getFailure()).isNull();
    }

    @Step("Add Hashicorp Vault account 0x6038dc01869425004ca0b8370f6c81cf464213b3 to <node> with secret engine path <secretEnginePath> and secret path <secretPath>")
    public void createHashicorpVaultAccount(QuorumNetworkProperty.Node node, String secretEnginePath, String secretPath) {
        // if the account is already added do nothing
        List<AccountService.Wallet> wallets = accountService
            .personalListWallets(node)
            .blockingFirst()
            .getWallets();

        List<AccountService.Wallet> filteredWallets = wallets.stream()
            .filter(w -> w.contains("0x6038dc01869425004ca0b8370f6c81cf464213b3"))
            .collect(Collectors.toList());

        if (filteredWallets.size() != 0) {
            return;
        }

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
        acctJson.setSecretName(secretPath);
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

    @Step("<node> gets the expected result when signing a known transaction with account 0x6038dc01869425004ca0b8370f6c81cf464213b3")
    public void compareSignTransaction(QuorumNetworkProperty.Node node) {
        final String expected = "0xf84f80808347b76080808300000025a050b4ca82805d053fea92514fabe2ecd8518c90f2064451d68cabac0b2bd2f04ea03aa1355d44807826e8152a6a6c59abc8d9459680086fe4df4e678f58952f1022";

        final Transaction toSign = hashicorpVaultSigningService.toSign(BigInteger.ZERO, "0x6038dc01869425004ca0b8370f6c81cf464213b3");
        final Optional<String> raw = transactionService
            .personalSignTransaction(node, toSign, "")
            .blockingFirst()
            .getRaw();

        assertThat(raw.isPresent()).isTrue();
        assertThat(raw.get()).isEqualTo(expected);
    }

    @Step("<node> gets the expected result when signing known arbitrary data with account 0x6038dc01869425004ca0b8370f6c81cf464213b3")
    public void compareSign(QuorumNetworkProperty.Node node) {
        final String expected = "0xead3d9a19ac3fb4003c50f2d85e27072dac4e78b77903d6061d8619ba671db0551ab3e72790a7d0c722a3c6ee070a75fa08bb04b7d3ae2ca0be1963bbbdf94c41c";

        final String signed = transactionService
            .personalSign(node, "0xaaaaaa", "0x6038dc01869425004ca0b8370f6c81cf464213b3", "")
            .blockingFirst()
            .getResult();
        assertThat(signed).isNotNull();
        assertThat(signed).isEqualTo(expected);
    }
}
