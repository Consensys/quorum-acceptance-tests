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

import com.quorum.gauge.common.GethArgBuilder;
import com.quorum.gauge.common.NodeType;
import com.quorum.gauge.common.QuorumNetworkProperty.Node;
import com.quorum.gauge.core.AbstractSpecImplementation;
import com.quorum.gauge.services.InfrastructureService;
import com.quorum.gauge.services.InfrastructureService.NetworkResources;
import com.quorum.gauge.services.InfrastructureService.NodeAttributes;
import com.quorum.gauge.services.RaftService;
import com.thoughtworks.gauge.Step;
import com.thoughtworks.gauge.datastore.DataStoreFactory;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.response.EthBlockNumber;
import org.web3j.tx.Contract;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@Service
public class BlockSynchronization extends AbstractSpecImplementation {
    private static final Logger logger = LoggerFactory.getLogger(BlockSynchronization.class);

    @Autowired
    private InfrastructureService infraService;

    @Autowired
    private RaftService raftService;

    @Step("Start a <networkType> Quorum Network, named it <id>, consisting of <nodes> with <gcmode> `gcmode` using <consensus> consensus")
    public void startNetwork(String networkType, String id, List<Node> nodes, String gcmode, String consensus) {
        GethArgBuilder additionalGethArgs = GethArgBuilder.newBuilder()
                .permissioned("permissioned".equalsIgnoreCase(networkType))
                .gcmode(gcmode);
        NetworkResources networkResources = new NetworkResources();
        try {
            Observable.fromIterable(nodes)
                    .flatMap(n -> infraService.startNode(
                            NodeAttributes.forNode(n.getName()).withAdditionalGethArgs(additionalGethArgs),
                            resourceId -> networkResources.add(n.getName(), resourceId)
                    ))
                    .doOnNext( ok -> {
                        assertThat(ok).as("Node must start successfully").isTrue();
                    })
                    .doOnComplete(() -> {
                        logger.debug("Waiting for network to be up completely...");
                        Thread.sleep(networkProperty.getConsensusGracePeriod().toMillis());
                    })
                    .blockingSubscribe();
        } finally {
            DataStoreFactory.getScenarioDataStore().put("networkResources", networkResources);
        }
        DataStoreFactory.getScenarioDataStore().put("nodes_" + id, nodes);
        DataStoreFactory.getScenarioDataStore().put("args_" + id, additionalGethArgs);
    }

    @Step("Send some transactions to create blocks in network <id> and capture the latest block height as <latestBlockHeightName>")
    public void sendSomeTransactions(String id, String latestBlockHeightName) {
        List<Node> nodes = (List<Node>) mustHaveValue(DataStoreFactory.getScenarioDataStore(), "nodes_" + id, List.class);
        sendSomeTransactionFromNodes(nodes, latestBlockHeightName, 5, 20);
    }

    @Step("Send some transactions to create blocks in network <id> from <nodes> and capture the latest block height as <latestBlockHeightName>")
    public void sendSomeTransactionsFromNodes(String id, List<Node> nodes, String latestBlockHeightName) {
        sendSomeTransactionFromNodes(nodes, latestBlockHeightName, 1, 4);
    }

    public void sendSomeTransactionFromNodes(List<Node> nodes, String latestBlockHeightName, int threadsPerNode, int txCount) {
        int txCountPerNode = (int) Math.round(Math.ceil((double) txCount / nodes.size()));
        if (txCountPerNode < threadsPerNode) {
            threadsPerNode = txCountPerNode;
        }
        List<Observable<?>> parallelSender = new ArrayList<>();
        // fire 5 public and 5 private txs
        for (Node n : nodes) {
            parallelSender.add(sendTxs(n, txCountPerNode, threadsPerNode, null)
                .subscribeOn(Schedulers.io()));
            parallelSender.add(sendTxs(n, txCountPerNode, threadsPerNode, randomNode(nodes, n))
                .subscribeOn(Schedulers.io()));
        }
        Observable.zip(parallelSender, oks -> true)
            .doOnComplete(() -> {
                BigInteger currentBlockNumber = utilService.getCurrentBlockNumber().blockingFirst().getBlockNumber();
                logger.debug("Current block number = {}", currentBlockNumber);
                DataStoreFactory.getScenarioDataStore().put(latestBlockHeightName, currentBlockNumber);
            })
            .blockingSubscribe();
    }


    private Observable<? extends Contract> sendTxs(Node n, int txCountPerNode, int threadsPerNode,  Node target) {
        return Observable.range(0, txCountPerNode)
                .doOnNext(c -> logger.debug("Sending tx {} to {}", c, n))
                .flatMap(v -> Observable.just(v)
                                .flatMap(num -> contractService.createSimpleContract(40, n, target))
                                .subscribeOn(Schedulers.io())
                        , threadsPerNode);
    }

