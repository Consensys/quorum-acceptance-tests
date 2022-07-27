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
import com.thoughtworks.gauge.Gauge;
import com.thoughtworks.gauge.Step;
import com.thoughtworks.gauge.datastore.DataStoreFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.stream.Collectors;

@Service
@SuppressWarnings("unchecked")
public class NestedPrivateContract extends AbstractSpecImplementation {
    private static final Logger logger = LoggerFactory.getLogger(NestedPrivateContract.class);

    @Step("Deploy a C1 contract with initial value <initialValue> in <source>'s default account and it's private for <target>, named this contract as <contractName>")
    public void setupC1Contract(int initialValue, QuorumNode source, QuorumNode target, String contractName) {
        logger.debug("Setting up contract from {} to {}", source, target);
        saveCurrentBlockNumber();
        Contract contract = nestedContractService.createC1Contract(initialValue, source, target).blockingFirst();

        DataStoreFactory.getSpecDataStore().put(contractName, contract);
        DataStoreFactory.getScenarioDataStore().put(contractName, contract);
    }

    @Step("Deploy a <privacyFlag> C1 contract with initial value <initialValue> in <source>'s default account and it's private for <target>, named this contract as <contractName>")
    public void setupC1Contract(String privacyFlag, int initialValue, QuorumNode source, QuorumNode target, String contractName) {
        logger.debug("Setting up contract from {} to {}", source, target);
        saveCurrentBlockNumber();
        Contract contract = nestedContractService.createC1Contract(
            initialValue,
            source,
            Arrays.asList(target),
            privacyService.parsePrivacyFlag(privacyFlag)
        ).blockingFirst();

        DataStoreFactory.getSpecDataStore().put(contractName, contract);
        DataStoreFactory.getScenarioDataStore().put(contractName, contract);
    }

    @Step("Deploy a C2 contract with initial value <c1Address> in <source>'s default account and it's private for <target>, named this contract as <contractName>")
    public void setupC2Contract(String c1Address, QuorumNode source, QuorumNode target, String contractName) {
        logger.debug("Setting up contract from {} to {}", source, target);
        Contract c1 = (Contract) DataStoreFactory.getSpecDataStore().get(c1Address);
        Contract contract = nestedContractService.createC2Contract(c1.getContractAddress(), source, target).blockingFirst();

        DataStoreFactory.getSpecDataStore().put(contractName, contract);
        DataStoreFactory.getScenarioDataStore().put(contractName, contract);
    }

    @Step("Deploy a <privacyFlag> C2 contract with initial value <c1Address> in <source>'s default account and it's private for <target>, named this contract as <contractName>")
    public void setupC2Contract(String privacyFlag, String c1Address, QuorumNode source, QuorumNode target, String contractName) {
        logger.debug("Setting up contract from {} to {}", source, target);
        Contract c1 = (Contract) DataStoreFactory.getSpecDataStore().get(c1Address);
        Contract contract = nestedContractService.createC2Contract(
            c1.getContractAddress(),
            source,
            Arrays.asList(target),
            privacyService.parsePrivacyFlag(privacyFlag)
        ).blockingFirst();

        DataStoreFactory.getSpecDataStore().put(contractName, contract);
        DataStoreFactory.getScenarioDataStore().put(contractName, contract);
    }

    @Step("Execute <privacyFlag> <contractName>'s `newContractC2()` function with new value <newValue> in <source> and it's private for <target>")
    public void callNewContractC2(String privacyFlag, String contractName, int newValue, QuorumNode source, String target) {
        Contract c1 = mustHaveValue(contractName, Contract.class);

        TransactionReceipt receipt = nestedContractService.newContractC2(
            source,
            Arrays.stream(target.split(",")).map(s -> QuorumNode.valueOf(s)).collect(Collectors.toList()),
            c1.getContractAddress(),
            BigInteger.valueOf(newValue),
            privacyService.parsePrivacyFlag(privacyFlag)).blockingFirst();
        Gauge.writeMessage("Transaction Hash %s", receipt.getTransactionHash());
        DataStoreFactory.getScenarioDataStore().put("transactionHash", receipt.getTransactionHash());
    }
}
