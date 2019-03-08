/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional debugrmation
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
import com.thoughtworks.gauge.Step;
import com.thoughtworks.gauge.datastore.DataStoreFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import org.web3j.tx.exceptions.ContractCallException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Service
@SuppressWarnings("unchecked")
public class SmartContractDualState extends AbstractSpecImplementation {
    private static final Logger logger = LoggerFactory.getLogger(SmartContractDualState.class);

    @Step("Deploy <contractName> smart contract with initial value <initialValue> from a default account in <node>, named this contract as <contractNameKey>")
    public void setupStorecAsPublicDependentContract(String contractName, int initialValue, QuorumNode node, String contractNameKey) {
        Contract c = contractService.createGenericStoreContract(node, contractName, initialValue, null, false, null).toBlocking().first();
        logger.debug("{} contract address is:{}", contractName, c.getContractAddress());

        assertThat(c.getContractAddress()).isNotBlank();

        DataStoreFactory.getSpecDataStore().put(contractNameKey, c);
        DataStoreFactory.getScenarioDataStore().put(contractNameKey, c);
        String typeKey = contractNameKey + "Type";
        DataStoreFactory.getSpecDataStore().put(typeKey, contractName);
        DataStoreFactory.getScenarioDataStore().put(typeKey, contractName);
    }

    @Step("Deploy <contractName> smart contract with initial value <initialValue> from a default account in <node> and it's private for <target>, named this contract as <contractNameKey>")
    public void setupStorecAsPrivateDependentContract(String contractName, int initialValue, QuorumNode node, QuorumNode target, String contractNameKey) {
        Contract c = contractService.createGenericStoreContract(node, contractName, initialValue, null, true, target).toBlocking().first();
        logger.debug("{} contract address is:{}", contractName, c.getContractAddress());

        assertThat(c.getContractAddress()).isNotBlank();

        DataStoreFactory.getSpecDataStore().put(contractNameKey, c);
        DataStoreFactory.getScenarioDataStore().put(contractNameKey, c);
        String typeKey = contractNameKey + "Type";
        DataStoreFactory.getSpecDataStore().put(typeKey, contractName);
        DataStoreFactory.getScenarioDataStore().put(typeKey, contractName);

    }

    @Step("Deploy <contractName> smart contract with contract <depContractName> initial value <initialValue> from a default account in <node>, named this contract as <contractNameKey>")
    public void setupStoreaOrStorebAsPublicContract(String contractName, String depContractName, int initialValue, QuorumNode node, String contractNameKey) {
        Contract dc = mustHaveValue(DataStoreFactory.getSpecDataStore(), depContractName, Contract.class);
        Contract c = contractService.createGenericStoreContract(node, contractName, initialValue, dc.getContractAddress(), false, null).toBlocking().first();
        logger.debug("{} contract address is:{} with dc contract address: {}", contractName, c.getContractAddress(), dc.getContractAddress());

        assertThat(c.getContractAddress()).isNotBlank();

        DataStoreFactory.getSpecDataStore().put(contractNameKey, c);
        DataStoreFactory.getScenarioDataStore().put(contractNameKey, c);
        String typeKey = contractNameKey + "Type";
        DataStoreFactory.getSpecDataStore().put(typeKey, contractName);
        DataStoreFactory.getScenarioDataStore().put(typeKey, contractName);
    }

    @Step("Deploy <contractName> smart contract with contract <dependentContractName> initial value <initialValue> from a default account in <source> and it's private for <target>, named this contract as <contractNameKey>")
    public void setupStoreaOrStorebAsPrivateContract(String contractName, String dependentContractName, int initialValue, QuorumNode source, QuorumNode target, String contractNameKey) {
        Contract dc = mustHaveValue(DataStoreFactory.getSpecDataStore(), dependentContractName, Contract.class);
        logger.debug("Setting up contract from {} to {}", source, target);
        Contract contract = contractService.createGenericStoreContract(source, contractName, initialValue, dc.getContractAddress(), true, target).toBlocking().first();
        logger.debug("{} contract address is:{} with dc contract address: {}", contractName, contract.getContractAddress(), dc.getContractAddress());

        assertThat(contract.getContractAddress()).isNotBlank();

        DataStoreFactory.getSpecDataStore().put(contractNameKey, contract);
        DataStoreFactory.getScenarioDataStore().put(contractNameKey, contract);
        String typeKey = contractNameKey + "Type";
        DataStoreFactory.getSpecDataStore().put(typeKey, contractName);
        DataStoreFactory.getScenarioDataStore().put(typeKey, contractName);
    }

