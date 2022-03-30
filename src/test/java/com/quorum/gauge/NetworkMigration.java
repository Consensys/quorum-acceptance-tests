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

import com.quorum.gauge.common.QuorumNetworkProperty;
import com.quorum.gauge.common.QuorumNode;
import com.quorum.gauge.core.AbstractSpecImplementation;
import com.quorum.gauge.services.ContractService;
import com.quorum.gauge.services.InfrastructureService;
import com.quorum.gauge.services.InfrastructureService.NetworkResources;
import com.thoughtworks.gauge.Step;
import com.thoughtworks.gauge.Table;
import com.thoughtworks.gauge.datastore.DataStoreFactory;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.methods.response.EthBlockNumber;
import org.web3j.tx.Contract;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@Service
public class NetworkMigration extends AbstractSpecImplementation {
    private static final Logger logger = LoggerFactory.getLogger(NetworkMigration.class);

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private InfrastructureService infraService;

    @Autowired
    private ContractService contractService;

    @Step("Start the network with: <table>")
    public void startNetwork(Table table) {
        NetworkResources networkResources = new NetworkResources();
        try {
            Observable.fromIterable(table.getTableRows()).flatMap(r -> infraService.startNode(InfrastructureService.NodeAttributes.forNode(r.getCell("node")).withQuorumVersionKey(r.getCell("quorum")).withTesseraVersionKey(r.getCell("tessera")), resourceId -> networkResources.add(r.getCell("node"), resourceId))).doOnNext(ok -> {
                assertThat(ok).as("Node must start successfully").isTrue();
            }).blockingSubscribe();

            var nodes = table.getTableRows().stream()
                .map(r -> networkProperty.getNode(r.getCell("node")))
                    .toArray(QuorumNetworkProperty.Node[]::new);

            utilService.waitForNodesToReach(networkProperty.getConsensusBlockHeight(), nodes);

        } finally {
            DataStoreFactory.getScenarioDataStore().put("networkResources", networkResources);
        }
    }

    /**
     * When restarting the network, we destroy the currrent containers and recreate new ones with new images
     * Datadir will not be deleted as it lives in the volume
     *
     * @param table
     */
    @Step("Restart the network with: <table>")
    public void restartNetwork(Table table) {
        NetworkResources existingNetworkResources = mustHaveValue(DataStoreFactory.getScenarioDataStore(), "networkResources", NetworkResources.class);
        infraService.deleteNetwork(existingNetworkResources).blockingSubscribe();
        NetworkResources networkResources = new NetworkResources();
        try {
            Observable.fromIterable(table.getTableRows()).flatMap(r -> infraService.startNode(InfrastructureService.NodeAttributes.forNode(r.getCell("node")).withQuorumVersionKey(r.getCell("quorum")).withTesseraVersionKey(r.getCell("tessera")), resourceId -> networkResources.add(r.getCell("node"), resourceId))).doOnNext(ok -> {
                assertThat(ok).as("Node must be restarted successfully").isTrue();
            }).doOnComplete(() -> {
                logger.debug("Waiting for network to be up completely...");
                Thread.sleep(networkProperty.getConsensusGracePeriod().toMillis());
            }).blockingSubscribe();
        } finally {
            DataStoreFactory.getScenarioDataStore().put("networkResources", networkResources);
        }
    }

    @Step("Stop and start <component> in <node> using <versionKey>")
    public void stopAndStartNodes(String component, String node, String versionKey) {
        final BigInteger beforeRestartBlockHeight = getCurrentBlockNumberOrDefault(node);

        NetworkResources existingNetworkResources = mustHaveValue(DataStoreFactory.getScenarioDataStore(), "networkResources", NetworkResources.class);
        infraService.deleteResources(existingNetworkResources.get(node)).blockingSubscribe();
        existingNetworkResources.remove(node);
        try {
            infraService.startNode(InfrastructureService.NodeAttributes.forNode(node).withQuorumVersionKey(versionKey), resourceId -> existingNetworkResources.add(node, resourceId)).doOnNext(ok -> {
                assertThat(ok).as(node + " must be restarted successfully").isTrue();
            }).blockingSubscribe();

            utilService.waitForNodesToReach(beforeRestartBlockHeight, networkProperty.getNode(node));

        } finally {
            DataStoreFactory.getScenarioDataStore().put("networkResources", existingNetworkResources);
        }
    }

    private BigInteger getCurrentBlockNumberOrDefault(final String node) {
        try {
            return utilService.getCurrentBlockNumberFrom(networkProperty.getNode(node)).blockingFirst().getBlockNumber();
        } catch (Exception ignored) {
            // if the node is currently down just wait for a few blocks
            return BigInteger.TEN;
        }
    }

