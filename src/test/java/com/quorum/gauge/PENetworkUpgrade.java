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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quorum.gauge.common.QuorumNode;
import com.quorum.gauge.core.AbstractSpecImplementation;
import com.quorum.gauge.services.InfrastructureService;
import com.quorum.gauge.services.InfrastructureService.NetworkResources;
import com.quorum.gauge.services.RaftService;
import com.thoughtworks.gauge.Step;
import com.thoughtworks.gauge.datastore.DataStoreFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Service
public class PENetworkUpgrade extends AbstractSpecImplementation {
    private static final Logger logger = LoggerFactory.getLogger(PENetworkUpgrade.class);

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private InfrastructureService infraService;

    @Autowired
    private RaftService raftService;

    private String getComponentContainerId(String component, String node) {
        NetworkResources existingNetworkResources = mustHaveValue(DataStoreFactory.getScenarioDataStore(), "networkResources", NetworkResources.class);
        final Vector<String> nodeResources = existingNetworkResources.get(node);
        return nodeResources.stream()
            .filter(id -> isComponent(component, id))
            .findFirst().orElseThrow(() -> new RuntimeException("Unable to find rquested component"));
    }

    private boolean isComponent(String component, String resId) {
        boolean isGeth = infraService.isGeth(resId).blockingFirst();
        switch (component) {
            case "quorum":
                return isGeth;
            case "tessera":
                return !isGeth;
        }
        return false;
    }

    @Step("Stop <component> in <node>")
    public void stopComponentInNode(String component, String node) {
        String containerId = getComponentContainerId(component, node);
        logger.debug("Stopping component {} in node {} - container ID {}", component, node, containerId);
        assertThat(infraService.stopResource(containerId).blockingFirst()).isTrue();
    }

    @Step("Run gethInit in <node> with genesis file having privacyEnhancementsBlock set to <blockNumberKey> + <decrement>")
    public void runGethInitOnNodeWithGenesisJson(String node, String blockNumberKey, String decrement) {
        BigInteger recordedBlockNumber = mustHaveValue(DataStoreFactory.getScenarioDataStore(), blockNumberKey, BigInteger.class);
        String quorumId = getComponentContainerId("quorum", node);
        BigInteger privacyEnhancementsBlock = recordedBlockNumber.add(new BigInteger(decrement));
        logger.debug("Recorded block height  {}", recordedBlockNumber);
        logger.debug("Changing genesis file privacyEnhancementsBlock to {}", privacyEnhancementsBlock);
        infraService.modifyFile(quorumId, "/data/qdata/genesis.json",
            new GenesisConfigOverride(Map.of("privacyEnhancementsBlock", privacyEnhancementsBlock))).blockingFirst();
        infraService.startResource(quorumId).blockingFirst();
    }

    @Step("Grep <component> in <node> for <message>")
    public void grepComponent(String component, String node, String message) {
        String containerId = getComponentContainerId(component, node);
        assertThat(infraService.grepLog(containerId, message, 30, TimeUnit.SECONDS).blockingFirst()).isTrue();
    }

    @Step("Check that <component> in <node> is <state>")
    public void nodeStateIs(String component, String node, String state) {
        String containerId = getComponentContainerId(component, node);
        String containerState = infraService.wait(containerId).blockingFirst() ? "up" : "down";
        assertThat(containerState).isEqualTo(state);
    }

    @Step("Stop and start <node> using quorum version <qVersionKey> and tessera version <tVersionKey>")
    public void stopAndStartNodes(String node, String qVersionKey, String tVersionKey) {
        NetworkResources existingNetworkResources = mustHaveValue(DataStoreFactory.getScenarioDataStore(), "networkResources", NetworkResources.class);
        infraService.deleteResources(existingNetworkResources.get(node)).blockingSubscribe();
        existingNetworkResources.remove(node);

        infraService.startNode(
            InfrastructureService.NodeAttributes.forNode(node)
                .withQuorumVersionKey(qVersionKey)
                .withTesseraVersionKey(tVersionKey),
            resourceId -> existingNetworkResources.add(node, resourceId))
            .doOnNext(ok -> {
                assertThat(ok).as(node + " must be restarted successfully").isTrue();
            })
            .doOnComplete(() -> {
                logger.debug("Waiting for {} to be up completely...", node);
                Thread.sleep(networkProperty.getConsensusGracePeriod().toMillis());
            })
            .blockingSubscribe();
    }

    @Step("Clear quorum data in <node>")
    public void clearDataForNode(String node) {
        String containerId = getComponentContainerId("quorum", node);
        infraService.writeFile(containerId, "/data/qdata/cleanStorage", "true").blockingFirst();
    }

    @Step("Change raft leader")
    public void changeRaftLeader() {
        final String oldLeader = raftService.getLeaderWithLocalEnodeInfo(QuorumNode.Node1).name();
        if (oldLeader.equals(QuorumNode.Node4)) {
            // if node4 was the leader - a new one should be elected so no additional leader change is necessary
            return;
        }
        String newLeader = oldLeader;
        int retry = 5;
        do {
            String containerId = getComponentContainerId("quorum", oldLeader);
            infraService.restartResource(containerId).blockingFirst();
            try {
                retry--;
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            newLeader = raftService.getLeaderWithLocalEnodeInfo(QuorumNode.Node1).name();
        } while (newLeader == oldLeader && retry >= 0);
        logger.debug("Old leader {} New leader {}", oldLeader, newLeader);
        assertThat(newLeader).isNotEqualTo(oldLeader);
    }

    class GenesisConfigOverride implements InfrastructureService.FileContentModifier {

        final Map<String, Object> configOverrides;

        GenesisConfigOverride(Map<String, Object> configOverrides) {
            this.configOverrides = configOverrides;
        }

        @Override
        public String modify(String content) {
            if (configOverrides == null) {
                return content;
            }
            ObjectMapper m = new ObjectMapper();
            try {
                Map<String, Object> data = m.readValue(content, Map.class);
                Map<String, Object> config = (Map<String, Object>) data.get("config");
                config.putAll(configOverrides);
                return m.writeValueAsString(data);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("unable to modify the content", e);
            }
        }
    }
}
