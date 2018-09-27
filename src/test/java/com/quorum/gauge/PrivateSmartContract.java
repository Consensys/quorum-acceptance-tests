package com.quorum.gauge;

import com.quorum.gauge.common.QuorumNode;
import com.quorum.gauge.ext.EthGetQuorumPayload;
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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Service
@SuppressWarnings("unchecked")
public class PrivateSmartContract {
    private static final Logger logger = LoggerFactory.getLogger(PrivateSmartContract.class);

    @Autowired
    ContractService contractService;

    @Autowired
    TransactionService transactionService;

    @Step("Deploy a simple smart contract with initial value <initialValue> in <source>'s default account and it's private for <target>, named this contract as <contractName>.")
    public void setupContract(int initialValue, QuorumNode source, QuorumNode target, String contractName) {
        logger.debug("Setting up contract from {} to {}", source, target);
        Contract contract = contractService.createSimpleContract(initialValue, source, target).toBlocking().first();

        DataStoreFactory.getSpecDataStore().put(contractName, contract);
    }

    @Step("Transaction Hash is returned for <contractName>.")
    public void verifyTransactionHash(String contractName) {
        Contract c = (Contract) DataStoreFactory.getSpecDataStore().get(contractName);
        String transactionHash = c.getTransactionReceipt().orElseThrow(()-> new RuntimeException("no transaction receipt for contract")).getTransactionHash();
        Gauge.writeMessage("Transaction Hash is %s", transactionHash);

        assertThat(transactionHash).isNotBlank();

        DataStoreFactory.getScenarioDataStore().put("transactionHash", transactionHash);
    }

    @Step("Transaction Receipt is present in <node> for <contractName>.")
    public void verifyTransactionReceipt(QuorumNode node, String contractName) {
        String transactionHash = (String) DataStoreFactory.getScenarioDataStore().get("transactionHash");
        Optional<TransactionReceipt> receipt = transactionService.getTransactionReceipt(node, transactionHash).toBlocking().first().getTransactionReceipt();

        assertThat(receipt.isPresent()).isTrue();
        assertThat(receipt.get().getBlockNumber()).isNotEqualTo(BigInteger.valueOf(0));
    }

    @Step("<contractName> stored in <source> and <target> must have the same storage root.")
    public void verifyStorageRoot(String contractName, QuorumNode source, QuorumNode target) {
        Contract c = (Contract) DataStoreFactory.getSpecDataStore().get(contractName);
        Observable.zip(
                contractService.getStorageRoot(source, c.getContractAddress()).subscribeOn(Schedulers.io()),
                contractService.getStorageRoot(target, c.getContractAddress()).subscribeOn(Schedulers.io()),
                (s, t) -> assertThat(s).isEqualTo(t)
        );
    }

    @Step("<contractName> stored in <source> and <stranger> must not have the same storage root.")
    public void verifyStorageRootForNonParticipatedNode(String contractName, QuorumNode source, QuorumNode stranger) {
        Contract c = (Contract) DataStoreFactory.getSpecDataStore().get(contractName);
        Observable.zip(
                contractService.getStorageRoot(source, c.getContractAddress()).subscribeOn(Schedulers.io()),
                contractService.getStorageRoot(stranger, c.getContractAddress()).subscribeOn(Schedulers.io()),
                (s, t) -> assertThat(s).isNotEqualTo(t)
        );
    }

    @Step("<contractName>'s `get()` function execution in <node> returns <expectedValue>.")
    public void verifyPrivacyWithParticipatedNodes(String contractName, QuorumNode node, int expectedValue) {
        Contract c = (Contract) DataStoreFactory.getSpecDataStore().get(contractName);
        int actualValue = contractService.readSimpleContractValue(node, c.getContractAddress());

        assertThat(actualValue).isEqualTo(expectedValue);
    }

    @Step("Execute <contractName>'s `set()` function with new value <newValue> in <source> and it's private for <target>.\n")
    public void updateNewValue(String contractName, int newValue, QuorumNode source, QuorumNode target) {
        Contract c = (Contract) DataStoreFactory.getSpecDataStore().get(contractName);
        TransactionReceipt receipt = contractService.updateSimpleContract(source, target, c.getContractAddress(), newValue).toBlocking().first();

        assertThat(receipt.getTransactionHash()).isNotBlank();
        assertThat(receipt.getBlockNumber()).isNotEqualTo(BigInteger.valueOf(0));
    }


    @Step("Deploy <count> private smart contracts between a default account in <source> and a default account in <target>")
    public void createMultiple(int count, QuorumNode source, QuorumNode target) {
        int arbitraryValue = 10;
        List<Observable<? extends Contract>> allObservableContracts = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            allObservableContracts.add(contractService.createSimpleContract(arbitraryValue, source, target).subscribeOn(Schedulers.io()));
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
            allObservableReceipts.add(transactionService.getTransactionReceipt(node, txHash).retry(10).subscribeOn(Schedulers.io()));
        }
        Integer actualCount = Observable.zip(allObservableReceipts, args -> {
            int count  = 0;
            for (Object o : args) {
                EthGetTransactionReceipt r  = (EthGetTransactionReceipt) o;
                if (r.getTransactionReceipt().isPresent() && r.getTransactionReceipt().get().getBlockNumber().compareTo(BigInteger.valueOf(0)) != 0) {
                    count++;
                }
            }
            return count;
        }).toBlocking().first();

        assertThat(actualCount).isEqualTo(expectedCount);
    }

    @Step("<contractName>'s payload is retrievable from <node>.")
    public void verifyPrivateContractPayloadIsAccessible(String contractName, QuorumNode node) {
        Contract c = (Contract) DataStoreFactory.getSpecDataStore().get(contractName);
        EthGetQuorumPayload payload = transactionService.getPrivateTransactionPayload(node, c.getTransactionReceipt().get().getTransactionHash()).toBlocking().first();

        assertThat(payload.getResult()).isNotEqualTo("0x");
    }

    @Step("<contractName>'s payload is not retrievable from <node>.")
    public void verifyPrivateContractPayloadIsNotAccessible(String contractName, QuorumNode node) {
        Contract c = (Contract) DataStoreFactory.getSpecDataStore().get(contractName);
        EthGetQuorumPayload payload = transactionService.getPrivateTransactionPayload(node, c.getTransactionReceipt().get().getTransactionHash()).toBlocking().first();

        assertThat(payload.getResult()).isEqualTo("0x");
    }
}
