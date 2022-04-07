package com.quorum.gauge;

import com.quorum.gauge.common.Context;
import com.quorum.gauge.common.PrivacyFlag;
import com.quorum.gauge.common.QuorumNetworkProperty;
import com.quorum.gauge.core.AbstractSpecImplementation;
import com.quorum.gauge.ext.contractextension.*;
import com.quorum.gauge.services.ExtensionService;
import com.thoughtworks.gauge.Step;
import com.thoughtworks.gauge.datastore.DataStore;
import com.thoughtworks.gauge.datastore.DataStoreFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@Service
public class ContractExtension extends AbstractSpecImplementation {

    private static final Logger logger = LoggerFactory.getLogger(ContractExtension.class);

    private final ExtensionService extensionService;

    @Autowired
    public ContractExtension(final ExtensionService extensionService) {
        this.extensionService = Objects.requireNonNull(extensionService);
    }

    @Step("Initiate contract extension to <newNode> with its default account as recipient from <creator> for contract <contractName>")
    public void createNewContractExtensionProposition(final QuorumNetworkProperty.Node newNode,
                                                      final QuorumNetworkProperty.Node creator,
                                                      final String contractName) throws InterruptedException {

        PrivacyFlag privacyFlag = PrivacyFlag.StandardPrivate;

        DataStoreFactory.getScenarioDataStore().put("privacyFlag", privacyFlag);


        final Set<QuorumNetworkProperty.Node> allNodes = Stream.of(newNode, creator)
            .collect(Collectors.toSet());
        DataStoreFactory.getScenarioDataStore().put("extensionAllNodes", allNodes);

        final Contract existingContract = mustHaveValue(DataStoreFactory.getScenarioDataStore(), contractName, Contract.class);

        final QuorumExtendContract result = extensionService
            .initiateContractExtension(creator, existingContract.getContractAddress(), newNode, privacyFlag)
            .blockingFirst();

//        System.err.println(result.getError().getMessage());
        assertThat(result.getError()).isNull();

        final String transactionHash = result.getResult();

        final Optional<TransactionReceipt> transactionReceipt = transactionService.pollTransactionReceipt(creator, transactionHash);

        assertThat(transactionReceipt.isPresent()).isTrue();
        assertThat(transactionReceipt.get().getStatus()).isEqualTo("0x1");

        final String contractAddress = transactionReceipt.get().getContractAddress();

        DataStoreFactory.getScenarioDataStore().put(contractName + "extensionAddress", contractAddress);

        Thread.sleep(1000);

    }

    @Step("Initiate <contractName> extension from non creating node <fromNode> to <newNode> should fail with error message <errMsg>")
    public void createInvalidExtensionProposition(final String contractName,
                                                  final QuorumNetworkProperty.Node fromNode,
                                                  final QuorumNetworkProperty.Node newNode,
                                                  final String errMsg) throws InterruptedException {
        PrivacyFlag privacyFlag = PrivacyFlag.StandardPrivate;

        DataStoreFactory.getScenarioDataStore().put("privacyFlag", privacyFlag);


        final Set<QuorumNetworkProperty.Node> allNodes = Stream.of(fromNode, newNode)
            .collect(Collectors.toSet());
        DataStoreFactory.getScenarioDataStore().put("extensionAllNodes", allNodes);

        final Contract existingContract = mustHaveValue(DataStoreFactory.getScenarioDataStore(), contractName, Contract.class);

        final QuorumExtendContract result = extensionService
            .initiateContractExtension(fromNode, existingContract.getContractAddress(), newNode, privacyFlag)
            .blockingFirst();

        assertThat(result.getError().getMessage()).isEqualTo(errMsg);
    }

