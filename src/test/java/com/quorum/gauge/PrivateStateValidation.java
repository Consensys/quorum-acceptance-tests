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
import com.quorum.gauge.ext.PrivateContractFlag;
import com.quorum.gauge.services.AbstractService;
import com.thoughtworks.gauge.Step;
import com.thoughtworks.gauge.datastore.DataStoreFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@Service
public class PrivateStateValidation extends AbstractSpecImplementation {

    private static final Logger logger = LoggerFactory.getLogger(PrivateStateValidation.class);

    @Step("Deploy a <flag> contract `SimpleStorage` with initial value <initialValue> in <source>'s default account and it's private for <privateFor>, named this contract as <contractName>")
    public void deploySimpleContract(PrivateContractFlag flag, int initialValue, QuorumNode source, String privateFor, String contractName) {
        Contract contract = contractService.createSimpleContract(
            initialValue,
            source,
            Arrays.stream(privateFor.split(","))
                .map(s -> QuorumNode.valueOf(s))
                .collect(Collectors.toList()),
            AbstractService.DEFAULT_GAS_LIMIT,
            flag
        ).toBlocking().first();

        DataStoreFactory.getSpecDataStore().put(contractName, contract);
        DataStoreFactory.getScenarioDataStore().put(contractName, contract);
    }

    @Step("Deploy a <flag> contract `C1` with initial value <initialValue> in <source>'s default account and it's private for <privateFor>, named this contract as <contractName>")
    public void deployC1Contract(PrivateContractFlag flag, int initialValue, QuorumNode source, String privateFor, String contractName) {
        Contract contract = nestedContractService.createC1Contract(
            initialValue,
            source,
            Arrays.stream(privateFor.split(","))
                .map(s -> QuorumNode.valueOf(s))
                .collect(Collectors.toList()),
            flag
        ).toBlocking().first();

        DataStoreFactory.getSpecDataStore().put(contractName, contract);
        DataStoreFactory.getScenarioDataStore().put(contractName, contract);
    }

    @Step("Deploy a <flag> contract `C2` with initial value <c1ContractName> in <source>'s default account and it's private for <privateFor>, named this contract as <contractName>")
    public void deployC2Contract(PrivateContractFlag flag, String c1ContractName, QuorumNode source, String privateFor, String contractName) {
        Contract c1 = mustHaveValue(c1ContractName, Contract.class);
        Contract contract = nestedContractService.createC2Contract(
            c1.getContractAddress(),
            source,
            Arrays.stream(privateFor.split(","))
                .map(s -> QuorumNode.valueOf(s))
                .collect(Collectors.toList()),
            flag
        ).toBlocking().first();

        DataStoreFactory.getSpecDataStore().put(contractName, contract);
        DataStoreFactory.getScenarioDataStore().put(contractName, contract);
    }

