/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.quorum.gauge;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.quorum.gauge.common.QuorumNode;
import com.quorum.gauge.common.RawDeployedContractTarget;
import com.quorum.gauge.common.RetryWithDelay;
import com.quorum.gauge.common.config.WalletData;
import com.quorum.gauge.core.AbstractSpecImplementation;
import com.quorum.gauge.ext.EthGetQuorumPayload;
import com.quorum.gauge.services.AbstractService;
import com.thoughtworks.gauge.Step;
import com.thoughtworks.gauge.datastore.DataStoreFactory;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.quorum.PrivacyFlag;
import org.web3j.tx.Contract;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Service
@SuppressWarnings("unchecked")
public class PrivateSmartContract extends AbstractSpecImplementation {
    private static final Logger logger = LoggerFactory.getLogger(PrivateSmartContract.class);

    @Step("Deploy a simple smart contract with initial value <initialValue> in <source>'s default account and it's private for <target>, named this contract as <contractName>")
    public void setupContract(int initialValue, QuorumNode source, QuorumNode target, String contractName) {
        setupContract("STANDARD_PRIVATE", initialValue, source, target, contractName);
    }

    @Step("Deploy a <privacyFlag> simple smart contract with initial value <initialValue> in <source>'s default account and it's private for <target>, named this contract as <contractName>")
    public void setupContract(String privacyFlag, int initialValue, QuorumNode source, QuorumNode target, String contractName) {
        saveCurrentBlockNumber();
        logger.debug("Setting up contract from {} to {}", source, target);
        Contract contract = contractService.createSimpleContract(
            initialValue,
            source,
            null, Arrays.asList(target),
            AbstractService.DEFAULT_GAS_LIMIT,
            privacyService.parsePrivacyFlag(privacyFlag)
        ).blockingFirst();

        DataStoreFactory.getSpecDataStore().put(contractName, contract);
        DataStoreFactory.getScenarioDataStore().put(contractName, contract);
        DataStoreFactory.getScenarioDataStore().put("transactionHash", contract.getTransactionReceipt().get().getTransactionHash());
    }

    @Step("Deploying a <privacyFlag> simple smart contract with initial value <initialValue> in <source>'s default account and it's private for <target> fails with message <failureMessage>")
    public void setupContractFailsWithMessage(String privacyFlag, int initialValue, QuorumNode source, QuorumNode target, String failureMessage) {
        saveCurrentBlockNumber();
        var flags = privacyService.parsePrivacyFlag(privacyFlag);
        logger.debug("Setting up contract from {} to {} with {}", source, target, flags);
        try {
            contractService.createSimpleContract(
                initialValue,
                source,
                null, Arrays.asList(target),
                AbstractService.DEFAULT_GAS_LIMIT,
               flags
            ).blockingFirst();
            assertThat(false).as("An exception should have been raised during contract creation.").isTrue();
        } catch (Exception txe) {
            assertThat(txe).hasMessageContaining(failureMessage);
        }
    }