    private Node randomNode(List<Node> nodes, Node n) {
        if (nodes.size() == 1) {
            return nodes.get(0);
        }
        List<Node> nodesLessN = nodes.stream().filter(s -> !n.getName().equalsIgnoreCase(s.getName())).collect(Collectors.toList());
        Random rand = new Random();
        return nodesLessN.get(rand.nextInt(nodesLessN.size()));
    }

    @Step("Add new node with <gcmode> `gcmode`, named it <newNode>, and join the network <id> as <nodeType>")
    public void addNewNode(String gcmode, Node newNode, String id, String ndType) {
        GethArgBuilder additionalGethArgs = mustHaveValue(DataStoreFactory.getScenarioDataStore(), "args_" + id, GethArgBuilder.class);
        NetworkResources networkResources = mustHaveValue(DataStoreFactory.getScenarioDataStore(), "networkResources", NetworkResources.class);
        NodeType nodeType = NodeType.valueOf(ndType);
        switch (networkProperty.getConsensus()) {
            case "raft":
                raftService.addPeer(networkResources.aNodeName(), newNode.getEnodeUrl(), nodeType)
                    .doOnNext(res -> {
                        Response.Error err = Optional.ofNullable(res.getError()).orElse(new Response.Error());
                        assertThat(err.getMessage()).as("raft.add" + nodeType.name() + " must succeed").isBlank();
                        if (nodeType == NodeType.learner)
                            DataStoreFactory.getScenarioDataStore().put(newNode.getName() + "_raftId", res.getResult());
                    })
                    .map(Response::getResult)
                    .flatMap(raftId -> infraService.startNode(
                        NodeAttributes.forNode(newNode.getName())
                            .withAdditionalGethArgs(additionalGethArgs.raftjoinexisting(raftId).gcmode(gcmode)),
                        resourceId -> networkResources.add(newNode.getName(), resourceId)
                    ))
                    .blockingSubscribe();
                break;
            case "istanbul":
                // this node is non-validator node, just start it up
                infraService.startNode(
                    NodeAttributes.forNode(newNode.getName())
                        .withAdditionalGethArgs(additionalGethArgs.gcmode(gcmode)),
                    resourceId -> networkResources.add(newNode.getName(), resourceId))
                    .blockingSubscribe();
                break;
            default:
                throw new UnsupportedOperationException(networkProperty.getConsensus() + " not supported yet");
        }
        // update static-nodes.json and permissioned-nodes.json from all the nodes
        // including the new node we just started
        Observable.fromIterable(networkResources.allResourceIds())
            .filter(containerId -> infraService.isGeth(containerId).blockingFirst())
            .doOnNext(gethContainerId -> logger.debug("Modifying files in container {}", StringUtils.substring(gethContainerId, 0, 12)))
            .flatMap(gethContainerId -> infraService.modifyFile(gethContainerId,
                "/data/qdata/static-nodes.json",
                InfrastructureService.JSONListModifier.with(newNode.getEnodeUrl())))
            .flatMap(gethContainerId -> infraService.modifyFile(gethContainerId,
                "/data/qdata/permissioned-nodes.json",
                InfrastructureService.JSONListModifier.with(newNode.getEnodeUrl())))
            .doOnComplete(() -> {
                Duration duration = networkProperty.getConsensusGracePeriod();
                logger.debug("Waiting {}s while network reflects new modification ...", duration.getSeconds());
                Thread.sleep(duration.toMillis());
            })
            .blockingSubscribe();
    }


    @Step("Verify node <node> has the block height greater or equal to <latestBlockHeightName>")
    public void verifyBlockHeight(Node node, String latestBlockHeightName) {
        BigInteger lastBlockHeight = mustHaveValue(DataStoreFactory.getScenarioDataStore(), latestBlockHeightName, BigInteger.class);
        BigInteger currentBlockHeight = utilService.getCurrentBlockNumberFrom(node).blockingFirst().getBlockNumber();

        assertThat(currentBlockHeight).isGreaterThanOrEqualTo(lastBlockHeight);
    }

    @Step("Record the current block number, named it as <name>")
    public void recordCurrentBlockNumber(String name) {
        BigInteger currentBlockNumber = utilService.getCurrentBlockNumber().blockingFirst().getBlockNumber();
        logger.debug("Current block number = {}", currentBlockNumber);
        DataStoreFactory.getScenarioDataStore().put(name, currentBlockNumber);
    }

