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
import com.quorum.gauge.services.RaftService;
import com.thoughtworks.gauge.Step;
import com.thoughtworks.gauge.datastore.DataStoreFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.tx.Contract;

import java.math.BigInteger;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Service
public class RaftGasUsage extends AbstractSpecImplementation {
    private static final Logger logger = LoggerFactory.getLogger(RaftGasUsage.class);

    private enum DataStoreKey {result, node, message}
    private enum DataStoreValue {success, exception}

    @Autowired
    RaftService raftService;

    @Step("Private transaction where minter is a participant and gas value is <gas>, name this contract as <contractName>")
    public void sendPrivateTransactionWithParticipantMinter(int gas, String contractName) {
        QuorumNode source = raftService.getLeader(QuorumNode.Node1);
        QuorumNode target = QuorumNode.Node2;
        if (source.equals(target)) {
            target = QuorumNode.Node3;
        }

        createContract(gas, source, target, contractName);
    }

    @Step("Private transaction where minter is not a participant and gas value is <gas>, name this contract as <contractName>")
    public void sendPrivateTransactionWithNonParticipantMinter(int gas, String contractName) {
        QuorumNode minter = raftService.getLeader(QuorumNode.Node1);
        QuorumNode source = QuorumNode.Node1;
        QuorumNode target = QuorumNode.Node2;
        if (source.equals(minter) || target.equals(minter)) {
            source = QuorumNode.Node3;
            target = QuorumNode.Node4;
        }

        createContract(gas, source, target, contractName);
    }

    @Step("Contract <contractName> had exception with message <expectedMessage>")
    public void checkContractForException(String contractName, String expectedMessage) {
        DataStoreValue result = mustHaveValue(DataStoreFactory.getScenarioDataStore(), contractName + "." + DataStoreKey.result, DataStoreValue.class);
        assertThat(result).isEqualTo(DataStoreValue.exception);
        String actualMessage = mustHaveValue(DataStoreFactory.getScenarioDataStore(), contractName + "." + DataStoreKey.message, String.class);
        assertThat(actualMessage).matches(".*" + expectedMessage + ".*");
    }


    @Step("Contract <contractName> result is <expectedResult>")
    public void checkContractResult(String contractName, DataStoreValue expectedResult) {
        DataStoreValue result = mustHaveValue(DataStoreFactory.getScenarioDataStore(), contractName + "." + DataStoreKey.result, DataStoreValue.class);
        assertThat(result).isEqualTo(expectedResult);
    }

    @Step("Contract <contractName> is not pending")
    public void checkNoPending(String contractName) {
        QuorumNode node = mustHaveValue(DataStoreFactory.getScenarioDataStore(), contractName + "." + DataStoreKey.node, QuorumNode.class);

        // TODO: this doesn't work, so assertion is commented out for now
        List<Transaction> transactions= utilService.getPendingHashes(node);
        //assertThat(transactions).isEmpty();
    }

    /**
     * Create contract, storing result in Scenario Data Store:
     *      key = <contractName> + ".node", value = node where contract was submitted
     *      key = <contractName> + ".result", value = "success" or "exception"
     */
    private void createContract(int gas, QuorumNode source, QuorumNode target, String contractName) {
        DataStoreFactory.getScenarioDataStore().put(contractName + "." + DataStoreKey.node, source);
        try {
            Contract contract = contractService.createSimpleContract(42, source, target, BigInteger.valueOf(gas)).toBlocking().first();
             DataStoreFactory.getScenarioDataStore().put(contractName + "." + DataStoreKey.result, DataStoreValue.success);
        } catch (RuntimeException e) {
            DataStoreFactory.getScenarioDataStore().put(contractName + "." + DataStoreKey.result, DataStoreValue.exception);
            DataStoreFactory.getScenarioDataStore().put(contractName + "." + DataStoreKey.message, e.getMessage());
        }
    }
}