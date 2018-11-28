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
import com.quorum.gauge.common.RetryWithDelay;
import com.quorum.gauge.core.AbstractSpecImplementation;
import com.quorum.gauge.services.QuorumBootService;
import com.thoughtworks.gauge.Step;
import com.thoughtworks.gauge.datastore.DataStoreFactory;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.web3j.tx.Contract;
import rx.Observable;
import rx.Scheduler;
import rx.functions.FuncN;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@Service
public class BlockSynchronization extends AbstractSpecImplementation {
    private static final Logger logger = LoggerFactory.getLogger(BlockSynchronization.class);

    @Step("Start a <networkType> Quorum Network, named it <id>, with <nodeCount> nodes with <gcmode> `gcmode` using <consensus> consensus")
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
        QuorumBootService.QuorumNetwork quorumNetwork = quorumBootService.createQuorumNetwork(newNetwork).toBlocking().first();

        DataStoreFactory.getScenarioDataStore().put("networkName", id);
        DataStoreFactory.getScenarioDataStore().put("network_" + id, quorumNetwork);
    }

    @Step("Send some transactions to create blocks in network <id> and capture the latest block height as <latestBlockHeightName>")
    public void sendSomeTransactions(String id, String latestBlockHeightName) {
        logger.debug("Send some transtractions to network name={}, capture blockheight to {}", id, latestBlockHeightName);
        QuorumBootService.QuorumNetwork quorumNetwork = mustHaveValue(DataStoreFactory.getScenarioDataStore(), "network_" + id, QuorumBootService.QuorumNetwork.class);
        Scheduler scheduler = networkAwaredScheduler(10);
        try {
            Context.setConnectionFactory(quorumNetwork.connectionFactory);
            int arbitraryValue = new Random().nextInt(50) + 1;
            List<Observable<? extends Contract>> allObservableContracts = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                allObservableContracts.add(contractService.createSimpleContract(arbitraryValue, QuorumNode.Node1, QuorumNode.values()[(i % 2) + 1])
                        .doAfterTerminate(() -> Context.clear())
                        .subscribeOn(scheduler));
            }
            BigInteger blockNumber = Observable.zip(allObservableContracts, args -> utilService.getCurrentBlockNumber().toBlocking().first()).toBlocking().first().getBlockNumber();
            DataStoreFactory.getScenarioDataStore().put("latestBlockHeightName", blockNumber);
        } finally {
            Context.clear();
        }
    }

    @Step("Add new node with <gcmode> `gcmode`, named it <nodeName>, and join the network <id>")
    public void addNewNode(String gcmode, QuorumNode nodeName, String id) {
        logger.debug("Add new node name={}, gcmode={}, network={}", nodeName, gcmode, id);
        String networkName = mustHaveValue(DataStoreFactory.getScenarioDataStore(), "networkName", String.class);

        assertThat(id).isEqualTo(networkName);

        QuorumBootService.QuorumNetwork qn = mustHaveValue(DataStoreFactory.getScenarioDataStore(), "network_" + id, QuorumBootService.QuorumNetwork.class);
        try {
            Context.setConnectionFactory(qn.connectionFactory);
            QuorumNode actualNodeName = quorumBootService.addNode(qn, "gcmode", gcmode).toBlocking().first();
            assertThat(actualNodeName).isEqualTo(nodeName);
        } finally {
            Context.clear();
        }
    }

    @Step("Verify node <nodeName> has the block height greater or equal to <latestBlockHeightName>")
    public void verifyBlockHeight(QuorumNode nodeName, String latestBlockHeightName) {
        logger.debug("Verify block height for node {}", nodeName);
        BigInteger lastBlockHeight = mustHaveValue(DataStoreFactory.getScenarioDataStore(), "latestBlockHeightName", BigInteger.class);
        String networkName = mustHaveValue(DataStoreFactory.getScenarioDataStore(), "networkName", String.class);
        QuorumBootService.QuorumNetwork qn = mustHaveValue(DataStoreFactory.getScenarioDataStore(), "network_" + networkName, QuorumBootService.QuorumNetwork.class);
        try {
            Context.setConnectionFactory(qn.connectionFactory);
            // retry to wait for block height getting synced
            BigInteger currentBlockNumber = utilService.getCurrentBlockNumberFrom(nodeName)
                    .map(ethBlockNumber -> {
                        if (ethBlockNumber.getBlockNumber().intValue() < lastBlockHeight.intValue()) {
                            throw new RuntimeException("retry");
                        }
                        return ethBlockNumber.getBlockNumber();
                    })
                    .retryWhen(new RetryWithDelay(20, 3000))
                    .toBlocking().first();

            // if timed out happens, the block height was not synced
        } finally {
            Context.clear();
        }

    }

    @Step("Stop all nodes in the network <id>")
    public void stopAllNodes(String id) {
        logger.debug("Stopping nodes in network {}", id);
        QuorumBootService.QuorumNetwork qn = mustHaveValue(DataStoreFactory.getScenarioDataStore(), "network_" + id, QuorumBootService.QuorumNetwork.class);
        Scheduler scheduler = networkAwaredScheduler(qn.config.nodes.size());
        List<Observable<Response>> parallelObservables = new ArrayList<>();
        for (Observable<Response> res : quorumBootService.stopNodes(qn)) {
            parallelObservables.add(res.subscribeOn(scheduler));
        }
        Observable.zip(parallelObservables, (FuncN<Object>) args -> {
            for (Object o : args) {
                Response res = (Response) o;
                assertThat(res.code()).as(res.message()).isEqualTo(200);
            }
            return true;
        });
    }

    @Step("Start all nodes in the network <id>")
    public void startAllNodes(String id) {
        logger.debug("Starting nodes in network {}", id);
        QuorumBootService.QuorumNetwork qn = mustHaveValue(DataStoreFactory.getScenarioDataStore(), "network_" + id, QuorumBootService.QuorumNetwork.class);
        Scheduler scheduler = networkAwaredScheduler(qn.config.nodes.size());
        List<Observable<Response>> parallelObservables = new ArrayList<>();
        for (Observable<Response> res : quorumBootService.startNodes(qn)) {
            parallelObservables.add(res.subscribeOn(scheduler));
        }
        Observable.zip(parallelObservables, (FuncN<Object>) args -> {
            for (Object o : args) {
                Response res = (Response) o;
                assertThat(res.code()).isEqualTo(200);
            }
            return true;
        });
    }

    @Step("Verify block heights in all nodes are the same in the network <id>")
    public void verifyBlockHeightsInAllNodes(String id) {
        logger.debug("Verifying block heights in all nodes for network {}", id);
        QuorumBootService.QuorumNetwork qn = mustHaveValue(DataStoreFactory.getScenarioDataStore(), "network_" + id, QuorumBootService.QuorumNetwork.class);
        Scheduler scheduler = networkAwaredScheduler(qn.config.nodes.size());
        List<Observable<BigInteger>> blockHeightObservables = new ArrayList<>();
        for (QuorumNode node : qn.connectionFactory.getNetworkProperty().getNodes().keySet()) {
            blockHeightObservables.add(utilService.getCurrentBlockNumberFrom(node).flatMap(ethBlockNumber -> Observable.just(ethBlockNumber.getBlockNumber())).subscribeOn(scheduler));
        }
        Observable.zip(blockHeightObservables, (FuncN<Object>) args -> {
            for (Object o : args) {
                BigInteger blockHeight = (BigInteger) o;
                assertThat(blockHeight).isNotEqualTo(BigInteger.ZERO);
            }
            return true;
        });
    }

    @Step("<nodeName> is able to seal new blocks")
    public void verifyBlockSealingViaLogs(QuorumNode nodeName) {
        logger.debug("Verifying block sealing from logs stream from {}", nodeName);
        String expectedLogMsg = "Successfully sealed new block";
        String networkName = mustHaveValue(DataStoreFactory.getScenarioDataStore(), "networkName", String.class);
        QuorumBootService.QuorumNetwork qn = mustHaveValue(DataStoreFactory.getScenarioDataStore(), "network_" + networkName, QuorumBootService.QuorumNetwork.class);
        WebSocket ws = null;
        try {
            Context.setConnectionFactory(qn.connectionFactory);
            Request logRequest = new Request.Builder().url(qn.operatorAddress.replaceFirst("http://", "ws://") + "/v1/nodes/" + nodeName.ordinal() + "/quorum/logs").build();
            CountDownLatch latch = new CountDownLatch(1);
            ws = okHttpClient.newWebSocket(logRequest, new WebSocketListener() {
                @Override
                public void onMessage(WebSocket webSocket, String text) {
                    if (text.contains(expectedLogMsg)) {
                        latch.countDown();
                    }
                }

                @Override
                public void onOpen(WebSocket webSocket, Response response) {
                    logger.debug("Connected to log stream API for node {}", nodeName);
                }
            });
            latch.await(30, TimeUnit.SECONDS);
            // save the current block number
            BigInteger currentBlockNumber = utilService.getCurrentBlockNumber().toBlocking().first().getBlockNumber();
            DataStoreFactory.getScenarioDataStore().put("blocknumber", currentBlockNumber);
        } catch (InterruptedException e) {
            fail("Timed out! Can't see " + expectedLogMsg + " from " + nodeName + " logs");
        } finally {
            if (ws != null) {
                ws.close(1000, "test finished");
            }
            Context.clear();
        }
    }
}
