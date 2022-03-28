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
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigInteger;
import java.time.Duration;
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
        return nodeResources.stream().filter(id -> isComponent(component, id)).findFirst().orElseThrow(() -> new RuntimeException("Unable to find rquested component"));
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

    @Step("Restart <component> in <node>")
    public void restartComponentInNode(String component, String node) {
        String containerId = getComponentContainerId(component, node);
        logger.debug("Restarting component {} in node {} - container ID {}", component, node, containerId);
        assertThat(infraService.restartResource(containerId).blockingFirst()).isTrue();
    }

    @Step("Stop <component> in <node> if consensus is istanbul")
    public void stopComponentInNodeIfIstanbul(String component, String node) {
        if (!"istanbul".equalsIgnoreCase(networkProperty.getConsensus())) {
            logger.debug("Consensus algorithm is not istanbul. Skipping step.");
            return;
        }
        stopComponentInNode(component, node);
    }

    @Step("Run gethInit in <node> with genesis file having privacyEnhancementsBlock set to <blockNumberKey> + <decrement>")
    public void runGethInitOnNodeWithGenesisJson(String node, String blockNumberKey, String decrement) {
        BigInteger recordedBlockNumber = mustHaveValue(DataStoreFactory.getScenarioDataStore(), blockNumberKey, BigInteger.class);
        String quorumId = getComponentContainerId("quorum", node);
        BigInteger privacyEnhancementsBlock = recordedBlockNumber.add(new BigInteger(decrement));
        logger.debug("Recorded block height  {}", recordedBlockNumber);
        logger.debug("Changing genesis file privacyEnhancementsBlock to {}", privacyEnhancementsBlock);
        infraService.modifyFile(quorumId, "/data/qdata/legacy-genesis.json", new GenesisConfigOverride(Map.of("privacyEnhancementsBlock", privacyEnhancementsBlock))).blockingFirst();
        infraService.modifyFile(quorumId, "/data/qdata/latest-genesis.json", new GenesisConfigOverride(Map.of("privacyEnhancementsBlock", privacyEnhancementsBlock))).blockingFirst();
        infraService.startResource(quorumId).blockingFirst();
    }

    @Step("Enable privacy enhancements in tessera <node>")
    public void enablePrivacyEnhancementsInTesseraNode(String node) {
        String tesseraId = getComponentContainerId("tessera", node);
        logger.debug("Changing tessera config to include feature 'enablePrivacyEnhancements' : 'true'");
        infraService.modifyFile(tesseraId, "/data/tm/config.json", new TesseraFeaturesOverride(Map.of("enablePrivacyEnhancements", "true"))).blockingFirst();
        infraService.restartResource(tesseraId).blockingFirst();
    }

    @Step("Grep <component> in <node> for <message>")
    public void grepComponent(String component, String node, String message) {
        String containerId = getComponentContainerId(component, node);
        assertThat(infraService.grepLog(containerId, message, 30, TimeUnit.SECONDS).blockingFirst()).isTrue();
    }

    @Step("Wait for tessera to discover <keyCount> keys in <node>")
    public void waitForTesseraToDiscoverKeysInNodes(int keyCount, String nodes) {
        String[] split = nodes.split(",");
        for (String node : split) {
            waitForTesseraToDiscoverKeysInNode(keyCount, QuorumNode.valueOf(node));
        }
    }

    public void waitForTesseraToDiscoverKeysInNode(int keyCount, QuorumNode node) {
        final String tessera3rdPartyUrl = privacyService.thirdPartyUrl(node);
        Request request = new Request.Builder().url(tessera3rdPartyUrl + "/partyinfo/keys").build();
        int count = 30;
        do {
            try {
                Response response = okHttpClient.newCall(request).execute();
                String responseBody = response.body().string();
                if (responseBody.split("\"key\"").length > keyCount) {
                    return;
                }
            } catch (IOException e) {
                logger.debug("Error while trying to retrieve tessera keys.", e);
            }
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                throw new RuntimeException("Sleep interrupted during waitForTesseraToDiscoverKeysInNode", e);
            }
            count--;
        } while (count > 0);

        assertThat(count).isNotZero();
    }

    @Step("Check that <component> in <node> is <state>")
    public void nodeStateIs(String component, String node, String state) {
        String containerId = getComponentContainerId(component, node);
        String containerState = infraService.wait(containerId).blockingFirst() ? "up" : "down";
        assertThat(containerState).isEqualTo(state);
    }

    @Step("Wait for <component> state in <node> to be <state>")
    public void waitForNodeState(String component, String node, String state) {
        String containerId = getComponentContainerId(component, node);
        String containerState = infraService.wait(containerId).blockingFirst() ? "up" : "down";
        int count = 20;
        while (!containerState.equals(state) && count > 0) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                throw new RuntimeException("Sleep interrupted during waitForNodeState", e);
            }
            count--;
            containerState = infraService.wait(containerId).blockingFirst() ? "up" : "down";
        }
        assertThat(containerState).isEqualTo(state);
    }

    @Step("Stop and start <node> using quorum version <qVersionKey> and tessera version <tVersionKey>")
    public void stopAndStartNodes(String node, String qVersionKey, String tVersionKey) {
        final BigInteger beforeRestartBlockHeight = getCurrentBlockNumberOrDefault(node);

        NetworkResources existingNetworkResources = mustHaveValue(DataStoreFactory.getScenarioDataStore(), "networkResources", NetworkResources.class);
        infraService.deleteResources(existingNetworkResources.get(node)).blockingSubscribe();
        existingNetworkResources.remove(node);

        infraService.startNode(InfrastructureService.NodeAttributes.forNode(node).withQuorumVersionKey(qVersionKey).withTesseraVersionKey(tVersionKey), resourceId -> existingNetworkResources.add(node, resourceId)).doOnNext(ok -> {
            assertThat(ok).as(node + " must be restarted successfully").isTrue();
        }).blockingSubscribe();

        utilService.waitForNodesToReach(beforeRestartBlockHeight, networkProperty.getNode(node));

    }

    private BigInteger getCurrentBlockNumberOrDefault(final String node) {
        try {
            return utilService.getCurrentBlockNumberFrom(networkProperty.getNode(node)).blockingFirst().getBlockNumber();
        } catch (Exception ignored) {
            return networkProperty.getConsensusBlockHeight();
        }
    }

    @Step("Clear quorum data in <node>")
    public void clearDataForNode(String node) {
        String containerId = getComponentContainerId("quorum", node);
        infraService.writeFile(containerId, "/data/qdata/cleanStorage", "true").blockingFirst();
    }

    @Step("Clear tessera data in <node>")
    public void clearTesseraDataForNode(String node) {
        String containerId = getComponentContainerId("tessera", node);
        infraService.writeFile(containerId, "/data/tm/cleanStorage", "true").blockingFirst();
    }

    @Step("Change raft leader")
    public void changeRaftLeader() {
        if (!"raft".equalsIgnoreCase(networkProperty.getConsensus())) {
            logger.debug("Consensus algorithm is not raft. Skipping step.");
            return;
        }
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
                throw new RuntimeException("Sleep interrupted during changeRaftLeader", e);
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

    class TesseraFeaturesOverride implements InfrastructureService.FileContentModifier {

        final Map<String, Object> configOverrides;

        TesseraFeaturesOverride(Map<String, Object> configOverrides) {
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
                Map<String, Object> config = (Map<String, Object>) data.get("features");
                config.putAll(this.configOverrides);
                return m.writeValueAsString(data);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("unable to modify the content", e);
            }
        }
    }


    @Step("Enable multiple private states in tessera <node>")
    public void enableMPSInTesseraNode(String node) throws InterruptedException {
        String tesseraId = getComponentContainerId("tessera", node);
        logger.debug("Changing tessera config to include feature 'enableMultiplePrivateStates' : 'true'");
        infraService.modifyFile(tesseraId, "/data/tm/config.json", new TesseraFeaturesOverride(Map.of("enableMultiplePrivateStates", "true"))).blockingFirst();
        infraService.restartResource(tesseraId).blockingFirst();
    }

    @Step("Run mpsdbupgrade on <node>")
    public void runGethMPSDBUpgradeOnNode(String node) {
        String quorumId = getComponentContainerId("quorum", node);
        infraService.writeFile(quorumId, "/data/qdata/executempsdbupgrade", "true").blockingFirst();
        infraService.modifyFile(quorumId, "/data/qdata/legacy-genesis.json", new GenesisConfigOverride(Map.of("isMPS", true))).blockingFirst();
        infraService.modifyFile(quorumId, "/data/qdata/latest-genesis.json", new GenesisConfigOverride(Map.of("isMPS", true))).blockingFirst();
        infraService.startResource(quorumId).blockingFirst();
    }
}