    @Step("<contractNameKey>'s <methodName> function execution in <node> returns <expectedValue>")
    public void verifyStoreContractGetValue(String contractNameKey, String methodName, QuorumNode node, int expectedValue) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractNameKey, Contract.class);
        String contractName = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractNameKey + "Type", String.class);
        int actualValue = contractService.readGenericStoreContractGetValue(node, c.getContractAddress(), contractName, methodName);
        logger.debug("{} {} {} = {}", contractNameKey, contractName, methodName, actualValue);

        assertThat(actualValue).isEqualTo(expectedValue);
    }

    @Step("<contractNameKey>'s <methodName> function execution in <node> with value <value> and its private for <target>")
    public void setStoreContractValueInPrivate(String contractNameKey, String methodName, QuorumNode node, int value, QuorumNode target) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractNameKey, Contract.class);
        String contractName = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractNameKey + "Type", String.class);
        logger.debug(" contract address is:{}", contractNameKey, c.getContractAddress());
        TransactionReceipt tr = contractService.setGenericStoreContractSetValue(node, c.getContractAddress(), contractName, methodName, value, true, target).toBlocking().first();
        logger.debug("{} {} {}, txHash = {}", contractNameKey, contractName, methodName, tr.getTransactionHash());

        assertThat(tr.getTransactionHash()).isNotBlank();
    }

    @Step("<contractNameKey>'s <methodName> function execution in <node> with value <value> and its private for <target>, should fail")
    public void setStoreContractValueInPrivateShouldFail(String contractNameKey, String methodName, QuorumNode node, int value, QuorumNode target) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractNameKey, Contract.class);
        String contractName = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractNameKey + "Type", String.class);
        logger.debug("{} contract address is:{}", contractNameKey, c.getContractAddress());
        try {
            TransactionReceipt tr = contractService.setGenericStoreContractSetValue(node, c.getContractAddress(), contractName, methodName, value, true, target).toBlocking().first();
            logger.debug("{} {} {}, txHash = {}", contractNameKey, contractName, methodName, tr.getTransactionHash());
        } catch (Exception txe) {
            assertThat(txe).hasMessageContaining("Transaction has failed");
        }
    }

    @Step("<contractNameKey>'s <methodName> function execution in <node> with value <value>")
    public void setStoreContractValueInPublic(String contractNameKey, String methodName, QuorumNode node, int value) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractNameKey, Contract.class);
        String contractName = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractNameKey + "Type", String.class);
        logger.debug("{} contract address is:{}, {} {}", contractNameKey, c.getContractAddress(), methodName, value);
        TransactionReceipt tr = contractService.setGenericStoreContractSetValue(node, c.getContractAddress(), contractName, methodName, value, false, null).toBlocking().first();
        logger.debug("{} {} {}, txHash = {}", contractNameKey, contractName, methodName, tr.getTransactionHash());

        assertThat(tr.getTransactionHash()).isNotBlank();
    }

    @Step("<contractNameKey>'s <methodName> function execution in <node> should fail")
    public void setStoreContractValueInPublic(String contractNameKey, String methodName, QuorumNode node) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractNameKey, Contract.class);
        String contractName = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractNameKey + "Type", String.class);
        try {
            contractService.readGenericStoreContractGetValue(node, c.getContractAddress(), contractName, methodName);
        } catch (Exception e) {
            assertThat(e).hasCauseInstanceOf(ContractCallException.class);
        }
    }

    @Step("<contractNameKey>'s <methodName> function execution in <node> with value <value>, should fail")
    public void setStoreContractValueInPublicShouldFail(String contractNameKey, String methodName, QuorumNode node, int value) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractNameKey, Contract.class);
        String contractName = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractNameKey + "Type", String.class);
        logger.debug("{} contract address is:{}", contractNameKey, c.getContractAddress());
        try {
            TransactionReceipt tr = contractService.setGenericStoreContractSetValue(node, c.getContractAddress(), contractName, methodName, value, false, null).toBlocking().first();
            logger.debug("{} {} {}, txHash = {}", contractNameKey, contractName, methodName, tr.getTransactionHash());
        } catch (Exception txe) {
            assertThat(txe).hasMessageContaining("Transaction has failed");
        }
    }
}