    @Step("Initiating contract extension to <newNode> with its default account as recipient from <creator> for contract <contractName> should fail with error <errMsg>")
    public void createFailedContractExtensionProposition(final QuorumNetworkProperty.Node newNode,
                                                         final QuorumNetworkProperty.Node creator,
                                                         final String contractName,
                                                         final String errMsg) throws InterruptedException {

        PrivacyFlag privacyFlag = PrivacyFlag.StandardPrivate;

        DataStoreFactory.getScenarioDataStore().put("privacyFlag", privacyFlag);


        final Set<QuorumNetworkProperty.Node> allNodes = Stream.of(newNode, creator)
            .collect(Collectors.toSet());
        DataStoreFactory.getScenarioDataStore().put("extensionAllNodes", allNodes);

        final Contract existingContract = mustHaveValue(DataStoreFactory.getScenarioDataStore(), contractName, Contract.class);

        final QuorumExtendContract result = extensionService
            .initiateContractExtension(creator, existingContract.getContractAddress(), newNode, privacyFlag)
            .blockingFirst();

//        System.err.println(result.getError().getMessage());
        assertThat(result.getError().getMessage()).isEqualTo(errMsg);
    }

    @Step("Initiating extension of <contractName> to <newNode> with its default account as recipient from <creator> should fail with error <expErrMsg>")
    public void createInavlidContractExtension(final String contractName,
                                               final QuorumNetworkProperty.Node newNode,
                                               final QuorumNetworkProperty.Node creator,
                                               final String expErrMsg) {
        PrivacyFlag privacyFlag = PrivacyFlag.StandardPrivate;
        DataStoreFactory.getScenarioDataStore().put("privacyFlag", privacyFlag);
        final Set<QuorumNetworkProperty.Node> allNodes = Stream.of(newNode, creator)
            .collect(Collectors.toSet());
        DataStoreFactory.getScenarioDataStore().put("extensionAllNodes", allNodes);

        final Contract existingContract = mustHaveValue(DataStoreFactory.getScenarioDataStore(), contractName, Contract.class);

        final QuorumExtendContract result = extensionService
            .initiateContractExtension(creator, existingContract.getContractAddress(), newNode, privacyFlag)
            .blockingFirst();

        assertThat(result.getError().getMessage()).isEqualTo(expErrMsg);
    }

    @Step("<newNode> accepts the offer to extend the contract <contractName>")
    public void acceptExtension(final QuorumNetworkProperty.Node newNode, final String contractName) {

        final DataStore store = DataStoreFactory.getScenarioDataStore();
        final PrivacyFlag privacyFlag = mustHaveValue(store, "privacyFlag", PrivacyFlag.class);
        final String contractAddress = mustHaveValue(store, contractName + "extensionAddress", String.class);
        final Set<QuorumNetworkProperty.Node> allNodes = mustHaveValue(store, "extensionAllNodes", Set.class);

        final QuorumVoteOnContract result = this.extensionService
            .acceptExtension(newNode, true, contractAddress, allNodes, privacyFlag)
            .blockingFirst();

        if(result.getError() != null) {
            logger.error(result.getError().toString());
        }

        assertThat(result.getError()).isNull();

        final Optional<TransactionReceipt> transactionReceipt = transactionService.pollTransactionReceipt(newNode, result.getResult());

        assertThat(transactionReceipt.isPresent()).isTrue();
        assertThat(transactionReceipt.get().getStatus()).isEqualTo("0x1");
    }

