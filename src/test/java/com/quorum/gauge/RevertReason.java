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

import com.quorum.gauge.core.AbstractSpecImplementation;
import com.quorum.gauge.services.IncreasingSimpleStorageContractService;
import com.thoughtworks.gauge.Step;
import com.thoughtworks.gauge.datastore.DataStoreFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.protocol.exceptions.TransactionException;
import org.web3j.tx.Contract;

import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@Service
public class RevertReason extends AbstractSpecImplementation {
    private static final Logger logger = LoggerFactory.getLogger(RevertReason.class);

    @Autowired
    private IncreasingSimpleStorageContractService increasingSimpleStorageContractService;

    @Step("Deploy IncreasingSimpleStorage contract with initial value <value> from <node>")
    public void deployIncreasingSimpleStorage(int value, String node) {
        Contract contract = increasingSimpleStorageContractService.createPublicIncreasingSimpleStorageContract(value, node, null, null, null).blockingFirst();
        DataStoreFactory.getScenarioDataStore().put("contractAddress", contract.getContractAddress());
    }

    @Step("Set value <value> from <node>")
    public void setValueOnContract(int value, String node) throws TransactionException {
        String contractAddress = DataStoreFactory.getScenarioDataStore().get("contractAddress").toString();
        increasingSimpleStorageContractService.setValue(contractAddress, value, node).blockingFirst();
    }

    @Step("Set value <value> from <node> fails with reason <reasonHash>")
    public void setValueOnContractFailsWithReason(int value, String node, String reasonHash) {
        String contractAddress = DataStoreFactory.getScenarioDataStore().get("contractAddress").toString();
        try {
            increasingSimpleStorageContractService.setValue(contractAddress, value, node).map(r -> r.getBlockNumber()).blockingFirst();
            fail("Expected to fail");
        } catch (RuntimeException err) {
            TransactionException exception = (TransactionException) err.getCause().getCause();
            assertThat(increasingSimpleStorageContractService.getRevertReasonTransactionReceipt(exception.getTransactionHash().get(), node).map(r -> r.getRevertReason()).blockingFirst()).isEqualTo(reasonHash);
        }
    }

    @Step("Get value from <node> matches <value>")
    public void validateValueOnContract(String node, int value) {
        String contractAddress = DataStoreFactory.getScenarioDataStore().get("contractAddress").toString();
        BigInteger savedValue = increasingSimpleStorageContractService.getValue(contractAddress, node).blockingFirst();
        assertThat(savedValue.intValue()).isEqualTo(value);
    }
}
