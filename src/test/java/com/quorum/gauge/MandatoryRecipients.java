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
import com.quorum.gauge.common.RetryWithDelay;
import com.quorum.gauge.core.AbstractSpecImplementation;
import com.quorum.gauge.services.AbstractService;
import com.thoughtworks.gauge.Step;
import com.thoughtworks.gauge.datastore.DataStoreFactory;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.quorum.PrivacyFlag;
import org.web3j.tx.Contract;

import java.util.Arrays;
import java.util.Random;
import java.util.stream.Collectors;

import static org.assertj.core.api.AssertionsForClassTypes.*;

@Service
public class MandatoryRecipients extends AbstractSpecImplementation {

    @Step("Deploy a <flag> contract `SimpleStorage` with initial value <initialValue> in <source>'s default account and it's private for <privateFor> and mandatory for <mandatoryFor>, named this contract as <contractName>")
    public void deploySimpleContract(String flag, int initialValue, QuorumNode source, String privateFor, String mandatoryFor, String contractName) {
        Contract contract = contractService.createSimpleContract(
            initialValue,
            source,
            null, Arrays.stream(privateFor.split(","))
                .map(s -> QuorumNode.valueOf(s))
                .collect(Collectors.toList()),
            AbstractService.DEFAULT_GAS_LIMIT,
            privacyService.parsePrivacyFlag(flag),
            Arrays.stream(mandatoryFor.split(","))
                .map(QuorumNode::valueOf)
                .collect(Collectors.toList())
        ).blockingFirst();

        DataStoreFactory.getSpecDataStore().put(contractName, contract);
        DataStoreFactory.getScenarioDataStore().put(contractName, contract);
    }

    @Step("Deploy a <flag> contract `SimpleStorage` with initial value <initialValue> in <source>'s default account and it's private for <privateFor> and mandatory for <mandatoryFor> fails with message <failureMessage>")
    public void deploySimpleContractFails(String flag, int initialValue, QuorumNode source, String privateFor, String mandatoryFor, String failureMessage) {
        assertThatThrownBy(() -> contractService.createSimpleContract(
            initialValue,
            source,
            null, Arrays.stream(privateFor.split(","))
                .map(s -> QuorumNode.valueOf(s))
                .collect(Collectors.toList()),
            AbstractService.DEFAULT_GAS_LIMIT,
            privacyService.parsePrivacyFlag(flag),
            Arrays.stream(mandatoryFor.split(","))
                .map(QuorumNode::valueOf)
                .collect(Collectors.toList())
        ).blockingFirst())
            .as("Expected exception thrown")
            .hasMessageContaining(failureMessage);
    }

    @Step("Deploy a MandatoryRecipients contract `SimpleStorage` with no mandatory recipients data fails with message <failureMessage>")
    public void deploySimpleContractMissingDataFails(String failureMessage) {
        assertThatThrownBy(() -> contractService.createSimpleContract(
            42,
            QuorumNode.Node1,
            null,
            Arrays.stream("Node2".split(","))
                .map(s -> QuorumNode.valueOf(s))
                .collect(Collectors.toList()),
            AbstractService.DEFAULT_GAS_LIMIT,
            PrivacyFlag.MANDATORY_FOR
        ).blockingFirst())
            .as("Expected exception thrown")
            .hasMessageContaining(failureMessage);
    }