    @Step("<node> rejects contract extension of <contractName>")
    public void rejectExtension(final QuorumNetworkProperty.Node node, final String contractName) {

        final DataStore store = DataStoreFactory.getScenarioDataStore();
        final PrivacyFlag privacyFlag = mustHaveValue(store, "privacyFlag", PrivacyFlag.class);
        final String contractAddress = mustHaveValue(store, contractName + "extensionAddress", String.class);
        final Set<QuorumNetworkProperty.Node> allNodes = mustHaveValue(store, "extensionAllNodes", Set.class);

        final QuorumVoteOnContract voteResult = this.extensionService
            .acceptExtension(node, false, contractAddress, allNodes, privacyFlag)
            .blockingFirst();

        assertThat(voteResult.getError()).isNull();

        final Optional<TransactionReceipt> transactionReceipt
            = transactionService.pollTransactionReceipt(node, voteResult.getResult());

        assertThat(transactionReceipt.isPresent()).isTrue();
        assertThat(transactionReceipt.get().getStatus()).isEqualTo("0x1");

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Step("<node> requests parties are updated for contract <contractName>")
    public void updatePartiesForContract(final QuorumNetworkProperty.Node node, final String contractName) {

        final DataStore store = DataStoreFactory.getScenarioDataStore();
        final PrivacyFlag privacyFlag = mustHaveValue(store, "privacyFlag", PrivacyFlag.class);
        final String contractAddress = mustHaveValue(store, contractName + "extensionAddress", String.class);
        final Set<QuorumNetworkProperty.Node> allNodes = mustHaveValue(store, "extensionAllNodes", Set.class);

        final QuorumUpdateParties result = this.extensionService
            .updateParties(node, contractAddress, allNodes, privacyFlag)
            .blockingFirst();

        assertThat(result.getError()).isNull();

        final Optional<TransactionReceipt> transactionReceipt = transactionService.pollTransactionReceipt(node, result.getResult());

        assertThat(transactionReceipt.isPresent()).isTrue();
        assertThat(transactionReceipt.get().getStatus()).isEqualTo("0x1");

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Step("<node> has <contractName> listed in all active extensions")
    public void contractIsListed(final QuorumNetworkProperty.Node node, final String contractName) {
        assertThat(getListedStatus(node, contractName)).isPresent();
    }

    @Step("<node> does not see <contractName> listed in all active extensions")
    public void contractIsNotListed(final QuorumNetworkProperty.Node node, final String contractName) {
        assertThat(getListedStatus(node, contractName)).isNotPresent();
    }

    public Optional<Map<Object, Object>> getListedStatus(final QuorumNetworkProperty.Node node, final String contractName) {
        final Optional<String> accessToken = haveValue(DataStoreFactory.getScenarioDataStore(), "access_token", String.class);
        accessToken.ifPresent(Context::storeAccessToken);

        final Contract contract = mustHaveValue(contractName, Contract.class);

        final QuorumActiveExtensionContracts result = this.extensionService
            .getExtensionContracts(node)
            .blockingFirst();

        Context.removeAccessToken();

        return result.getResult()
            .stream()
            .filter(contractStatus -> contractStatus.containsValue(contract.getContractAddress()))
            .findFirst();
    }

    @Step("<node> sees <contractName> has status <status>")
    public void contractHasStatus(final QuorumNetworkProperty.Node node, final String contractName, final String status) {

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
    public void cancelExtension(final QuorumNetworkProperty.Node node, final String contractName) throws InterruptedException {

        final DataStore store = DataStoreFactory.getScenarioDataStore();
        final PrivacyFlag privacyFlag = mustHaveValue(store, "privacyFlag", PrivacyFlag.class);
        final String contractAddress = mustHaveValue(store, contractName + "extensionAddress", String.class);
        final Set<QuorumNetworkProperty.Node> allNodes = mustHaveValue(store, "extensionAllNodes", Set.class);

        final QuorumCancel result = this.extensionService
            .cancelExtension(node, contractAddress, allNodes, privacyFlag)
            .blockingFirst();

        assertThat(result.getError()).isNull();

        Thread.sleep(1000);
    }


    @Step("Wait for <contractName> to disappear from active extension in <node>")
    public void extensionCompleted(final String contractName, final QuorumNetworkProperty.Node node) {

        final Contract contract = mustHaveValue(contractName, Contract.class);
        Optional<String> accessToken = haveValue(DataStoreFactory.getScenarioDataStore(), "access_token", String.class);
        accessToken.ifPresent(Context::storeAccessToken);
        try {
            int count = 0;

            while (true) {
                count++;
                final QuorumActiveExtensionContracts result = this.extensionService
                    .getExtensionContracts(node)
                    .blockingFirst();

                final Optional<Map<Object, Object>> first = result.getResult()
                    .stream()
                    .filter(contractStatus -> contractStatus.containsValue(contract.getContractAddress()))
                    .findFirst();
                if ((!first.isPresent()) || (count > 25)) {
                    break;
                } else {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            final DataStore store = DataStoreFactory.getScenarioDataStore();

            final String contractAddress = mustHaveValue(store, contractName + "extensionAddress", String.class);
            String status = extensionService.getExtensionStatus(node, contractAddress);
            int i = 0;
            if (!status.equals("DONE")) {
                while (true) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    i++;
                    status = extensionService.getExtensionStatus(node, contractAddress);
                    if ((i > 25) || status.equals("DONE")) {
                        break;
                    }

                }
            }
            assertThat(status).describedAs("Extension must be successfully completed").isEqualTo("DONE");
        } finally {
            Context.removeAccessToken();
        }
    }

}
