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

import com.quorum.gauge.common.PrivacyFlag;
import com.quorum.gauge.common.QuorumNetworkProperty;
import com.quorum.gauge.common.QuorumNode;
import com.quorum.gauge.core.AbstractSpecImplementation;
import com.quorum.gauge.services.AbstractService;
import com.thoughtworks.gauge.Step;
import com.thoughtworks.gauge.datastore.DataStoreFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Service
public class StorageMasterSmartContract extends AbstractSpecImplementation {
    private static final Logger logger = LoggerFactory.getLogger(StorageMasterSmartContract.class);

    @Step("Deploy a public storage master contract in <source>'s default account, named this contract as <contractName>")
    public void setupStorageMasterPublicContract(QuorumNetworkProperty.Node source, String contractName) {
        saveCurrentBlockNumber();
        logger.debug("Setting up public storage master contract from {}", source);
        Contract contract = storageMasterService.createStorageMasterPublicContract(
            source, AbstractService.DEFAULT_GAS_LIMIT).blockingFirst();

        DataStoreFactory.getSpecDataStore().put(contractName, contract);
        DataStoreFactory.getScenarioDataStore().put(contractName, contract);
    }

    @Step("Deploy a simple storage from master storage contract <storageMaster> in <source>'s default account, named this contract as <contractName>")
    public void setupSimpleStorageFromStorageMasterPublicContract(String storageMaster, QuorumNetworkProperty.Node source, String contractName) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), storageMaster, Contract.class);
        saveCurrentBlockNumber();
        logger.debug("Setting up simple storage from public storage master contract from {}", source);
        Contract ssChild = storageMasterService.createSimpleStorageFromStorageMasterPublic(
            source, c.getContractAddress(), AbstractService.DEFAULT_GAS_LIMIT, 10).blockingFirst();

        DataStoreFactory.getSpecDataStore().put(contractName, ssChild);
        DataStoreFactory.getScenarioDataStore().put(contractName, ssChild);
    }

    @Step("Deploy a <privacyFlags> storage master contract in <source>'s default account and it's private for <target>, named this contract as <contractName>")
    public void setupStorageMasterContract(String privacyFlags, QuorumNode source, QuorumNode target, String contractName) {
        saveCurrentBlockNumber();
        logger.debug("Setting up storage master from {} to {}", source, target);
        Contract contract = storageMasterService.createStorageMasterContract(
            source,
            Collections.singletonList(target),
            AbstractService.DEFAULT_GAS_LIMIT,
            Arrays.stream(privacyFlags.split(",")).map(PrivacyFlag::valueOf).collect(Collectors.toList())
        ).blockingFirst();

        DataStoreFactory.getSpecDataStore().put(contractName, contract);
        DataStoreFactory.getScenarioDataStore().put(contractName, contract);
    }

    @Step("Deploy a <privacyFlags> simple storage from master storage contract <storageMaster> in <source>'s default account and it's private for <target>, named this contract as <contractName>")
    public void setupSimpleStorageFromStorageMasterContract(String privacyFlags, String storageMaster, QuorumNode source, QuorumNode target, String contractName) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), storageMaster, Contract.class);
        saveCurrentBlockNumber();
        logger.debug("Setting up simple storage from storage master contract from {}", source);
        Contract ssChild = storageMasterService.createSimpleStorageFromStorageMaster(
            source, Collections.singletonList(target), c.getContractAddress(), AbstractService.DEFAULT_GAS_LIMIT,
            10,
            Arrays.stream(privacyFlags.split(",")).map(PrivacyFlag::valueOf).collect(Collectors.toList())
        ).blockingFirst();

        DataStoreFactory.getSpecDataStore().put(contractName, ssChild);
        DataStoreFactory.getScenarioDataStore().put(contractName, ssChild);
    }

    @Step("Deploy a <privacyFlags> simple storage C2C3 with value <value> from master storage contract <storageMaster> in <source>'s default account and it's private for <target>")
    public void setupSimpleStorageC2C3FromStorageMasterContract(String privacyFlags, int value, String storageMaster, QuorumNode source, QuorumNode target) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), storageMaster, Contract.class);
        saveCurrentBlockNumber();
        logger.debug("Setting up simple storage C2C3 from storage master contract from {}", source);
        TransactionReceipt receipt = storageMasterService.createSimpleStorageC2C3FromStorageMaster(
            source, Collections.singletonList(target), c.getContractAddress(), AbstractService.DEFAULT_GAS_LIMIT,
            value,
            Arrays.stream(privacyFlags.split(",")).map(PrivacyFlag::valueOf).collect(Collectors.toList())
        ).blockingFirst();

        assertThat(receipt.isStatusOK()).isTrue();
    }

    @Step("Invoke a <privacyFlags> setC2C3Value with value <value> in master storage contract <storageMaster> in <source>'s default account and it's private for <target>")
    public void setC2C3FromStorageMasterPublicContract(String privacyFlags, int value, String storageMaster, QuorumNode source, QuorumNode target) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), storageMaster, Contract.class);
        saveCurrentBlockNumber();
        logger.debug("Invoking setC2V3Value in storage master contract from {}", source);
        TransactionReceipt receipt = storageMasterService.setC2C3ValueFromStorageMaster(
            source, Collections.singletonList(target), c.getContractAddress(), AbstractService.DEFAULT_GAS_LIMIT,
            value,
            Arrays.stream(privacyFlags.split(",")).map(PrivacyFlag::valueOf).collect(Collectors.toList())
        ).blockingFirst();
    }


    @Step("<contractName>'s `getC2C3Value()` function execution in <node> returns <expectedValue>")
    public void getC2C3FromStorageMasterPublicContract(String storageMaster, QuorumNode source, int expectedValue) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), storageMaster, Contract.class);
        saveCurrentBlockNumber();
        logger.debug("Invoking getC2V3Value in storage master contract from {}", source);
        final int value = storageMasterService.readStorageMasterC2C3Value(source, c.getContractAddress());
        assertThat(value).isEqualTo(expectedValue);
    }

}
