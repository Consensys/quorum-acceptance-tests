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
import com.quorum.gauge.common.QuorumNetworkProperty;
import com.quorum.gauge.core.AbstractSpecImplementation;
import com.quorum.gauge.services.InfrastructureService;
import com.quorum.gauge.services.RaftService;
import com.thoughtworks.gauge.Step;
import com.thoughtworks.gauge.datastore.DataStoreFactory;
import io.reactivex.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.Response;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Service
public class MixedDNSCompatibility extends AbstractSpecImplementation {
    private static final Logger logger = LoggerFactory.getLogger(BlockSynchronization.class);

    @Autowired
    private InfrastructureService infraService;

    @Autowired
    private RaftService raftService;

    @Step("Start a Quorum Network, named it <id>, consisting of <nodes> with <raftdnsenable> using raft consensus")
    public void startNetwork(String id, List<QuorumNetworkProperty.Node> nodes, String raftdnsenable) {
        GethArgBuilder additionalGethArgs = GethArgBuilder.newBuilder().raftdnsenable("raftdnsenable".equalsIgnoreCase(raftdnsenable));
        InfrastructureService.NetworkResources networkResources = new InfrastructureService.NetworkResources();
        try {
            Observable.fromIterable(nodes)
                .flatMap(n -> infraService.startNode(
                    InfrastructureService.NodeAttributes.forNode(n.getName()).withAdditionalGethArgs(additionalGethArgs),
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
    }

    @Step("Add new raft peer <newNode> with <raftdnsenable> to network <id>")
    public void addNewNode(QuorumNetworkProperty.Node newNode, String raftdnsenable, String id) {
        GethArgBuilder additionalGethArgs = GethArgBuilder.newBuilder().raftdnsenable("raftdnsenable".equalsIgnoreCase(raftdnsenable));
        InfrastructureService.NetworkResources networkResources = mustHaveValue(DataStoreFactory.getScenarioDataStore(), "networkResources", InfrastructureService.NetworkResources.class);
        raftService.addPeer(networkResources.aNodeName(), newNode.getEnodeUrl(), NodeType.peer)
            .doOnNext(res -> {
                Response.Error err = Optional.ofNullable(res.getError()).orElse(new Response.Error());
                assertThat(err.getMessage()).as("raft.addPeer must succeed").isBlank();
            })
            .map(Response::getResult)
            .flatMap(raftId -> infraService.startNode(
                InfrastructureService.NodeAttributes.forNode(newNode.getName())
                    .withAdditionalGethArgs(additionalGethArgs.raftjoinexisting(raftId)),
                resourceId -> networkResources.add(newNode.getName(), resourceId)
            ))
            .blockingSubscribe();
    }

    @Step("Propose new node from <node> with hostname expect error be <hasError>")
    public void raftAddPeer(String node, boolean hasError) {
        String arbitraryHostName = "arbitraryHostName";
        String arbitraryEnodeURL = "enode://239c1f044a2b03b6c4713109af036b775c5418fe4ca63b04b1ce00124af00ddab7cc088fc46020cdc783b6207efe624551be4c06a994993d8d70f684688fb7cf@"
            + arbitraryHostName
            + ":6666?discport=0&raftport=50401";
        raftService.addPeer(node, arbitraryEnodeURL, NodeType.peer)
            .doOnNext(res -> {
                Response.Error err = Optional.ofNullable(res.getError()).orElse(new Response.Error());
                if (hasError) {
                    assertThat(err.getMessage()).as("raft.addPeer must fail").isNotBlank();
                } else {
                    assertThat(err.getMessage()).as("raft.addPeer must not fail").isBlank();
                }
            })
            .blockingSubscribe();
        DataStoreFactory.getScenarioDataStore().put("hostname", arbitraryHostName);
    }

    @Step("Restart all nodes in the network <id>")
    public void restartAllNodes(String id) {
        InfrastructureService.NetworkResources networkResources = mustHaveValue(DataStoreFactory.getScenarioDataStore(), "networkResources", InfrastructureService.NetworkResources.class);
        // stop all nodes
        Observable.fromIterable(networkResources.allResourceIds())
            .flatMap(infraService::stopResource)
            .blockingSubscribe();

        // start tessera nodes first
        Observable.fromIterable(networkResources.allResourceIds())
            .filter(resId -> !infraService.isGeth(resId).blockingFirst())
            .flatMap(infraService::startResource)
            .blockingSubscribe();
        // start quorum nodes
        Observable.fromIterable(networkResources.allResourceIds())
            .filter(resId -> infraService.isGeth(resId).blockingFirst())
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

    @Step("Verify <node> has raft peer <raftId> with valid hostname in raft cluster")
    public void verifyNode(String node, String raftId) {
        assertThat(raftService.getCluster(node).blockingFirst().getCluster().stream()
            .filter( n -> n.get("raftId").equals(raftId))
            .findFirst()
            .orElseThrow()
            .get("hostname")
            .equals(mustHaveValue(DataStoreFactory.getScenarioDataStore(), "hostname", String.class)))
            .isEqualTo(true);
    }

}
