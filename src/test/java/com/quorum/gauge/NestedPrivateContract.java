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

import com.quorum.gauge.common.QuorumNode;
import com.quorum.gauge.core.AbstractSpecImplementation;
import com.quorum.gauge.ext.EthGetQuorumPayload;
import com.thoughtworks.gauge.Step;
import com.thoughtworks.gauge.datastore.DataStoreFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import rx.Observable;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Service
@SuppressWarnings("unchecked")
public class NestedPrivateContract extends AbstractSpecImplementation {
    private static final Logger logger = LoggerFactory.getLogger(NestedPrivateContract.class);

    @Step("Deploy a C1 contract with initial value <initialValue> in <source>'s default account and it's private for <target>, named this contract as <contractName>")
    public void setupC1Contract(int initialValue, QuorumNode source, QuorumNode target, String contractName) {
        logger.debug("Setting up contract from {} to {}", source, target);
        Contract contract = nestedContractService.createC1Contract(initialValue, source, target).toBlocking().first();

        DataStoreFactory.getSpecDataStore().put(contractName, contract);
        DataStoreFactory.getScenarioDataStore().put(contractName, contract);
    }

    @Step("Deploy a C2 contract with initial value <c1Address> in <source>'s default account and it's private for <target>, named this contract as <contractName>")
    public void setupC2Contract(String c1Address, QuorumNode source, QuorumNode target, String contractName) {
        logger.debug("Setting up contract from {} to {}", source, target);
        Contract c1 = (Contract) DataStoreFactory.getSpecDataStore().get(c1Address);
        Contract contract = nestedContractService.createC2Contract(c1.getContractAddress(), source, target).toBlocking().first();

        DataStoreFactory.getSpecDataStore().put(contractName, contract);
        DataStoreFactory.getScenarioDataStore().put(contractName, contract);
    }

    @Step("Nested Contract Transaction Hash is returned for <contractName>")
    public void verifyTransactionHash(String contractName) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractName, Contract.class);
        String transactionHash = c.getTransactionReceipt().orElseThrow(() -> new RuntimeException("no transaction receipt for contract")).getTransactionHash();

        assertThat(transactionHash).isNotBlank();

        DataStoreFactory.getScenarioDataStore().put(contractName + "_transactionHash", transactionHash);
    }

    @Step("Nested Contract Transaction Receipt is present in <node> for <contractName>")
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


    @Step("Nested Contract C1 <contractName>'s `get()` function execution in <node> returns <expectedValue>")
    public void verifyPrivacyWithParticipatedNodesC1(String contractName, QuorumNode node, int expectedValue) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractName, Contract.class);
        int actualValue = nestedContractService.readC1Value(node, c.getContractAddress());

        assertThat(actualValue).isEqualTo(expectedValue);
    }

    @Step("Nested Contract C2 <contractName>'s `get()` function execution in <node> returns <expectedValue>")
    public void verifyPrivacyWithParticipatedNodesC2(String contractName, QuorumNode node, int expectedValue) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractName, Contract.class);
        int actualValue = nestedContractService.readC2Value(node, c.getContractAddress());

        assertThat(actualValue).isEqualTo(expectedValue);
    }


    @Step("Nested Contract C1 Execute <contractName>'s `set()` function with new value <newValue> in <source> and it's private for <target>")
    public void updateNewValueC1(String contractName, int newValue, QuorumNode source, QuorumNode target) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractName, Contract.class);
        TransactionReceipt receipt = nestedContractService.updateC1Contract(source, target, c.getContractAddress(), newValue).toBlocking().first();

        assertThat(receipt.getTransactionHash()).isNotBlank();
        assertThat(receipt.getBlockNumber()).isNotEqualTo(currentBlockNumber());
    }

    @Step("Nested Contract C1 Fails To Execute <contractName>'s `set()` function with new value <newValue> in <source> and it's private for <target>")
    public void updateNewValueC1Fails(String contractName, int newValue, QuorumNode source, QuorumNode target) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractName, Contract.class);
        Exception exception = null;
        try {
            nestedContractService.updateC1Contract(source, target, c.getContractAddress(), newValue).toBlocking().first();
        } catch (Exception e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
    }

    @Step("Nested Contract C2 Fails To Execute <contractName>'s `set()` function with new value <newValue> in <source> and it's private for <target>")
    public void updateNewValueC2Fails(String contractName, int newValue, QuorumNode source, QuorumNode target) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractName, Contract.class);
        Exception exception = null;
        try {
            nestedContractService.updateC2Contract(source, target, c.getContractAddress(), newValue).toBlocking().first();
        } catch (Exception e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
    }


    @Step("Nested Contract C2 Execute <contractName>'s `set()` function with new value <newValue> in <source> and it's private for <target>")
    public void updateNewValueC2(String contractName, int newValue, QuorumNode source, QuorumNode target) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractName, Contract.class);
        TransactionReceipt receipt = nestedContractService.updateC2Contract(source, target, c.getContractAddress(), newValue).toBlocking().first();

        assertThat(receipt.getTransactionHash()).isNotBlank();
        assertThat(receipt.getBlockNumber()).isNotEqualTo(currentBlockNumber());
    }


    @Step("Nested Contract <contractName>'s payload is retrievable from <node>")
    public void verifyPrivateContractPayloadIsAccessible(String contractName, QuorumNode node) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractName, Contract.class);
        EthGetQuorumPayload payload = transactionService.getPrivateTransactionPayload(node, c.getTransactionReceipt().get().getTransactionHash()).toBlocking().first();

        assertThat(payload.getResult()).isNotEqualTo("0x");
    }


    @Step("Nested Contract <contractName>'s payload is not retrievable from <node>")
    public void verifyPrivateContractPayloadIsNotAccessible(String contractName, QuorumNode node) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractName, Contract.class);
        EthGetQuorumPayload payload = transactionService.getPrivateTransactionPayload(node, c.getTransactionReceipt().get().getTransactionHash()).toBlocking().first();

        assertThat(payload.getResult()).isEqualTo("0x");
    }
}