    @Step("Transaction Hash is returned for <contractName>")
    public void verifyTransactionHash(String contractName) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractName, Contract.class);
        String transactionHash = c.getTransactionReceipt().orElseThrow(() -> new RuntimeException("no transaction receipt for contract")).getTransactionHash();

        assertThat(transactionHash).isNotBlank();

        DataStoreFactory.getScenarioDataStore().put(contractName + "_transactionHash", transactionHash);
    }

    @Step("Transaction Receipt is present in <node> for <contractName> from <node>'s default account")
    public void verifyTransactionReceipt(QuorumNode node, String contractName, QuorumNode source) {
        String transactionHash = mustHaveValue(DataStoreFactory.getScenarioDataStore(), contractName + "_transactionHash", String.class);
        Optional<TransactionReceipt> receipt = transactionService.getTransactionReceipt(node, transactionHash)
            .map(ethGetTransactionReceipt -> {
                if (ethGetTransactionReceipt.getTransactionReceipt().isPresent()) {
                    return ethGetTransactionReceipt;
                } else {
                    throw new RuntimeException("retry");
                }
            }).retryWhen(new RetryWithDelay(20, 3000))
            .blockingFirst().getTransactionReceipt();

        assertThat(receipt.isPresent()).isTrue();
        assertThat(receipt.get().getBlockNumber()).isNotEqualTo(currentBlockNumber());

        String senderAddr = accountService.getDefaultAccountAddress(source).blockingFirst();
        assertThat(receipt.get().getFrom()).isEqualTo(senderAddr);
    }

    @Step("Transaction Receipt is present in <node> for <txReference>")
    public void verifyTransactionReceipt(QuorumNode node, String txReference) {
        String transactionHash = mustHaveValue(DataStoreFactory.getScenarioDataStore(), txReference, String.class);
        Optional<TransactionReceipt> receipt = transactionService.getTransactionReceipt(node, transactionHash)
            .map(ethGetTransactionReceipt -> {
                if (ethGetTransactionReceipt.getTransactionReceipt().isPresent()) {
                    return ethGetTransactionReceipt;
                } else {
                    throw new RuntimeException("retry");
                }
            }).retryWhen(new RetryWithDelay(20, 3000))
            .blockingFirst().getTransactionReceipt();

        assertThat(receipt.isPresent()).isTrue();
    }

    @Step("<contractName> stored in <source> and <target> must have the same storage root")
    public void verifyStorageRoot(String contractName, QuorumNode source, QuorumNode target) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractName, Contract.class);
        Observable.zip(
            contractService.getStorageRoot(source, c.getContractAddress()).subscribeOn(Schedulers.io()),
            contractService.getStorageRoot(target, c.getContractAddress()).subscribeOn(Schedulers.io()),
            (s, t) -> assertThat(s).isEqualTo(t)
        );
    }

    @Step("<contractName> stored in <source> and <stranger> must not have the same storage root")
    public void verifyStorageRootForNonParticipatedNode(String contractName, QuorumNode source, QuorumNode stranger) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractName, Contract.class);
        Observable.zip(
            contractService.getStorageRoot(source, c.getContractAddress()).subscribeOn(Schedulers.io()),
            contractService.getStorageRoot(stranger, c.getContractAddress()).subscribeOn(Schedulers.io()),
            (s, t) -> assertThat(s).isNotEqualTo(t)
        );
    }

    @Step("<contractName>'s `get()` function execution in <node> returns <expectedValue>")
    public void verifyPrivacyWithParticipatedNodes(String contractName, QuorumNode node, int expectedValue) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractName, Contract.class);
        // check transaction receipt to make sure the state is ready
        assertThat(c.getTransactionReceipt().isPresent()).isTrue();
        transactionService.getTransactionReceipt(node, c.getTransactionReceipt().get().getTransactionHash())
            .map(ethGetTransactionReceipt -> {
                if (ethGetTransactionReceipt.getTransactionReceipt().isPresent()) {
                    return ethGetTransactionReceipt;
                } else {
                    throw new RuntimeException("retry");
                }
            }).retryWhen(new RetryWithDelay(5, 1000))
            .blockingSubscribe();
        int actualValue = contractService.readSimpleContractValue(node, c.getContractAddress());

        assertThat(actualValue).isEqualTo(expectedValue);
    }

    @Step("Execute <contractName>'s `set()` function with privacy type <flag> to set new value to <newValue> in <source> and it's private for <target>")
    public TransactionReceipt updateNewValue(String contractName, String flag, int newValue, QuorumNode source, QuorumNode target) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractName, Contract.class);
        TransactionReceipt receipt = contractService.updateSimpleContract(source,
            target,
            c.getContractAddress(),
            newValue,
            privacyService.parsePrivacyFlag(flag)
        ).blockingFirst();

        assertThat(receipt.getTransactionHash()).isNotBlank();
        assertThat(receipt.getBlockNumber()).isNotEqualTo(currentBlockNumber());

        return transactionService.waitForTransactionReceipt(target, receipt.getTransactionHash());
    }

    @Step("Execute <contractName>'s `set()` function with new value <newValue> in <source> and it's private for <target>")
    public void updateNewValue(String contractName, int newValue, QuorumNode source, QuorumNode target) {
        updateNewValue(contractName, PrivacyFlag.STANDARD_PRIVATE.name(), newValue, source, target);
    }

    @Step("Execute <contractName>'s `set()` function with new value <newValue> in <source> and it's private for <target>, store transaction hash as <txRef>")
    public void updateNewValue(String contractName, int newValue, QuorumNode source, QuorumNode target, String txRef) {
        TransactionReceipt r = updateNewValue(contractName, PrivacyFlag.STANDARD_PRIVATE.name(), newValue, source, target);
        DataStoreFactory.getScenarioDataStore().put(txRef, r.getTransactionHash());
    }

    @Step("Deploy <count> private smart contracts between a default account in <source> and a default account in <target>")
    public void createMultiple(int count, QuorumNode source, QuorumNode target) {
        int arbitraryValue = 10;
        List<Observable<? extends Contract>> allObservableContracts = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            allObservableContracts.add(contractService.createSimpleContract(arbitraryValue, source, target));
        }
        List<Contract> contracts = Observable.zip(allObservableContracts, args -> {
            List<Contract> tmp = new ArrayList<>();
            for (Object o : args) {
                tmp.add((Contract) o);
            }
            return tmp;
        }).blockingFirst();

        DataStoreFactory.getSpecDataStore().put(String.format("%s_source_contract", source), contracts);
        DataStoreFactory.getSpecDataStore().put(String.format("%s_target_contract", target), contracts);
        DataStoreFactory.getScenarioDataStore().put(String.format("%s_source_contract", source), contracts);
        DataStoreFactory.getScenarioDataStore().put(String.format("%s_target_contract", target), contracts);
    }

    @Step("<node> has received <expectedCount> transactions")
    public void verifyNumberOfTransactions(QuorumNode node, int expectedCount) {
        List<Contract> sourceContracts = haveValue(DataStoreFactory.getSpecDataStore(), String.format("%s_source_contract", node), List.class, new ArrayList<Contract>());
        List<Contract> targetContracts = haveValue(DataStoreFactory.getSpecDataStore(), String.format("%s_target_contract", node), List.class, new ArrayList<Contract>());
        List<Contract> contracts = new ArrayList<>(sourceContracts);
        if (targetContracts != null) {
            contracts.addAll(targetContracts);
        }
        Scheduler scheduler = threadLocalDelegateScheduler(contracts.size());
        List<Observable<EthGetTransactionReceipt>> allObservableReceipts = new ArrayList<>();
        for (Contract c : contracts) {
            String txHash = c.getTransactionReceipt().orElseThrow(() -> new RuntimeException("no receipt for contract")).getTransactionHash();
            allObservableReceipts.add(transactionService.getTransactionReceipt(node, txHash)
                .map(ethGetTransactionReceipt -> {
                    if (ethGetTransactionReceipt.getTransactionReceipt().isPresent()) {
                        return ethGetTransactionReceipt;
                    } else {
                        throw new RuntimeException("retry");
                    }
                })
                .retryWhen(new RetryWithDelay(20, 3000))
                .subscribeOn(scheduler));
        }
        Integer actualCount = Observable.zip(allObservableReceipts, args -> {
            int count = 0;
            for (Object o : args) {
                EthGetTransactionReceipt r = (EthGetTransactionReceipt) o;
                if (r.getTransactionReceipt().isPresent() && r.getTransactionReceipt().get().getBlockNumber().compareTo(BigInteger.valueOf(0)) != 0) {
                    count++;
                }
            }
            return count;
        }).blockingFirst();

        assertThat(actualCount).isEqualTo(expectedCount);
    }

    @Step("<contractName>'s payload is retrievable from <node>")
    public void verifyPrivateContractPayloadIsAccessible(String contractName, QuorumNode node) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractName, Contract.class);
        EthGetQuorumPayload payload = transactionService.getPrivateTransactionPayload(node, c.getTransactionReceipt().get().getTransactionHash()).blockingFirst();

        assertThat(payload.getResult()).isNotEqualTo("0x");
    }

    @Step("<contractName>'s payload is not retrievable from <node>")
    public void verifyPrivateContractPayloadIsNotAccessible(String contractName, QuorumNode node) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractName, Contract.class);
        EthGetQuorumPayload payload = transactionService.getPrivateTransactionPayload(node, c.getTransactionReceipt().get().getTransactionHash()).blockingFirst();

        assertThat(payload.getResult()).isEqualTo("0x");
    }

    @Step("Asynchronously deploy a simple smart contract with initial value <initialValue> in <source>'s default account and it's private for <target>, named this contract as <contractName>")
    public void setupContractAsync(int initialValue, QuorumNode source, QuorumNode target, String contractName) {
        // sourceAccount == null indicates that we are using the default account
        setupContractAsyncWithAccount(initialValue, source, null, target, contractName);
    }

    /**
     * This depends on waithook Docker container to be available. The solution here is to use "localhost" for websocket listener of the callback
     * and use the container hostname for Quorum to send the response.
     */
    private void setupContractAsyncWithAccount(int initialValue, QuorumNode source, String sourceAccount, QuorumNode target, String contractName) {
        CountDownLatch waitForCallback = new CountDownLatch(1);
        CountDownLatch waitForWebSocket = new CountDownLatch(1);

        String baseUrl = "/testing_quorum_" + Math.abs(new Random().nextLong());
        String callbackUrl = "http://waithook.local:3012" + baseUrl;

        Request websocketCallback = new Request.Builder().url("ws://localhost:3012" + baseUrl).build();
        WebSocket ws = okHttpClient.newWebSocket(websocketCallback, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                logger.debug("Connected to callback listener");
                waitForWebSocket.countDown();
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                logger.debug("Received text: {}", text);
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    HashMap<String, Object> htmlPayload = objectMapper.readValue(text, new TypeReference<HashMap<String, Object>>() {
                    });
                    HashMap<String, String> htmlBody = objectMapper.readValue((String) htmlPayload.get("body"), new TypeReference<HashMap<String, String>>() {
                    });
                    String error = htmlBody.get("error");
                    String txHash = htmlBody.get("txHash");
                    logger.debug("Error: {}, TxHash: {}", error, txHash);
                    if (StringUtils.isEmpty(error)) {
                        DataStoreFactory.getScenarioDataStore().put(contractName + "_transactionHash", txHash);
                    } else {
                        DataStoreFactory.getScenarioDataStore().put(contractName + "_error", error);
                    }
                } catch (IOException e) {
                    logger.debug("Unable to read response", e);
                    throw new RuntimeException("Unable to read response [" + text + "]", e);
                } finally {
                    waitForCallback.countDown();
                }
            }
        });

        try {
            waitForWebSocket.await(3, TimeUnit.SECONDS);

            contractService.createClientReceiptContractAsync(initialValue, source, sourceAccount, target, callbackUrl).blockingFirst();

            waitForCallback.await(20, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            ws.close(1000, "Finished");
        }
    }

    @Step("Asynchronously deploy a simple smart contract with initial value <initialValue> in <source>'s non-existed account and it's private for <target>, named this contract as <contractName>")
    public void setupContractAsyncWithInvalidAccount(int initialValue, QuorumNode source, QuorumNode target, String contractName) {
        byte[] randomBytes = new byte[20];
        new Random().nextBytes(randomBytes);
        String nonExistedAccount = "0x" + Hex.toHexString(randomBytes);
        setupContractAsyncWithAccount(initialValue, source, nonExistedAccount, target, contractName);
    }

    @Step("An error is returned for <contractName>")
    public void verifyTransacionHashValue(String contractName) {
        String actualValue = mustHaveValue(DataStoreFactory.getScenarioDataStore(), contractName + "_error", String.class);

        assertThat(actualValue).as("Error message").isNotBlank();
    }

    @Step("Deploy `ClientReceipt` smart contract from a default account in <source> and it's private for <target>, named this contract as <contractName>")
    public void deployClientReceiptSmartContract(QuorumNode source, QuorumNode target, String contractName) {
        Contract c = contractService.createClientReceiptPrivateSmartContract(source, target).blockingFirst();

        DataStoreFactory.getSpecDataStore().put(contractName, c);
        DataStoreFactory.getScenarioDataStore().put(contractName, c);
        DataStoreFactory.getScenarioDataStore().put(contractName + "_source", source);
        DataStoreFactory.getScenarioDataStore().put(contractName + "_target", target);
    }

    @Step("Execute <contractName>'s `deposit()` function <count> times with arbitrary id and value from <source>. And it's private for <target>")
    public void executeDeposit(String contractName, int count, QuorumNode source, QuorumNode target) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractName, Contract.class);
        Scheduler scheduler = threadLocalDelegateScheduler(count);
        List<Observable<TransactionReceipt>> observables = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            observables.add(contractService.updateClientReceiptPrivate(source, target, c.getContractAddress(), BigInteger.ZERO));
        }
        List<TransactionReceipt> receipts = Observable.zip(observables, objects -> Observable.fromArray(objects).map(o -> (TransactionReceipt) o).toList().blockingGet()).blockingFirst();

        DataStoreFactory.getScenarioDataStore().put("receipts", receipts);
    }

    @Step("Execute <contractName>'s `deposit()` function <count> times with arbitrary id and value between original parties")
    public void executeDepositBetweenOriginalParties(String contractName, int count) {
        List<Observable<TransactionReceipt>> observables = new ArrayList<>();
        String[] contractNames = contractName.split(",");
        for (String cName : contractNames) {
            Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), cName, Contract.class);
            QuorumNode source = mustHaveValue(DataStoreFactory.getScenarioDataStore(), cName + "_source", QuorumNode.class);
            QuorumNode target = mustHaveValue(DataStoreFactory.getScenarioDataStore(), cName + "_target", QuorumNode.class);
            for (int i = 0; i < count; i++) {
                observables.add(contractService.updateClientReceiptPrivate(source, target, c.getContractAddress(), BigInteger.ZERO));
            }
        }
        List<TransactionReceipt> receipts = Observable.zip(observables, objects -> Observable.fromArray(objects).map(o -> (TransactionReceipt) o).toList().blockingGet()).blockingFirst();

        DataStoreFactory.getScenarioDataStore().put("receipts", receipts);
    }

    @Step("<node> has received transactions from <contractName> which contain <expectedEventCount> log events in state")
    public void verifyLogEvents(QuorumNode node, String contractName, int expectedEventCount) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractName, Contract.class);

        EthLog ethLog = transactionService.getLogsUsingFilter(node, c.getContractAddress()).blockingFirst();
        List<EthLog.LogResult> logResults = ethLog.getLogs();

        assertThat(logResults.size()).as("Log Event Count").isEqualTo(expectedEventCount);
    }

    @Step("Send <count> simple private smart contracts from a default account in <source> and it's separately private for <targets>")
    public void sendPrivateSmartContracts(int count, QuorumNode source, String targets) {
        String[] target = targets.split(",");
        QuorumNode[] targetNodes = new QuorumNode[target.length];
        for (int i = 0; i < targetNodes.length; i++) {
            targetNodes[i] = QuorumNode.valueOf(target[i]);
        }
        List<Observable<? extends Contract>> allObservableContracts = new ArrayList<>();
        for (QuorumNode targetNode : targetNodes) {
            for (int i = 0; i < count; i++) {
                int arbitraryValue = new Random().nextInt(50) + 1;
                allObservableContracts.add(contractService.createSimpleContract(arbitraryValue, source, targetNode));
            }
        }
        BigInteger blockNumber = Observable.zip(allObservableContracts, args -> utilService.getCurrentBlockNumber().blockingFirst()).blockingFirst().getBlockNumber();
        assertThat(blockNumber).isNotEqualTo(currentBlockNumber());
    }

    @Step("Send <count> simple private raw smart contracts from wallet <wallet> in <source> and it's separately private for <targets> and saved under <store>")
    public void sendRawPrivateSmartContracts(int count, WalletData wallet, QuorumNode source, String targets, String store) {
        String[] target = targets.split(",");
        QuorumNode[] targetNodes = new QuorumNode[target.length];
        for (int i = 0; i < targetNodes.length; i++) {
            targetNodes[i] = QuorumNode.valueOf(target[i]);
        }

        try {
            List deployedContracts = Observable.zip(
                rawContractService.createNRawSimplePrivateContract(count, wallet, source, targetNodes),
                (a) -> a
            ).collectInto(new ArrayList<>(), (objects, deployedContract) -> objects.addAll(Arrays.asList(deployedContract))).subscribeOn(Schedulers.io()).blockingGet();
            DataStoreFactory.getScenarioDataStore().put(store, deployedContracts);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Step("Verify the stored data <store> into the store to have the right value")
    public void verifyStoredData(String store) {
        List<RawDeployedContractTarget> deployedContracts = (List<RawDeployedContractTarget>) mustHaveValue(DataStoreFactory.getScenarioDataStore(), store, List.class);

        deployedContracts.forEach(o -> {
            int value = contractService.readSimpleContractValue(o.target, o.receipt.getContractAddress());
            assertThat(value).isEqualTo(o.value);
        });
    }
}