    @Step("Verify node <node> has the block height greater or equal to <latestBlockHeightName>")
    public void verifyBlockHeightGreaterThanOrEqualTo(Node node, String latestBlockHeightName) {
        verifyBlockHeight(node, latestBlockHeightName, ">=");
    }

    @Step("Verify node <node> has the block height less than or equal to <latestBlockHeightName>")
    public void verifyBlockHeightLessThanOrEqualTo(Node node, String latestBlockHeightName) {
        verifyBlockHeight(node, latestBlockHeightName, "<=");
    }

    public void verifyBlockHeight(Node node, String latestBlockHeightName, String comparison) {
        BigInteger lastBlockHeight = mustHaveValue(DataStoreFactory.getScenarioDataStore(), latestBlockHeightName, BigInteger.class);
        BigInteger currentBlockHeight = utilService.getCurrentBlockNumberFrom(node).blockingFirst().getBlockNumber();
        switch (comparison) {
            case ">=":
                assertThat(currentBlockHeight).isGreaterThanOrEqualTo(lastBlockHeight);
                break;
            case "<=":
                assertThat(currentBlockHeight).isLessThanOrEqualTo(lastBlockHeight);
                break;
        }
    }

    @Step("Record the current block number, named it as <name>")
    public void recordCurrentBlockNumber(String name) {
        BigInteger currentBlockNumber = utilService.getCurrentBlockNumber().blockingFirst().getBlockNumber();
        logger.debug("Current block number = {}", currentBlockNumber);
        DataStoreFactory.getScenarioDataStore().put(name, currentBlockNumber);
    }


    @Step("Stop all nodes in the network <id>")
    public void stopAllNodes(String id) {
        NetworkResources networkResources = mustHaveValue(DataStoreFactory.getScenarioDataStore(), "networkResources", NetworkResources.class);
        Observable.fromIterable(networkResources.allResourceIds())
                .flatMap(infraService::stopResource)
                .blockingSubscribe();
    }


    @Step("Start all nodes in the network <id>")
    public void startAllNodes(String id) {
        NetworkResources networkResources = mustHaveValue(DataStoreFactory.getScenarioDataStore(), "networkResources", NetworkResources.class);
        // start all nodes
        Observable.fromIterable(networkResources.allResourceIds())
                .flatMap(infraService::startResource)
                .blockingSubscribe();
        // wait for them to be healthy
        Observable.fromIterable(networkResources.allResourceIds())
                .flatMap(infraService::wait)
                .doOnNext(ok -> {
                    assertThat(ok).as("Node must be up").isTrue();
                })
                .doOnComplete(() -> {
                    Duration duration = networkProperty.getConsensusGracePeriod();
                    logger.debug("Waiting {}s for network to be up completely...", duration.toSeconds());
                    Thread.sleep(duration.toMillis());
                })
                .blockingSubscribe();
    }

    @Step("Verify block heights in all nodes are greater or equals to <blockHeightName> in the network <id>")
    public void verifyBlockHeightsInAllNodes(String blockHeightName, String id) {
        BigInteger expectedBlockNumber = mustHaveValue(DataStoreFactory.getScenarioDataStore(), blockHeightName, BigInteger.class);
        NetworkResources networkResources = mustHaveValue(DataStoreFactory.getScenarioDataStore(), "networkResources", NetworkResources.class);
        Observable.fromIterable(networkResources.getNodeNames())
                .doOnNext(n -> {
                    EthBlockNumber b = utilService.getCurrentBlockNumberFrom(networkProperty.getNode(n)).blockingFirst();
                    assertThat(b.getBlockNumber()).as("Block number from node " + n).isGreaterThanOrEqualTo(expectedBlockNumber);
                })
                .blockingSubscribe();
    }

    @Step("<nodeName> is able to seal new blocks")
    public void verifyBlockSealingViaLogs(Node nodeName) {
        String expectedLogMsg = "Successfully sealed new block";
        NetworkResources networkResources = mustHaveValue(DataStoreFactory.getScenarioDataStore(), "networkResources", NetworkResources.class);

        assertThat(networkResources.get(nodeName.getName())).as(nodeName.getName() + " started").isNotNull();
        // sometimes istanbul/raft take long time to sync
        Duration longerDuration = Duration.ofSeconds(networkProperty.getConsensusGracePeriod().getSeconds() * 2);
        logger.debug("Grepping '{}' in the log stream for {}s ...", expectedLogMsg, longerDuration.getSeconds());
        Boolean found = Observable.fromIterable(networkResources.get(nodeName.getName()))
                .filter(id -> infraService.isGeth(id).blockingFirst())
                .flatMap(id -> infraService.grepLog(id, expectedLogMsg, longerDuration.getSeconds(), TimeUnit.SECONDS))
                .blockingFirst();

        assertThat(found).as(expectedLogMsg).isTrue();
    }

