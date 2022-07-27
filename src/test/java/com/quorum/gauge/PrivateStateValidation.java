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
import com.quorum.gauge.services.AbstractService;
import com.thoughtworks.gauge.Step;
import com.thoughtworks.gauge.datastore.DataStoreFactory;
import io.reactivex.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.quorum.PrivacyFlag;
import org.web3j.tx.Contract;

import java.util.Arrays;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@Service
public class PrivateStateValidation extends AbstractSpecImplementation {

    private static final Logger logger = LoggerFactory.getLogger(PrivateStateValidation.class);

    @Step("Deploy a <flag> contract `SimpleStorage` with initial value <initialValue> in <source>'s default account and it's private for <privateFor>, named this contract as <contractName>")
    public void deploySimpleContract(String flag, int initialValue, QuorumNode source, String privateFor, String contractName) {
        Contract contract = contractService.createSimpleContract(
            initialValue,
            source,
            null, Arrays.stream(privateFor.split(","))
                .map(s -> QuorumNode.valueOf(s))
                .collect(Collectors.toList()),
            AbstractService.DEFAULT_GAS_LIMIT,
            privacyService.parsePrivacyFlag(flag)
        ).blockingFirst();

        DataStoreFactory.getSpecDataStore().put(contractName, contract);
        DataStoreFactory.getScenarioDataStore().put(contractName, contract);
    }

    @Step("Deploy a public contract `C1` with initial value <initialValue> in <source>'s default account, named this contract as <contractName>")
    public void deployPublicC1Contract(int initialValue, QuorumNode source, String contractName) {
        Contract contract = nestedContractService.createPublicC1Contract(
            initialValue,
            source
        ).blockingFirst();

        DataStoreFactory.getSpecDataStore().put(contractName, contract);
        DataStoreFactory.getScenarioDataStore().put(contractName, contract);
    }

    @Step("Deploy a <flag> contract `C1` with initial value <initialValue> in <source>'s default account and it's private for <privateFor>, named this contract as <contractName>")
    public void deployC1Contract(String flag, int initialValue, QuorumNode source, String privateFor, String contractName) {
        Contract contract = nestedContractService.createC1Contract(
            initialValue,
            source,
            Arrays.stream(privateFor.split(","))
                .map(s -> QuorumNode.valueOf(s))
                .collect(Collectors.toList()),
            privacyService.parsePrivacyFlag(flag)
        ).blockingFirst();

        DataStoreFactory.getSpecDataStore().put(contractName, contract);
        DataStoreFactory.getScenarioDataStore().put(contractName, contract);
    }

    @Step("Deploy a <flag> contract `C2` with initial value <c1ContractName> in <source>'s default account and it's private for <privateFor>, named this contract as <contractName>")
    public void deployC2Contract(String flag, String c1ContractName, QuorumNode source, String privateFor, String contractName) {
        Contract c1 = mustHaveValue(c1ContractName, Contract.class);
        Contract contract = nestedContractService.createC2Contract(
            c1.getContractAddress(),
            source,
            Arrays.stream(privateFor.split(","))
                .map(s -> QuorumNode.valueOf(s))
                .collect(Collectors.toList()),
            privacyService.parsePrivacyFlag(flag)
        ).blockingFirst();

        DataStoreFactory.getSpecDataStore().put(contractName, contract);
        DataStoreFactory.getScenarioDataStore().put(contractName, contract);
    }