    @Step("Contract `C1`(<contractName>)'s `get()` function execution in <node> returns <expectedValue>")
    public void readC1Contract(String contractName, QuorumNode node, int expectedValue) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractName, Contract.class);
        int actualValue = nestedContractService.readC1Value(node, c.getContractAddress());

        assertThat(actualValue).isEqualTo(expectedValue);
    }

    @Step("Contract `C2`(<contractName>)'s `get()` function execution in <node> returns <expectedValue>")
    public void readC2Contract(String contractName, QuorumNode node, int expectedValue) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractName, Contract.class);
        int actualValue = nestedContractService.readC2Value(node, c.getContractAddress());

        assertThat(actualValue).isEqualTo(expectedValue);
    }

    @Step("Fail to execute contract `C2`(<contractName>)'s `get()` function in <node>")
    public void failGetExecution(String contractName, QuorumNode node) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractName, Contract.class);

        assertThatThrownBy(
            () -> nestedContractService.readC2Value(node, c.getContractAddress())
        ).as("Expected exception thrown")
            .isNotNull();
    }

    @Step("Fail to execute contract `C2`(<contractName>)'s `set()` function with new arbitrary value in <node> and it's private for <privateFor>")
    public void failSetExecution(String contractName, QuorumNode node, String privateFor) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractName, Contract.class);
        String[] participants = privateFor.split(",");
        int arbitraryValue = new Random().nextInt(100) + 1000;

        List<Observable<TransactionReceipt>> receipts = Arrays.stream(participants)
            .map(s -> QuorumNode.valueOf(s))
            .map(target -> nestedContractService.updateC2Contract(node, target, c.getContractAddress(), arbitraryValue).subscribeOn(Schedulers.io()))
            .collect(Collectors.toList());

        assertThatThrownBy(
            () -> Observable.zip(receipts, (args) -> null).toBlocking().first()
        ).as("Expected exception thrown")
            .isNotNull();
    }

    @Step("Fail to execute simple contract(<contractName>)'s `set()` function with new arbitrary value in <node> and it's private for <privateFor>")
    public void failSetExecutionSimpleContract(String contractName, QuorumNode node, String privateFor) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractName, Contract.class);
        String[] participants = privateFor.split(",");
        int arbitraryValue = new Random().nextInt(100) + 1000;

        List<Observable<TransactionReceipt>> receipts = Arrays.stream(participants)
            .map(s -> QuorumNode.valueOf(s))
            .map(target -> contractService.updateSimpleContract(node, target, c.getContractAddress(), arbitraryValue).subscribeOn(Schedulers.io()))
            .collect(Collectors.toList());

        assertThatThrownBy(
            () -> Observable.zip(receipts, (args) -> null).toBlocking().first()
        ).as("Expected exception thrown")
            .isNotNull();
    }

    @Step("Fire and forget execution of contract `C2`(<contractName>)'s `set()` function with new arbitrary value in <node> and it's private for <privateFor>")
    public void fireAndForget(String contractName, QuorumNode node, String privateFor) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractName, Contract.class);
        String[] participants = privateFor.split(",");
        int arbitraryValue = new Random().nextInt(100) + 1000;

        List<Observable<TransactionReceipt>> receipts = Arrays.stream(participants)
            .map(s -> QuorumNode.valueOf(s))
            .map(target -> nestedContractService.updateC1Contract(node, target, c.getContractAddress(), arbitraryValue).subscribeOn(Schedulers.io()))
            .collect(Collectors.toList());

        Observable.zip(receipts, (args) -> {
            Arrays.stream(args)
                .map(o -> (TransactionReceipt) o)
                .forEach(tr -> logger.debug(tr.getTransactionHash()));
            return null;
        }).doOnError(e -> logger.debug("Got exception but will ignore. Exception is {}", e.getMessage())).toBlocking().first();
    }

    @Step("Fire and forget execution of simple contract(<contractName>)'s `set()` function with new arbitrary value in <node> and it's private for <privateFor>")
    public void fireAndForgetSimpleContract(String contractName, QuorumNode node, String privateFor) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractName, Contract.class);
        String[] participants = privateFor.split(",");
        int arbitraryValue = new Random().nextInt(100) + 1000;

        List<Observable<TransactionReceipt>> receipts = Arrays.stream(participants)
            .map(s -> QuorumNode.valueOf(s))
            .map(target -> contractService.updateSimpleContract(node, target, c.getContractAddress(), arbitraryValue).subscribeOn(Schedulers.io()))
            .collect(Collectors.toList());

        Observable.zip(receipts, (args) -> {
            Arrays.stream(args)
                .map(o -> (TransactionReceipt) o)
                .forEach(tr -> logger.debug(tr.getTransactionHash()));
            return null;
        }).onExceptionResumeNext(Observable.just(null)).first().toBlocking();
    }

    @Step("Execute contract `C2`(<contractName>)'s `set()` function with new value <newValue> in <source> and it's private for <target>")
    public void updateNewValue(String contractName, int newValue, QuorumNode source, QuorumNode target) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractName, Contract.class);
        TransactionReceipt receipt = nestedContractService.updateC2Contract(source, target, c.getContractAddress(), newValue).toBlocking().first();

        assertThat(receipt.getTransactionHash()).isNotBlank();
        assertThat(receipt.getBlockNumber()).isNotEqualTo(currentBlockNumber());
    }
}