    @Step("Promote learner node <newNode> from node <fromNode>, in the network <networkName>")
    public void promoteLearnerNode(Node newNode, Node fromNode, String id) {
        Integer raftId = mustHaveValue(DataStoreFactory.getScenarioDataStore(), newNode.getName() + "_raftId", Integer.class);
        switch (networkProperty.getConsensus()) {
            case "raft":
                raftService.promoteToPeer(fromNode.getName(), raftId)
                    .doOnNext(res -> {
                        Response.Error err = Optional.ofNullable(res.getError()).orElse(new Response.Error());
                        assertThat(err.getMessage()).as("raft.promoteToPeer must succeed").isBlank();
                        assertThat(res.getResult()).isTrue();
                    }).blockingSubscribe();
                break;
            default:
                throw new UnsupportedOperationException(networkProperty.getConsensus() + " not supported yet");
        }
    }

    @Step("Stop node <nodeName> in the network <id>")
    public void stopNode(Node nodeName, String id) {
        NetworkResources networkResources = mustHaveValue(DataStoreFactory.getScenarioDataStore(), "networkResources", NetworkResources.class);
        String resourceId = networkResources.getResourceId(nodeName.getName());
        assertThat(resourceId).isNotNull();
        infraService.stopResource(resourceId).doOnNext(ok -> {
            assertThat(ok).as("Node must be stopped").isTrue();
        }).doOnComplete(() -> {
            Duration duration = Duration.ofSeconds(10);
            logger.debug("Waiting {}s for node to be up completely...", duration.toSeconds());
            Thread.sleep(duration.toMillis());
        }).blockingSubscribe();
        logger.debug("Docker ps - After stopping node {} resource id {}", nodeName, resourceId);
        dockerPs();
        logger.debug("-----------------------------------");
    }

    public void dockerPs(){
        try {
            Process process = Runtime.getRuntime().exec(
                "docker ps --filter name=template-raft-node");
            StringBuilder output = new StringBuilder();

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line + "\n");
            }

            int exitVal = process.waitFor();
            if (exitVal == 0) {
                logger.debug("Success!");
                logger.debug(output.toString());
            }
        }catch(Exception ex){}
    }

    @Step("Start node <nodeName> in the network <id>")
    public void startNode(Node nodeName, String id) {
        NetworkResources networkResources = mustHaveValue(DataStoreFactory.getScenarioDataStore(), "networkResources", NetworkResources.class);
        String resourceId = networkResources.getResourceId(nodeName.getName());
        assertThat(resourceId).isNotNull();
        infraService.startResource(resourceId).doOnNext(ok -> {
            assertThat(ok).as("Node must be up").isTrue();
        }).doOnComplete(() -> {
            Duration duration = Duration.ofSeconds(10);
            logger.debug("Waiting {}s for node to be up completely...", duration.toSeconds());
            Thread.sleep(duration.toMillis());
        }).blockingSubscribe();
    }

    @Step("Save block height <latestBlockHeightName> as <newBlockHeightName>")
    public void saveBlockHeightAs(String latestBlockHeightName, String newBlockHeightName) {
        BigInteger lastBlockHeight = mustHaveValue(DataStoreFactory.getScenarioDataStore(), latestBlockHeightName, BigInteger.class);
        DataStoreFactory.getScenarioDataStore().put(newBlockHeightName, lastBlockHeight);
    }

    @Step("Verify block height <bhName1> is greater than <bhName2>")
    public void compareBlockHeightsGreaterThan(String bhName1, String bhName2) {
        BigInteger lastBlockHeight1 = mustHaveValue(DataStoreFactory.getScenarioDataStore(), bhName1, BigInteger.class);
        BigInteger lastBlockHeight2 = mustHaveValue(DataStoreFactory.getScenarioDataStore(), bhName2, BigInteger.class);
        assertThat(lastBlockHeight1).isGreaterThan(lastBlockHeight2);
    }

    @Step("Verify block height <bhName1> is equal to <bhName2>")
    public void compareBlockHeightsEqualTo(String bhName1, String bhName2) {
        BigInteger lastBlockHeight1 = mustHaveValue(DataStoreFactory.getScenarioDataStore(), bhName1, BigInteger.class);
        BigInteger lastBlockHeight2 = mustHaveValue(DataStoreFactory.getScenarioDataStore(), bhName2, BigInteger.class);
        assertThat(lastBlockHeight1).isEqualTo(lastBlockHeight2);
    }

}
