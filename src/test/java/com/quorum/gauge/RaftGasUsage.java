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

    @Autowired
    RaftService raftService;

    @Step("Get number of nodes and store as <storageKey>")
    public void getPeerCount(String storageKey) {
        int peerCount = utilService.getNumberOfNodes(QuorumNode.Node1);
        DataStoreFactory.getScenarioDataStore().put(storageKey, peerCount);
    }

    @Step("Check <storageKey> nodes are still running")
    public void checkPeerCount(String storageKey) {
        int currentPeerCount = utilService.getNumberOfNodes(QuorumNode.Node1);
        int expectedPeerCount = mustHaveValue(DataStoreFactory.getScenarioDataStore(), storageKey, Integer.class);
        assertThat(currentPeerCount).isEqualTo(expectedPeerCount);
    }

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

    @Step("Public transaction from non-minter where gas value is <gas>, name this contract as <contractName>")
    public void sendPublicTransactionFromNonMinter(int gas, String contractName) {
        QuorumNode source = QuorumNode.Node1;
        QuorumNode minter = raftService.getLeader(QuorumNode.Node1);
        if (source.equals(minter)) {
            source = QuorumNode.Node2;
        }
        DataStoreFactory.getScenarioDataStore().put("source_node", source);

        // Save account balances on source node & minter, prior to transaction
        BigInteger sourceBalance = accountService.getDefaultAccountBalance(source).blockingFirst().getBalance();
        BigInteger minterBalance = accountService.getDefaultAccountBalance(minter).blockingFirst().getBalance();
        DataStoreFactory.getScenarioDataStore().put("source_balance", sourceBalance);
        DataStoreFactory.getScenarioDataStore().put("minter_balance", minterBalance);

        createContract(gas, source, null, contractName);
    }

    @Step("Private transaction from non-minter where gas value is <gas>, name this contract as <contractName>")
    public void sendPrivateTransactionFromNonMinter(int gas, String contractName) {
        QuorumNode source = QuorumNode.Node1;
        QuorumNode minter = raftService.getLeader(QuorumNode.Node1);
        if (source.equals(minter)) {
            source = QuorumNode.Node2;
        }
        QuorumNode target = QuorumNode.Node3;
        DataStoreFactory.getScenarioDataStore().put("source_node", source);

        // Save account balances on source node & minter, prior to transaction
        BigInteger sourceBalance = accountService.getDefaultAccountBalance(source).blockingFirst().getBalance();
        BigInteger minterBalance = accountService.getDefaultAccountBalance(minter).blockingFirst().getBalance();
        DataStoreFactory.getScenarioDataStore().put("source_balance", sourceBalance);
        DataStoreFactory.getScenarioDataStore().put("minter_balance", minterBalance);

        createContract(gas, source, target, contractName);
    }

    @Step("Contract <contractName> had exception with message <expectedMessage>")
    public void checkContractForException(String contractName, String expectedMessage) {
        CreationResult result = mustHaveValue(DataStoreFactory.getScenarioDataStore(), contractName, CreationResult.class);
        assertThat(result.getResult()).isEqualTo(CreationResult.CreationResultTypes.exception);
        assertThat(result.getErrorMessage()).matches(".*" + expectedMessage + ".*");
    }


    @Step("Contract <contractName> creation succeeded")
    public void checkContractResult(String contractName) {
        CreationResult result = mustHaveValue(DataStoreFactory.getScenarioDataStore(), contractName, CreationResult.class);
        assertThat(result.getResult()).isEqualTo(CreationResult.CreationResultTypes.success);
    }

    @Step("No transactions are pending on node for <contractName>")
    public void noTransactionsPending(String contractName) {
        CreationResult result = mustHaveValue(DataStoreFactory.getScenarioDataStore(), contractName, CreationResult.class);

        List<Transaction> transactions= utilService.getPendingTransactions(result.getNode());
        assertThat(transactions).isEmpty();
    }

    @Step("Contract <contractName> is not pending")
    public void checkContractNotPending(String contractName) {
        CreationResult result = mustHaveValue(DataStoreFactory.getScenarioDataStore(), contractName, CreationResult.class);

        String transactionHash = result.contract.getTransactionReceipt().orElseThrow(() -> new RuntimeException("no transaction receipt for contract")).getTransactionHash();

        List<Transaction> transactions= utilService.getPendingTransactions(result.getNode());
        transactions.forEach(t -> logger.info("=== GOT HASH: {}", t.getHash()));
        transactions.forEach(t -> assertThat(t.getHash()).isNotEqualTo(transactionHash));
    }

    @Step("On source node, the default account's balance is now less than its previous balance")
    public void checkDefaultAccountBalanceDecreasedOnSender() {
        BigInteger prevBalance = mustHaveValue(DataStoreFactory.getScenarioDataStore(), "source_balance", BigInteger.class);

        QuorumNode source = mustHaveValue(DataStoreFactory.getScenarioDataStore(), "source_node", QuorumNode.class);
        BigInteger currentBalance = accountService.getDefaultAccountBalance(source).blockingFirst().getBalance();

        assertThat(currentBalance).isLessThan(prevBalance);
    }

    @Step("On minter, the default account's balance is now greater than its previous balance")
    public void checkDefaultAccountBalanceIncreasedOnMinter() {
        BigInteger prevBalance = mustHaveValue(DataStoreFactory.getScenarioDataStore(), "minter_balance", BigInteger.class);

        QuorumNode minter = raftService.getLeader(QuorumNode.Node1);
        BigInteger currentBalance = accountService.getDefaultAccountBalance(minter).blockingFirst().getBalance();

        assertThat(currentBalance).isGreaterThan(prevBalance);
    }

    /**
     * Create contract, storing result in Scenario Data Store:
     */
    private void createContract(int gas, QuorumNode source, QuorumNode target, String contractName) {
        try {
            Contract contract = contractService.createSimpleContract(42, source, target, BigInteger.valueOf(gas)).blockingFirst();
            DataStoreFactory.getScenarioDataStore().put(contractName,
                    new CreationResult(CreationResult.CreationResultTypes.success, contract, source, ""));
        } catch (RuntimeException e) {
            DataStoreFactory.getScenarioDataStore().put(contractName,
                    new CreationResult(CreationResult.CreationResultTypes.exception, null, source, e.getMessage()));
        }
    }

    private static class CreationResult {
        enum CreationResultTypes {success, exception}

        private final CreationResultTypes result;
        private final Contract contract;
        private final QuorumNode node;
        private final String errorMessage;

        public CreationResult(CreationResultTypes result, Contract contract, QuorumNode node, String errorMessage) {
            this.result = result;
            this.contract = contract;
            this.node = node;
            this.errorMessage = errorMessage;
        }

        public CreationResultTypes getResult() {
            return result;
        }

        public QuorumNode getNode() {
            return node;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