    @Step("Contract `C1`(<contractName>)'s `get()` function execution in <node> returns <expectedValue>")
    public void readC1Contract(String contractName, QuorumNode node, int expectedValue) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractName, Contract.class);

        // if a transactionHash has been stored then wait until node has executed it
        // TODO this is a temporary fix for particularly flaky tests where state is being read before it has been
        // updated - we should probably rework all tests to check that the state is ready before getting
        Optional<String> transactionHash = Optional.ofNullable(DataStoreFactory.getScenarioDataStore().get("transactionHash"))
            .map(Object::toString);
        transactionHash.ifPresent(h -> transactionService.waitForTransactionReceipt(node, h));

        int actualValue = nestedContractService.readC1Value(node, c.getContractAddress());

        assertThat(actualValue).isEqualTo(expectedValue);
    }

    @Step("Contract `C2`(<contractName>)'s `get()` function execution in <node> returns <expectedValue>")
    public void readC2Contract(String contractName, QuorumNode node, int expectedValue) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractName, Contract.class);
        int actualValue = nestedContractService.readC2Value(node, c.getContractAddress());

        assertThat(actualValue).isEqualTo(expectedValue);
    }

    @Step("Fail to execute contract `C2`(<contractName>)'s `get()` function in <node> with error <error>")
    public void failGetExecutionContract(String contractName, QuorumNode node, String error) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractName, Contract.class);
        try {
            int v = nestedContractService.readC2Value(node, c.getContractAddress());
        } catch(Exception e) {
            if (!e.toString().contains(error)) {
                throw e;
            }
            // We're supposed to catch this exception, good to go.
        }
    }

    @Step("Fail to execute <flag> contract `C2`(<contractName>)'s `restoreFromC1()` function in <node> and it's private for <privateFor>")
    public void failRestoreFromC1Execution(String flag, String contractName, QuorumNode node, String privateFor) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractName, Contract.class);

        assertThatThrownBy(
            () -> nestedContractService.restoreFromC1(node,
                Arrays.stream(privateFor.split(",")).map(s -> QuorumNode.valueOf(s)).collect(Collectors.toList()),
                c.getContractAddress(),
                privacyService.parsePrivacyFlag(flag)
            ).blockingFirst()
        ).as("Expected exception thrown")
            .isNotNull();
    }

    @Step("Execute <flag> contract `C2`(<contractName>)'s `restoreFromC1()` function in <source> and it's private for <privateFor>")
    public void succeedRestoreFromC1Execution(String flag, String contractName, QuorumNode source, String privateFor) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractName, Contract.class);
        TransactionReceipt receipt = nestedContractService.restoreFromC1(
            source,
            Arrays.stream(privateFor.split(",")).map(s -> QuorumNode.valueOf(s)).collect(Collectors.toList()),
            c.getContractAddress(),
            privacyService.parsePrivacyFlag(flag)
        ).blockingFirst();

        assertThat(receipt.getTransactionHash()).isNotBlank();
        assertThat(receipt.getBlockNumber()).isNotEqualTo(currentBlockNumber());
    }

    @Step("Fail to execute <flag> contract `C2`(<contractName>)'s `set()` function with new arbitrary value in <node> and it's private for <privateFor> with error <error>")
    public void failSetExecution(String flag, String contractName, QuorumNode node, String privateFor, String error) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractName, Contract.class);
        int arbitraryValue = new Random().nextInt(100) + 1000;

        assertThatThrownBy(
            () -> nestedContractService.updateC2Contract(
                node,
                Arrays.stream(privateFor.split(",")).map(s -> QuorumNode.valueOf(s)).collect(Collectors.toList()),
                c.getContractAddress(),
                arbitraryValue,
                privacyService.parsePrivacyFlag(flag)
            ).blockingFirst()
        ).as("Expected exception thrown")
            .hasMessageContaining(error);
    }

    @Step("Fail to execute <flag> simple contract(<contractName>)'s `set()` function with new arbitrary value in <node> and it's private for <privateFor> with error <error>")
    public void failSetExecutionSimpleContract(String flag, String contractName, QuorumNode node, String privateFor, String error) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractName, Contract.class);
        int arbitraryValue = new Random().nextInt(100) + 1000;

        assertThatThrownBy(
            () -> contractService.updateSimpleContract(
                node,
                Arrays.stream(privateFor.split(",")).map(s -> QuorumNode.valueOf(s)).collect(Collectors.toList()),
                c.getContractAddress(),
                arbitraryValue,
                privacyService.parsePrivacyFlag(flag)
            ).blockingFirst()
        ).as("Expected exception thrown")
            .hasMessageContaining(error);
    }

    @Step("Fire and forget execution of <flag> contract `C2`(<contractName>)'s `set()` function with new arbitrary value in <node> and it's private for <privateFor>")
    public void fireAndForget(String flag, String contractName, QuorumNode node, String privateFor) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractName, Contract.class);
        int arbitraryValue = new Random().nextInt(100) + 1000;

        TransactionReceipt receipt = nestedContractService.updateC2Contract(
            node,
            Arrays.stream(privateFor.split(",")).map(s -> QuorumNode.valueOf(s)).collect(Collectors.toList()),
            c.getContractAddress(),
            arbitraryValue,
            privacyService.parsePrivacyFlag(flag)
        ).onExceptionResumeNext(Observable.empty()).blockingFirst();
        if (receipt != null) {
            String txHashKey = contractName + "_transactionHash";
            DataStoreFactory.getSpecDataStore().put(txHashKey, receipt.getTransactionHash());
            DataStoreFactory.getScenarioDataStore().put(txHashKey, receipt.getTransactionHash());
        }
    }

    @Step("Fire and forget execution of simple contract(<contractName>)'s `set()` function with new arbitrary value in <node> and it's private for <privateFor>")
    public void fireAndForgetSimpleContractNoFlag(String contractName, QuorumNode node, String privateFor) {
        fireAndForgetSimpleContractArbitraryValue(PrivacyFlag.STANDARD_PRIVATE.toString(), contractName, node, privateFor);
    }

    @Step("Fire and forget execution of <flag> simple contract(<contractName>)'s `set()` function with new arbitrary value in <node> and it's private for <privateFor>")
    public void fireAndForgetSimpleContractArbitraryValue(String flag, String contractName, QuorumNode node, String privateFor) {
        int arbitraryValue = new Random().nextInt(100) + 1000;
        fireAndForgetSimpleContractWithValue(flag, contractName, String.valueOf(arbitraryValue), node, privateFor);
    }

    @Step("Fire and forget execution of <flag> simple contract(<contractName>)'s `set()` function with new value <value> in <node> and it's private for <privateFor>")
    public void fireAndForgetSimpleContractWithValue(String flag, String contractName, String value, QuorumNode node, String privateFor) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractName, Contract.class);
        int intValue = Integer.parseInt(value);

        TransactionReceipt receipt = contractService.updateSimpleContract(
            node,
            Arrays.stream(privateFor.split(",")).map(s -> QuorumNode.valueOf(s)).collect(Collectors.toList()),
            c.getContractAddress(),
            intValue,
            privacyService.parsePrivacyFlag(flag)
        ).onExceptionResumeNext(Observable.empty()).blockingFirst();
        if (receipt != null) {
            String txHashKey = contractName + "_transactionHash";
            DataStoreFactory.getSpecDataStore().put(txHashKey, receipt.getTransactionHash());
            DataStoreFactory.getScenarioDataStore().put(txHashKey, receipt.getTransactionHash());
        }
    }

    @Step("Execute contract `C2`(<contractName>)'s `set()` function with new value <newValue> in <source> and it's private for <privateFor>")
    public void updateNewValue(String contractName, int newValue, QuorumNode source, String privateFor) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractName, Contract.class);
        TransactionReceipt receipt = nestedContractService.updateC2Contract(
            source,
            Arrays.stream(privateFor.split(",")).map(s -> QuorumNode.valueOf(s)).collect(Collectors.toList()),
            c.getContractAddress(), newValue, PrivacyFlag.STANDARD_PRIVATE).blockingFirst();

        assertThat(receipt.getTransactionHash()).isNotBlank();
        assertThat(receipt.getBlockNumber()).isNotEqualTo(currentBlockNumber());

        TransactionReceipt receiptPrivateFor = transactionService.waitForTransactionReceipt(QuorumNode.valueOf(privateFor), receipt.getTransactionHash());
        assertThat(receiptPrivateFor.getBlockNumber()).isNotEqualTo(currentBlockNumber());
    }
}