    @Step("Use SimpleStorage smart contract, populate network with <publicTxCount> public transactions randomly between <nodesStr>")
    public void deploySimpleStoragePublicContract(int publicTxCount, String nodesStr) {
        this.deploySimpleStoragePublicContract(publicTxCount, nodesStr, 10);
    }

    @Step("Use SimpleStorage smart contract, populate network with <publicTxCount> public transactions randomly between <nodesStr> with <number> of threads per node")
    public void deploySimpleStoragePublicContract(int publicTxCount, String nodesStr, int threadsPerNode) {
        List<String> nodes = Arrays.stream(nodesStr.split(",")).map(String::trim).collect(Collectors.toList());
        // build calls for public transactions
        // fire to each node in parallel, max <threadsPerNode>> threads for each node
        int txCountPerNode = (int) Math.round(Math.ceil((double) publicTxCount / nodes.size()));
        if (txCountPerNode < threadsPerNode) {
            threadsPerNode = txCountPerNode;
        }
        List<Observable<?>> parallelNodes = new ArrayList<>();
        for (String n : nodes) {
            parallelNodes.add(sendTxs(QuorumNode.valueOf(n), txCountPerNode, threadsPerNode).subscribeOn(Schedulers.io()));
        }
        Observable.zip(parallelNodes, oks -> true).blockingSubscribe();
    }

    private Observable<? extends Contract> sendTxs(QuorumNode n, int txCountPerNode, int threadsPerNode) {
        return Observable.range(0, txCountPerNode).doOnNext(c -> logger.debug("Sending tx {} to {}", c, n)).flatMap(v -> Observable.just(v).flatMap(num -> contractService.createSimpleContract(40, n, null)).subscribeOn(Schedulers.io()), threadsPerNode);
    }

    private QuorumNode randomNode(List<String> nodes, QuorumNode n) {
        List<String> nodesLessN = nodes.stream().filter(s -> !n.name().equalsIgnoreCase(s)).collect(Collectors.toList());
        Random rand = new Random();
        return QuorumNode.valueOf(nodesLessN.get(rand.nextInt(nodesLessN.size())));
    }

    @Step("Verify block number in <nodes> in sync with <name>")
    public void verifyBlockNumberInSync(String nodes, String name) {
        BigInteger expectedBlockNumber = mustHaveValue(DataStoreFactory.getScenarioDataStore(), name, BigInteger.class);
        Observable.fromIterable(Arrays.stream(nodes.split(",")).map(String::trim).collect(Collectors.toList())).map(networkProperty::getNode).doOnNext(n -> {
            EthBlockNumber b = utilService.getCurrentBlockNumberFrom(n).blockingFirst();
            assertThat(b.getBlockNumber()).as("Block number from node " + n).isGreaterThanOrEqualTo(expectedBlockNumber);
        }).blockingSubscribe();
    }

    @Step("Network is running")
    public void checkNetwork() {
        NetworkResources existingNetworkResources = mustHaveValue(DataStoreFactory.getScenarioDataStore(), "networkResources", NetworkResources.class);

        int status = infraService.checkNetwork(existingNetworkResources).blockingFirst();

        assertThat(status & InfrastructureService.STATUS_RUNNING).as("Network must be running").isEqualTo(InfrastructureService.STATUS_RUNNING);
        assertThat(status & InfrastructureService.STATUS_HEALTHY).as("Network must be healthy").isEqualTo(InfrastructureService.STATUS_HEALTHY);
    }

    @Step("Verify nodes <nodes> are using <consensusAlgorithm> consensus")
    public void checkAllNodesAreUsingConsensus(List<QuorumNetworkProperty.Node> nodes, String consensusAlgorithm) {
        NetworkResources networkResources = mustHaveValue(DataStoreFactory.getScenarioDataStore(), "networkResources", NetworkResources.class);

        final String grepString = getGrepString(consensusAlgorithm);
        var waitForLog = Observable.fromIterable(nodes)
            .flatMap(n -> {
                var resourceId = networkResources.getResourceId(n.getName()).stream().filter(i -> infraService.isGeth(i).blockingFirst()).findFirst().get();
                return infraService.grepLog(resourceId, grepString, 30, TimeUnit.SECONDS);
            }).blockingIterable();

        assertThat(waitForLog).allMatch(found -> found);
    }

    @NotNull
    private String getGrepString(final String consensusAlgorithm) {
        if(consensusAlgorithm.compareToIgnoreCase("ibft") == 0) {
            return "Start new ibft round";
        } else if(consensusAlgorithm.compareToIgnoreCase("qbft") == 0) {
            return "QBFT: start new round";
        } else {
            assertThat(true).isEqualTo(false).as("Do not know how to grep for " + consensusAlgorithm);
            throw new RuntimeException("Do not know how to grep for " + consensusAlgorithm);
        }
    }
}