    @Step("Fail to execute <flag> simple contract(<contractName>)'s `set()` function with new arbitrary value in <node> and it's private for <privateFor> and mandatory for <mandatoryFor> with error <error>")
    public void updateSimpleContractFails(String flag, String contractName, QuorumNode node, String privateFor, String mandatoryFor, String error) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractName, Contract.class);
        int arbitraryValue = new Random().nextInt(100) + 1000;

        assertThatThrownBy(
            () -> contractService.updateSimpleContractWithMandatoryRecipients(
                    node,
                    Arrays.stream(privateFor.split(",")).map(s -> QuorumNode.valueOf(s)).collect(Collectors.toList()),
                    c.getContractAddress(),
                    arbitraryValue,
                    privacyService.parsePrivacyFlag(flag),
                    Arrays.stream(mandatoryFor.split(",")).map(s -> QuorumNode.valueOf(s)).collect(Collectors.toList()))
                .blockingFirst()
        ).as("Expected exception thrown")
            .hasMessageContaining(error);
    }

    @Step("Fail to execute <flag> simple contract(<contractName>)'s `set()` function with new arbitrary value in <node> and it's private for <privateFor> and no mandatory recipients with error <error>")
    public void updateSimpleContractMissingDataFails(String flag, String contractName, QuorumNode node, String privateFor, String error) {
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

    @Step("Fire and forget execution of <flag> simple contract(<contractName>)'s `set()` function with new value <value> in <node> and it's private for <privateFor> and mandatory for <mandatoryFor>")
    public void updateSimpleContract(String flag, String contractName, String value, QuorumNode node, String privateFor, String mandatoryFor) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractName, Contract.class);
        int intValue = Integer.parseInt(value);

        TransactionReceipt receipt = contractService.updateSimpleContractWithMandatoryRecipients(
                node,
                Arrays.stream(privateFor.split(",")).map(s -> QuorumNode.valueOf(s)).collect(Collectors.toList()),
                c.getContractAddress(),
                intValue,
                privacyService.parsePrivacyFlag(flag),
                Arrays.stream(mandatoryFor.split(",")).map(s -> QuorumNode.valueOf(s)).collect(Collectors.toList()))
            .blockingFirst();
        if (receipt != null) {
            String txHashKey = contractName + "_transactionHash";
            DataStoreFactory.getSpecDataStore().put(txHashKey, receipt.getTransactionHash());
            DataStoreFactory.getScenarioDataStore().put(txHashKey, receipt.getTransactionHash());
        }
    }

    @Step("Execute <contractName>'s `get()` function in <node> returns <expectedValue>")
    public void getValueSimpleContract(String contractName, QuorumNode node, int expectedValue) {
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

    @Step("Deploy a MR contract `C1` with initial value <initialValue> in <source>'s default account and it's private for <privateFor>, mandatory for <mandatoryFor> named this contract as <contractName>")
    public void deployC1Contract(int initialValue, QuorumNode source, String privateFor, String mandatoryFor, String contractName) {
        Contract contract = nestedContractService.createC1ContractWithMandatoryRecipients(
            initialValue,
            source,
            Arrays.stream(privateFor.split(","))
                .map(s -> QuorumNode.valueOf(s))
                .collect(Collectors.toList()),
            Arrays.stream(mandatoryFor.split(","))
                .map(s -> QuorumNode.valueOf(s))
                .collect(Collectors.toList()),
            PrivacyFlag.MANDATORY_FOR
        ).blockingFirst();

        DataStoreFactory.getSpecDataStore().put(contractName, contract);
        DataStoreFactory.getScenarioDataStore().put(contractName, contract);
    }

    @Step("Deploy a MR contract `C2` with initial value <c1ContractName> in <source>'s default account and it's private for <privateFor> mandatory for <mandatoryFor>, named this contract as <contractName>")
    public void deployC2Contract(String c1ContractName, QuorumNode source, String privateFor, String mandatoryFor, String contractName) {
        Contract c1 = mustHaveValue(c1ContractName, Contract.class);
        Contract contract = nestedContractService.createC2ContractWithMandatoryRecipients(
            c1.getContractAddress(),
            source,
            Arrays.stream(privateFor.split(","))
                .map(s -> QuorumNode.valueOf(s))
                .collect(Collectors.toList()),
            Arrays.stream(mandatoryFor.split(","))
                .map(s -> QuorumNode.valueOf(s))
                .collect(Collectors.toList()),
            PrivacyFlag.MANDATORY_FOR
        ).blockingFirst();

        DataStoreFactory.getSpecDataStore().put(contractName, contract);
        DataStoreFactory.getScenarioDataStore().put(contractName, contract);
    }


}
