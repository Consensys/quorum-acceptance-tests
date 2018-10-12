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
import com.quorum.gauge.core.AbstractSpecImplementation;
import com.quorum.gauge.ext.EthGetQuorumPayload;
import com.thoughtworks.gauge.Step;
import com.thoughtworks.gauge.datastore.DataStoreFactory;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import rx.Observable;
import rx.schedulers.Schedulers;

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
        logger.debug("Setting up contract from {} to {}", source, target);
        Contract contract = contractService.createSimpleContract(initialValue, source, target).toBlocking().first();

        DataStoreFactory.getSpecDataStore().put(contractName, contract);
        DataStoreFactory.getScenarioDataStore().put(contractName, contract);
    }

    @Step("Transaction Hash is returned for <contractName>")
    public void verifyTransactionHash(String contractName) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractName, Contract.class);
        String transactionHash = c.getTransactionReceipt().orElseThrow(()-> new RuntimeException("no transaction receipt for contract")).getTransactionHash();

        assertThat(transactionHash).isNotBlank();

        DataStoreFactory.getScenarioDataStore().put(contractName + "_transactionHash", transactionHash);
    }

    @Step("Transaction Receipt is present in <node> for <contractName>")
    public void verifyTransactionReceipt(QuorumNode node, String contractName) {
        String transactionHash = mustHaveValue(DataStoreFactory.getScenarioDataStore(), contractName + "_transactionHash", String.class);
        Optional<TransactionReceipt> receipt = transactionService.getTransactionReceipt(node, transactionHash)
                .map(ethGetTransactionReceipt -> {
                    if (ethGetTransactionReceipt.getTransactionReceipt().isPresent())
                        return ethGetTransactionReceipt;
                    throw new RuntimeException("retry");
                }).retryWhen(attempts -> attempts.zipWith(
                        Observable.range(1, 10), (n, i) -> i)
                        .flatMap(i -> Observable.timer(3, TimeUnit.SECONDS))
                        .doOnCompleted(() -> {
                            throw new RuntimeException("Expected transaction receipt is ready but not due to time out!");
                        })
                ).toBlocking().first().getTransactionReceipt();

        assertThat(receipt.isPresent()).isTrue();
        assertThat(receipt.get().getBlockNumber()).isNotEqualTo(currentBlockNumber());
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
        int actualValue = contractService.readSimpleContractValue(node, c.getContractAddress());

        assertThat(actualValue).isEqualTo(expectedValue);
    }

    @Step("Execute <contractName>'s `set()` function with new value <newValue> in <source> and it's private for <target>")
    public void updateNewValue(String contractName, int newValue, QuorumNode source, QuorumNode target) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractName, Contract.class);
        TransactionReceipt receipt = contractService.updateSimpleContract(source, target, c.getContractAddress(), newValue).toBlocking().first();

        assertThat(receipt.getTransactionHash()).isNotBlank();
        assertThat(receipt.getBlockNumber()).isNotEqualTo(currentBlockNumber());
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

        DataStoreFactory.getSpecDataStore().put(String.format("%s_source_contract", source), contracts);
        DataStoreFactory.getSpecDataStore().put(String.format("%s_target_contract", target), contracts);
        DataStoreFactory.getScenarioDataStore().put(String.format("%s_source_contract", source), contracts);
        DataStoreFactory.getScenarioDataStore().put(String.format("%s_target_contract", target), contracts);
    }

    @Step("<node> has received <expectedCount> transactions")
    public void verifyNumberOfTransactions(QuorumNode node, int expectedCount) {
        List<Contract> sourceContracts =  haveValue(DataStoreFactory.getSpecDataStore(), String.format("%s_source_contract", node), List.class, new ArrayList<Contract>());
        List<Contract> targetContracts = haveValue(DataStoreFactory.getSpecDataStore(), String.format("%s_target_contract", node), List.class, new ArrayList<Contract>());
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

    @Step("<contractName>'s payload is retrievable from <node>")
    public void verifyPrivateContractPayloadIsAccessible(String contractName, QuorumNode node) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractName, Contract.class);
        EthGetQuorumPayload payload = transactionService.getPrivateTransactionPayload(node, c.getTransactionReceipt().get().getTransactionHash()).toBlocking().first();

        assertThat(payload.getResult()).isNotEqualTo("0x");
    }

    @Step("<contractName>'s payload is not retrievable from <node>")
    public void verifyPrivateContractPayloadIsNotAccessible(String contractName, QuorumNode node) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractName, Contract.class);
        EthGetQuorumPayload payload = transactionService.getPrivateTransactionPayload(node, c.getTransactionReceipt().get().getTransactionHash()).toBlocking().first();

        assertThat(payload.getResult()).isEqualTo("0x");
    }

    @Step("Asynchronously deploy a simple smart contract with initial value <initialValue> in <source>'s default account and it's private for <target>, named this contract as <contractName>")
    public void setupContractAsync(int initialValue, QuorumNode source, QuorumNode target, String contractName) {
        // sourceAccount == null indicates that we are using the default account
        setupContractAsyncWithAccount(initialValue, source, null, target, contractName);
    }

    private void setupContractAsyncWithAccount(int initialValue, QuorumNode source, String sourceAccount, QuorumNode target, String contractName) {
        CountDownLatch waitForCallback = new CountDownLatch(1);
        CountDownLatch waitForWebSocket = new CountDownLatch(1);

        String baseUrl = "waithook.com/" + UUID.randomUUID().toString();
        String callbackUrl = "http://" + baseUrl;

        Request callback = new Request.Builder().url("ws://" + baseUrl).build();
        WebSocket ws = okHttpClient.newWebSocket(callback, new WebSocketListener() {
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
            waitForWebSocket.await();

            contractService.createClientReceiptContractAsync(initialValue, source, sourceAccount, target, callbackUrl).toBlocking().first();

            waitForCallback.await(20, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
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
        Contract c = contractService.createClientReceiptPrivateSmartContract(source, target).toBlocking().first();

        DataStoreFactory.getSpecDataStore().put(contractName, c);
        DataStoreFactory.getScenarioDataStore().put(contractName, c);
    }

    @Step("Execute <contractName>'s `deposit()` function <count> times with arbitrary id and value from <source>. And it's private for <target>")
    public void excuteDesposit(String contractName, int count, QuorumNode source, QuorumNode target) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractName, Contract.class);
        List<Observable<TransactionReceipt>> observables = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            observables.add(contractService.updateClientReceiptPrivate(source, target, c.getContractAddress(), BigInteger.ZERO).subscribeOn(Schedulers.io()));
        }
        List<TransactionReceipt> receipts = Observable.zip(observables, objects -> Observable.from(objects).map(o -> (TransactionReceipt)o ).toList().toBlocking().first()).toBlocking().first();

        DataStoreFactory.getScenarioDataStore().put("receipts", receipts);
    }
}
