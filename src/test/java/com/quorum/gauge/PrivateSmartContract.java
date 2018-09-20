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
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Service
public class PrivateSmartContract {
    private static final Logger logger = LoggerFactory.getLogger(PrivateSmartContract.class);

    @Autowired
    ContractService contractService;

    @Autowired
    TransactionService transactionService;

    @Step("Private smart contract is established between <source> and <target>.")
    public void setupContract(QuorumNode source, QuorumNode target) throws Exception {
        logger.debug("Setting up contract from {} to {}", source, target);
        Contract contract = contractService.createSimpleContract(source, target);

        DataStoreFactory.getScenarioDataStore().put("contract", contract);
    }

    @Step("Transaction Hash is returned.")
    public void verifyTransactionHash() {
        Contract c = (Contract) DataStoreFactory.getScenarioDataStore().get("contract");
        String transactionHash = c.getTransactionReceipt().orElseThrow(()-> new RuntimeException("no transaction receipt for contract")).getTransactionHash();
        Gauge.writeMessage("Transaction Hash is %s", transactionHash);

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

    @Step("<source> and <target> must have the same storage root.")
    public void verifyStorageRoot(QuorumNode source, QuorumNode target) {
        Contract c = (Contract) DataStoreFactory.getScenarioDataStore().get("contract");
        String sourceStorageRoot = contractService.getStorageRoot(source, c.getContractAddress());
        String targetStorageRoot = contractService.getStorageRoot(target, c.getContractAddress());

        assertThat(sourceStorageRoot).isEqualTo(targetStorageRoot);
    }

    @Step("<source> and <stranger> must not have the same storage root.")
    public void verifyStorageRootForNonParticipatedNode(QuorumNode source, QuorumNode stranger) {
        Contract c = (Contract) DataStoreFactory.getScenarioDataStore().get("contract");
        String sourceStorageRoot = contractService.getStorageRoot(source, c.getContractAddress());
        String targetStorageRoot = contractService.getStorageRoot(stranger, c.getContractAddress());

        assertThat(sourceStorageRoot).isNotEqualTo(targetStorageRoot);
    }

    @Step("Create <count> private smart contracts between <source> and <target>")
    public void createMultiple(int count, QuorumNode source, QuorumNode target) {
        List<Observable<? extends Contract>> allObservableContracts = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            allObservableContracts.add(contractService.createSimpleContractObservable(source, target).subscribeOn(Schedulers.io()));
        }
        List<Contract> contracts = Observable.zip(allObservableContracts, args -> {
            List<Contract> tmp = new ArrayList<>();
            for (Object o : args) {
                tmp.add((Contract) o);
            }
            return tmp;
        }).toBlocking().first();

        DataStoreFactory.getScenarioDataStore().put(String.format("%s_source_contract", source), contracts);
        DataStoreFactory.getScenarioDataStore().put(String.format("%s_target_contract", target), contracts);
    }

    @Step("<node> has received <expectedCount> transactions.")
    public void verifyNumberOfTransactions(QuorumNode node, int expectedCount) {
        List<Contract> sourceContracts = (List<Contract>) DataStoreFactory.getScenarioDataStore().get(String.format("%s_source_contract", node));
        List<Contract> targetContracts = (List<Contract>) DataStoreFactory.getScenarioDataStore().get(String.format("%s_target_contract", node));
        List<Contract> contracts = new ArrayList<>(sourceContracts);
        if (targetContracts != null) {
            contracts.addAll(targetContracts);
        }
        List<Observable<EthGetTransactionReceipt>> allObservableReceipts = new ArrayList<>();
        for (Contract c : contracts) {
            String txHash = c.getTransactionReceipt().orElseThrow(() -> new RuntimeException("no receipt for contract")).getTransactionHash();
            allObservableReceipts.add(transactionService.getTransactionReceiptObservable(node, txHash).subscribeOn(Schedulers.io()));
        }
        Integer actualCount = Observable.zip(allObservableReceipts, args -> args.length).toBlocking().first();

        assertThat(actualCount).isEqualTo(expectedCount);
    }
}
