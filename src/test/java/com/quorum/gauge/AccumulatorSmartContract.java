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


import com.quorum.gauge.common.QuorumNetworkProperty;
import com.quorum.gauge.common.RetryWithDelay;
import com.quorum.gauge.core.AbstractSpecImplementation;
import com.quorum.gauge.services.AbstractService;
import com.quorum.gauge.sol.Accumulator;
import com.thoughtworks.gauge.Step;
import com.thoughtworks.gauge.datastore.DataStoreFactory;
import io.reactivex.disposables.Disposable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.web3j.abi.EventValues;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@Service
public class AccumulatorSmartContract extends AbstractSpecImplementation {
    private static final Logger logger = LoggerFactory.getLogger(AccumulatorSmartContract.class);

    @Step("Deploy a public accumulator contract in <source>'s default account with initial value <initVal>, name this contract as <contractName>")
    public void setupAccumulatorPublicContract(String source, int initVal, String contractName) {
        saveCurrentBlockNumber();
        logger.debug("Setting up public accumulator contract from {}", source);
        Accumulator contract = accumulatorService.createAccumulatorPublicContract(
            networkProperty.getNode(source), AbstractService.DEFAULT_GAS_LIMIT, initVal).blockingFirst();

        DataStoreFactory.getSpecDataStore().put(contractName, contract);
        DataStoreFactory.getScenarioDataStore().put(contractName, contract);
    }

    @Step("Deploy a <privacyFlag> accumulator contract in <source>'s default account with initial value <initVal> and it's private for <privateFor>, name this contract as <contractName>")
    public void setupAccumulatorContract(String privacyFlag, String source, int initVal, String privateFor, String contractName) {
        saveCurrentBlockNumber();
        logger.debug("Setting up storage master from {} to {}", source, privateFor);

        List<QuorumNetworkProperty.Node> privateForList = Arrays.stream(privateFor.split(","))
            .map(s -> networkProperty.getNode(s))
            .collect(Collectors.toList());


        Accumulator contract = accumulatorService.createAccumulatorPrivateContract(
            networkProperty.getNode(source),
            privateForList,
            AbstractService.DEFAULT_GAS_LIMIT, initVal,
            privacyService.parsePrivacyFlag(privacyFlag)
        ).blockingFirst();

        DataStoreFactory.getSpecDataStore().put(contractName, contract);
        DataStoreFactory.getScenarioDataStore().put(contractName, contract);
    }

    private String eventListKey(String node, String contractName) {
        return node + "_" + contractName + "_eventList";
    }
    private String subscriptionKey(String node, String contractName) {
        return node + "_" + contractName + "_subscription";
    }

