package com.quorum.gauge;

import com.quorum.gauge.common.PrivacyFlag;
import com.quorum.gauge.common.QuorumNode;
import com.quorum.gauge.core.AbstractSpecImplementation;
import com.quorum.gauge.ext.contractextension.*;
import com.quorum.gauge.services.ExtensionService;
import com.thoughtworks.gauge.Step;
import com.thoughtworks.gauge.datastore.DataStore;
import com.thoughtworks.gauge.datastore.DataStoreFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@Service
public class ContractExtension extends AbstractSpecImplementation {

    private final ExtensionService extensionService;

    @Autowired
    public ContractExtension(final ExtensionService extensionService) {
        this.extensionService = Objects.requireNonNull(extensionService);
    }

    @Step("Create a <privacyFlag> extension to <newNode> from <creator> with <voters> as voters for contract <contractName>")
    public void createNewContractExtensionProposition(final PrivacyFlag privacyFlag,
                                                      final QuorumNode newNode,
                                                      final QuorumNode creator,
                                                      final String allVoters,
                                                      final String contractName) throws InterruptedException {

        DataStoreFactory.getScenarioDataStore().put("privacyFlag", privacyFlag);

        final List<QuorumNode> voters = Stream
            .of(allVoters.split(","))
            .map(QuorumNode::valueOf)
            .collect(Collectors.toList());

        final Set<QuorumNode> allNodes = Stream
            .concat(voters.stream(), Stream.of(newNode, creator))
            .collect(Collectors.toSet());
        DataStoreFactory.getScenarioDataStore().put("extensionAllNodes", allNodes);

        final Contract existingContract = mustHaveValue(DataStoreFactory.getScenarioDataStore(), contractName, Contract.class);

        final QuorumExtendContract result = extensionService
            .initiateContractExtension(creator, existingContract.getContractAddress(), newNode, new ArrayList<>(allNodes), privacyFlag)
            .blockingFirst();

//        System.err.println(result.getError().getMessage());
        assertThat(result.getError()).isNull();

        final String transactionHash = result.getResult();

        final Optional<TransactionReceipt> transactionReceipt = this.getTransactionReceipt(creator, transactionHash);

        assertThat(transactionReceipt.isPresent()).isTrue();
        assertThat(transactionReceipt.get().getStatus()).isEqualTo("0x1");

        final String contractAddress = transactionReceipt.get().getContractAddress();

        DataStoreFactory.getScenarioDataStore().put(contractName + "extensionAddress", contractAddress);

        Thread.sleep(1000);

    }

    @Step("<newNode> accepts the offer to extend the contract <contractName>")
    public void acceptContractExtension(final QuorumNode newNode, final String contractName) {

        final DataStore store = DataStoreFactory.getScenarioDataStore();
        final PrivacyFlag privacyFlag = mustHaveValue(store, "privacyFlag", PrivacyFlag.class);
        final String contractAddress = mustHaveValue(store, contractName + "extensionAddress", String.class);
        final Set<QuorumNode> allNodes = mustHaveValue(store, "extensionAllNodes", Set.class);

        final QuorumVoteOnContract result = this.extensionService
            .voteOnExtension(newNode, true, contractAddress, allNodes, privacyFlag)
            .blockingFirst();

        assertThat(result.getError()).isNull();

        final Optional<TransactionReceipt> transactionReceipt = this.getTransactionReceipt(newNode, result.getResult());

        assertThat(transactionReceipt.isPresent()).isTrue();
        assertThat(transactionReceipt.get().getStatus()).isEqualTo("0x1");
    }

    @Step("<node> votes <vote> to extending contract <contractName>")
    public void voteOnContract(final QuorumNode node, final boolean vote, final String contractName) throws InterruptedException {

        final DataStore store = DataStoreFactory.getScenarioDataStore();
        final PrivacyFlag privacyFlag = mustHaveValue(store, "privacyFlag", PrivacyFlag.class);
        final String contractAddress = mustHaveValue(store, contractName + "extensionAddress", String.class);
        final Set<QuorumNode> allNodes = mustHaveValue(store, "extensionAllNodes", Set.class);

        final QuorumVoteOnContract voteResult = this.extensionService
            .voteOnExtension(node, vote, contractAddress, allNodes, privacyFlag)
            .blockingFirst();

        assertThat(voteResult.getError()).isNull();

        final Optional<TransactionReceipt> transactionReceipt
            = this.getTransactionReceipt(node, voteResult.getResult());

        assertThat(transactionReceipt.isPresent()).isTrue();
        assertThat(transactionReceipt.get().getStatus()).isEqualTo("0x1");

        Thread.sleep(1000);
    }

