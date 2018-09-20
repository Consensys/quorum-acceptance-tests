package com.quorum.gauge;

import com.quorum.gauge.common.QuorumNode;
import com.quorum.gauge.services.ContractService;
import com.quorum.gauge.services.TransactionService;
import com.thoughtworks.gauge.Gauge;
import com.thoughtworks.gauge.Step;
import com.thoughtworks.gauge.datastore.DataStoreFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Service
public class PrivateContract {
    private static final Logger logger = LoggerFactory.getLogger(PrivateContract.class);

    @Autowired
    ContractService contractService;

    @Autowired
    TransactionService transactionService;

    @Step("Private contract is established between <source> and <target>.")
    public void setupContract(QuorumNode source, QuorumNode target) throws Exception {
        logger.debug("Setting up contract from {} to {}", source, target);
        Contract contract = contractService.createSimpleContract(source, target);

        DataStoreFactory.getScenarioDataStore().put("contract", contract);
    }

    @Step("Transaction Hash is returned.")
    public void verifyTransactionHash() {
        Contract c = (Contract) DataStoreFactory.getScenarioDataStore().get("contract");
        String transactionHash = c.getTransactionReceipt().orElseThrow(()-> new RuntimeException("no transaction receipt for contract")).getTransactionHash();
        Gauge.writeMessage("Transaction Hash is {}", transactionHash);

        assertThat(transactionHash).isNotBlank();

        DataStoreFactory.getScenarioDataStore().put("transactionHash", transactionHash);
    }

    @Step("Transaction Receipt is present in <node>.")
    public void verifyTransactionReceipt(QuorumNode node) {
        String transactionHash = (String) DataStoreFactory.getScenarioDataStore().get("transactionHash");
        Optional<TransactionReceipt> receipt = transactionService.getTransactionReceipt(node, transactionHash);

        assertThat(receipt.isPresent()).isTrue();
    }

    @Step("<source> and <target> see the same value.")
    public void verifyPrivacyWithParticipatedNodes(QuorumNode source, QuorumNode target) {
        Contract c = (Contract) DataStoreFactory.getScenarioDataStore().get("contract");
        int sourceValue = contractService.readSimpleContractValue(source, c.getContractAddress());
        int targetValue = contractService.readSimpleContractValue(target, c.getContractAddress());

        assertThat(sourceValue).isEqualTo(targetValue);
    }

    @Step("<stranger> must not be able to see the same value.")
    public void verifyPrivacyWithNonParticipatedNode(QuorumNode stranger) {
        Contract c = (Contract) DataStoreFactory.getScenarioDataStore().get("contract");
        int strangerValue = contractService.readSimpleContractValue(stranger, c.getContractAddress());

        assertThat(strangerValue).isEqualTo(0);
    }

    @Step("<source> updates to new value <newValue> with <target>.")
    public void updateNewValue(QuorumNode source, int newValue, QuorumNode target) {
        Contract c = (Contract) DataStoreFactory.getScenarioDataStore().get("contract");
        TransactionReceipt receipt = contractService.updateSimpleContract(source, target, c.getContractAddress(), newValue);

        assertThat(receipt.getTransactionHash()).isNotBlank();
    }
}
