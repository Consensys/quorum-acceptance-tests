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

import com.quorum.gauge.common.Context;
import com.quorum.gauge.common.QuorumNetworkConfiguration;
import com.quorum.gauge.common.QuorumNode;
import com.quorum.gauge.core.AbstractSpecImplementation;
import com.quorum.gauge.services.QuorumNodeConnectionFactory;
import com.thoughtworks.gauge.Step;
import com.thoughtworks.gauge.datastore.DataStoreFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.web3j.tx.Contract;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class BlockSynchronization extends AbstractSpecImplementation {
    private static final Logger logger = LoggerFactory.getLogger(BlockSynchronization.class);

    @Step("Start a <networkType> Quorum Network, named it <id>, with <nodeCount> nodes with <gcmode> `gcmode`s using <consensus> consensus")
    public void startNetwork(String networkType, String id, int nodeCount, String gcmode, String consensus) {
        logger.debug("Create a network with name={}, type={}, gcmode={}, consensus={}", id, networkType, gcmode, consensus);

        QuorumNetworkConfiguration.QuorumNetworkConsensus.ConsensusType consensusType = QuorumNetworkConfiguration.QuorumNetworkConsensus.ConsensusType.valueOf(consensus);
        QuorumNetworkConfiguration.QuorumNetworkConsensus consensusConfig = QuorumNetworkConfiguration.QuorumNetworkConsensus.New()
                .name(consensusType);
        if (consensusType == QuorumNetworkConfiguration.QuorumNetworkConsensus.ConsensusType.istanbul) {
            consensusConfig.config("validators", "0,1,2")
                    .config("geth_args", "--istanbul.blockperiod=1 --emitcheckpoints --syncmode=full");
        }
        QuorumNetworkConfiguration newNetwork = QuorumNetworkConfiguration.New()
                .name(id)
                .consensus(
                        consensusConfig
                );
        for (int i = 0; i < nodeCount; i++) {
            QuorumNetworkConfiguration.GenericQuorumNodeConfiguration quorum = QuorumNetworkConfiguration.GenericQuorumNodeConfiguration.New()
                    .image(QuorumNetworkConfiguration.DEFAULT_QUORUM_DOCKER_IMAGE)
                    .config("gcmode", gcmode)
                    .config("verbosity", "5");
            if (consensusType == QuorumNetworkConfiguration.QuorumNetworkConsensus.ConsensusType.istanbul) {
                quorum.config("mine", "")
                        .config("minerthreads", "1");
            }
            QuorumNetworkConfiguration.GenericQuorumNodeConfiguration txManager = QuorumNetworkConfiguration.GenericQuorumNodeConfiguration.New()
                    .image(QuorumNetworkConfiguration.DEFAULT_TX_MANAGER_DOCKER_IMAGE);
            if ("permissioned".equalsIgnoreCase(networkType)) {
                quorum.config("permissioned", "");
            }
            newNetwork.addNodes(QuorumNetworkConfiguration.QuorumNodeConfiguration.New()
                    .quorum(quorum)
                    .txManager(txManager));
        }
        String conf = newNetwork.toYAML();
        logger.debug("Using network configuration: {}", conf);
        QuorumNodeConnectionFactory connectionFactory = quorumBootService.createQuorumNetwork(conf).toBlocking().first();

        DataStoreFactory.getScenarioDataStore().put("networkName", id);
        DataStoreFactory.getScenarioDataStore().put("connectionFactory_" + id, connectionFactory);
    }

    @Step("Send some transactions to create blocks in network <id> and capture the latest block height as <latestBlockHeightName>")
    public void sendSomeTransactions(String id, String latestBlockHeightName) {
        logger.debug("Send some transtractions to network name={}, capture blockheight to {}", id, latestBlockHeightName);
        QuorumNodeConnectionFactory connectionFactory = mustHaveValue(DataStoreFactory.getScenarioDataStore(), "connectionFactory_" + id, QuorumNodeConnectionFactory.class);
        try {
            Context.setConnectionFactory(connectionFactory);
            int arbitraryValue = new Random().nextInt(50) + 1;
            List<Observable<? extends Contract>> allObservableContracts = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                allObservableContracts.add(contractService.createSimpleContract(arbitraryValue, QuorumNode.Node1, QuorumNode.values()[(i % 2) + 1]).subscribeOn(Schedulers.io()));
            }
            BigInteger blockNumber = Observable.zip(allObservableContracts, args -> utilService.getCurrentBlockNumber().toBlocking().first()).toBlocking().first().getBlockNumber();
            DataStoreFactory.getScenarioDataStore().put("latestBlockHeightName", blockNumber);
        } finally {
            Context.clear();
        }
    }

    @Step("Add new node with <gcmode> gcmode, named it <nodeName>, and join the network <id>")
    public void addNewNode(String gcmode, QuorumNode nodeName, String id) {
        logger.debug("Add new node name={}, gcmode={}, network={}", nodeName, gcmode, id);
    }

    @Step("Verify node <nodeName> has the same block height with <latestBlockHeightName>")
    public void verifyBlockHeight(QuorumNode nodeName, String latestBlockHeightName) {
        logger.debug("Verify block height for node {}", nodeName);
    }

    @Step("Verify privacy between <source> and <target> using a simple smart contract excluding <arbitraryNode>")
    public void verifyPrivacy(QuorumNode source, QuorumNode target, QuorumNode arbitraryNode) {
        logger.debug("Verify privacy between {} and {}", source, target);
    }

    @Step("Stop all nodes in the network <id>")
    public void stopAllNodes(String id) {
        logger.debug("Stopping nodes in network {}", id);
    }

    @Step("Start all nodes in the network <id>")
    public void startAllNodes(String id) {
        logger.debug("Starting nodes in network {}", id);
    }

    @Step("Verify block heights in all nodes are the same in the network <id>")
    public void verifyBlockHeightsInAllNodes(String id) {
        logger.debug("Verifying block heights in all nodes for network {}", id);
    }

    @Step("<nodeName> is able to seal new blocks")
    public void verifyBlockSealingViaLogs(QuorumNode nodeName) {
        logger.debug("Verifying block sealing from logs stream from {}", nodeName);
    }
}