    private void subscribeToAccumulatorContractEventsOnNode(String node, String contractName) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractName, Accumulator.class);
        List<Accumulator.IncEventEventResponse> eventList = new LinkedList<>();
        Disposable subscription = accumulatorService.subscribeTo(networkProperty.getNode(node), c.getContractAddress(), eventList);

        String eventListKey = eventListKey(node, contractName);
        String subscriptionKey = subscriptionKey(node, contractName);
        DataStoreFactory.getSpecDataStore().put(eventListKey, eventList);
        DataStoreFactory.getSpecDataStore().put(subscriptionKey, subscription);
        DataStoreFactory.getScenarioDataStore().put(eventListKey, eventList);
        DataStoreFactory.getScenarioDataStore().put(subscriptionKey, subscription);
    }

    @Step("Subscribe to accumulator contract <contractName> IncEvent on <nodes>")
    public void subscribeToAccumulatorContractEvents(String contractName, String nodes) {
        for (String node : nodes.split(",")) {
            subscribeToAccumulatorContractEventsOnNode(node, contractName);
        }
    }

    @Step("Unsubscribe to accumulator contract <contractName> IncEvent on <nodes>")
    public void unsubscribeToAccumulatorContractEvents(String contractName, String nodes) {
        for (String node : nodes.split(",")) {
            String subscriptionKey = subscriptionKey(node, contractName);
            Disposable subscription = mustHaveValue(subscriptionKey, Disposable.class);
            subscription.dispose();
        }
    }

    @Step("Wait for events poll")
    public void waitForEventsPoll() {
        accumulatorService.sleepForPollingInterval();
    }

    @Step("Check IncEvent <index> for accumulator contract <contractName> on <node> has value <value>")
    public void checkIncEventOnNodeHasValue(int index, String contractName, String node, int value) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractName, Accumulator.class);
        List<Accumulator.IncEventEventResponse> eventList = (List<Accumulator.IncEventEventResponse>) DataStoreFactory.getSpecDataStore().get(eventListKey(node, contractName));
        assertThat(eventList.get(index).value).isEqualTo(BigInteger.valueOf(value));
    }

    @Step("Check IncEvent list size for accumulator contract <contractName> on <node> is <value>")
    public void checkIncEventListOnNodeHasSize(String contractName, String node, int size) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractName, Accumulator.class);
        List<Accumulator.IncEventEventResponse> eventList = (List<Accumulator.IncEventEventResponse>) DataStoreFactory.getSpecDataStore().get(eventListKey(node, contractName));
        assertThat(eventList.size()).isEqualTo(size);
    }


    @Step("Invoke accumulator.inc with value <value> in contract <contract> in <source>'s default account, file transaction hash as <txReference>")
    public void incAccumulatorPublicContract(int value, String contract, String source, String txReference) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contract, Contract.class);
        saveCurrentBlockNumber();
        logger.debug("Invoking accumulator.inc with value {} from {}", value, source);
        TransactionReceipt receipt = accumulatorService.incAccumulatorPublic(
            networkProperty.getNode(source), c.getContractAddress(), AbstractService.DEFAULT_GAS_LIMIT,
            value).blockingFirst();

        DataStoreFactory.getSpecDataStore().put(txReference, receipt.getTransactionHash());
        DataStoreFactory.getScenarioDataStore().put(txReference, receipt.getTransactionHash());
    }

    @Step("Invoke a <privacyFlag> accumulator.inc with value <value> in contract <contract> in <source>'s default account and it's private for <privateFor>, file transaction hash as <txReference>")
    public void incAccumulatorPrivateContract(String privacyFlag, int value, String contract, String source, String privateFor, String txReference) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contract, Contract.class);
        saveCurrentBlockNumber();
        List<QuorumNetworkProperty.Node> privateForList = Arrays.stream(privateFor.split(","))
            .map(s -> networkProperty.getNode(s))
            .collect(Collectors.toList());
        logger.debug("Invoking accumulator.inc in accumulator contract from {}", source);
        TransactionReceipt receipt = accumulatorService.incAccumulatorPrivate(
            networkProperty.getNode(source), privateForList, c.getContractAddress(), AbstractService.DEFAULT_GAS_LIMIT,
            value,
            privacyService.parsePrivacyFlag(privacyFlag)
        ).blockingFirst();
        DataStoreFactory.getSpecDataStore().put(txReference, receipt.getTransactionHash());
        DataStoreFactory.getScenarioDataStore().put(txReference, receipt.getTransactionHash());
    }

    @Step("Invoking a <privacyFlag> accumulator.inc with value <value> in contract <contract> in <source>'s default account and it's private for <privateFor> fails with error <error>")
    public void incAccumulatorPrivateContractFailsWithError(String privacyFlag, int value, String contract, String source, String privateFor, String error) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contract, Contract.class);
        saveCurrentBlockNumber();
        List<QuorumNetworkProperty.Node> privateForList = Arrays.stream(privateFor.split(","))
            .map(s -> networkProperty.getNode(s))
            .collect(Collectors.toList());
        logger.debug("Invoking accumulator.inc in accumulator contract from {}", source);
        assertThatThrownBy(
            () -> accumulatorService.incAccumulatorPrivate(
            networkProperty.getNode(source), privateForList, c.getContractAddress(), AbstractService.DEFAULT_GAS_LIMIT,
            value,
            privacyService.parsePrivacyFlag(privacyFlag))
                .blockingFirst()
        ).as("Expected exception thrown")
            .hasMessageContaining(error);
    }

    @Step("Accumulator <contractName>'s `get()` function execution in <node> returns <expectedValue>")
    public void getFromAccumulatorContract(String storageMaster, String source, int expectedValue) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), storageMaster, Contract.class);
        saveCurrentBlockNumber();
        logger.debug("Invoking get in accumulator contract from {}", source);
        final int value = accumulatorService.get(networkProperty.getNode(source), c.getContractAddress());
        assertThat(value).isEqualTo(expectedValue);
    }

    private TransactionReceipt getTransactionReceipt(QuorumNetworkProperty.Node node, String txReference) {
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
        return receipt.get();
    }

    @Step("Transaction Receipt in <node> for <txReference> has no logs")
    public void verifyTransactionReceiptHasNoLogs(String node, String txReference) {
        TransactionReceipt receipt = getTransactionReceipt(networkProperty.getNode(node), txReference);
        assertThat(receipt.getLogs().size()).isZero();
    }


    @Step("Transaction Receipt in <node> for <txReference> has status <status>")
    public void verifyTransactionReceiptHasStatus(String node, String txReference, String status) {
        TransactionReceipt receipt = getTransactionReceipt(networkProperty.getNode(node), txReference);
        assertThat(receipt.getStatus()).isEqualTo(status);
    }

    @Step("Transaction Receipt in <node> for <txReference> has one IncEvent log with value <value>")
    public void verifyTransactionReceiptHasOneLog(String node, String txReference, int value) {
        TransactionReceipt receipt = getTransactionReceipt(networkProperty.getNode(node), txReference);
        assertThat(receipt.getLogs().size()).isEqualTo(1);
        final Log log = receipt.getLogs().get(0);
        final EventValues eventValues = Contract.staticExtractEventParameters(Accumulator.INCEVENT_EVENT, log);
        BigInteger incEventVal = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        assertThat(incEventVal.intValue()).isEqualTo(value);
    }
}