    @Step("<node> requests parties are updated for contract <contractName>")
    public void updatePartiesForContract(final QuorumNode node, final String contractName) throws InterruptedException {

        final DataStore store = DataStoreFactory.getScenarioDataStore();
        final PrivacyFlag privacyFlag = mustHaveValue(store, "privacyFlag", PrivacyFlag.class);
        final String contractAddress = mustHaveValue(store, contractName + "extensionAddress", String.class);
        final Set<QuorumNode> allNodes = mustHaveValue(store, "extensionAllNodes", Set.class);

        final QuorumUpdateParties result = this.extensionService
            .updateParties(node, contractAddress, allNodes, privacyFlag)
            .blockingFirst();

        assertThat(result.getError()).isNull();

        final Optional<TransactionReceipt> transactionReceipt = this.getTransactionReceipt(node, result.getResult());

        assertThat(transactionReceipt.isPresent()).isTrue();
        assertThat(transactionReceipt.get().getStatus()).isEqualTo("0x1");

        Thread.sleep(1000);
    }

    @Step("<node> has <contractName> listed in all active extensions")
    public void contractIsListed(final QuorumNode node, final String contractName) {

        final Contract contract = mustHaveValue(contractName, Contract.class);

        final QuorumActiveExtensionContracts result = this.extensionService
            .getExtensionContracts(node)
            .blockingFirst();

        final Optional<Map<Object, Object>> first = result.getResult()
            .stream()
            .filter(contractStatus -> contractStatus.containsValue(contract.getContractAddress()))
            .findFirst();

        assertThat(first).isPresent();
    }

    //TODO: deduplicate with opposite step ("contractIsListed")
    @Step("<node> does not see <contractName> listed in all active extensions")
    public void contractIsNotListed(final QuorumNode node, final String contractName) {

        final Contract contract = mustHaveValue(contractName, Contract.class);

        final QuorumActiveExtensionContracts result = this.extensionService
            .getExtensionContracts(node)
            .blockingFirst();

        final Optional<Map<Object, Object>> first = result.getResult()
            .stream()
            .filter(contractStatus -> contractStatus.containsValue(contract.getContractAddress()))
            .findFirst();

        assertThat(first).isNotPresent();

    }

    @Step("<node> sees <contractName> has status <status>")
    public void contractHasStatus(final QuorumNode node, final String contractName, final String status) {

        final Contract contract = mustHaveValue(contractName, Contract.class);

        final QuorumActiveExtensionContracts result = this.extensionService
            .getExtensionContracts(node)
            .blockingFirst();

        final Optional<Map<Object, Object>> first = result.getResult()
            .stream()
            .filter(contractStatus -> contractStatus.containsValue(contract.getContractAddress()))
            .findFirst();

        assertThat(first).isPresent();
        assertThat(first.get().get("status")).isEqualTo(status);
    }

    @Step("<node> cancels <contractName>")
    public void cancelExtension(final QuorumNode node, final String contractName) throws InterruptedException {

        final DataStore store = DataStoreFactory.getScenarioDataStore();
        final PrivacyFlag privacyFlag = mustHaveValue(store, "privacyFlag", PrivacyFlag.class);
        final String contractAddress = mustHaveValue(store, contractName + "extensionAddress", String.class);
        final Set<QuorumNode> allNodes = mustHaveValue(store, "extensionAllNodes", Set.class);

        final QuorumCancel result = this.extensionService
            .cancelExtension(node, contractAddress, allNodes, privacyFlag)
            .blockingFirst();

        assertThat(result.getError()).isNull();

        Thread.sleep(1000);
    }

    private Optional<TransactionReceipt> getTransactionReceipt(final QuorumNode node, final String transactionHash) {
        return transactionService
            .getTransactionReceipt(node, transactionHash)
            .repeatWhen(completed -> completed.delay(2, TimeUnit.SECONDS))
            .takeUntil(ethGetTransactionReceipt -> {
                return ethGetTransactionReceipt.getTransactionReceipt().isPresent();
            })
            .timeout(30, TimeUnit.SECONDS)
            .blockingLast()
            .getTransactionReceipt();
    }
}
