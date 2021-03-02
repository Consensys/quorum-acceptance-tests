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

package com.quorum.gauge.services;

import com.quorum.gauge.common.NodeType;
import com.quorum.gauge.common.QuorumNetworkProperty.Node;
import com.quorum.gauge.common.QuorumNode;
import com.quorum.gauge.ext.NodeInfo;
import com.quorum.gauge.ext.RaftCluster;
import com.quorum.gauge.ext.RaftLeader;
import io.reactivex.Observable;
import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.Response;

import java.util.Arrays;
import java.util.Map;

@Service
public class RaftService extends AbstractService {
    private static final Logger logger = LoggerFactory.getLogger(RaftService.class);

    public Observable<RaftAddPeer> addPeer(String existingNode, String enodeUrl, NodeType nodeType) {
        return addPeer(QuorumNode.valueOf(existingNode), enodeUrl, nodeType);
    }

    public Observable<RaftAddPeer> addPeer(QuorumNode node, String enode, NodeType nodeType) {
        String rpcMethod = nodeType == NodeType.peer ? "raft_addPeer" : "raft_addLearner";
        Request<?, RaftService.RaftAddPeer> request = new Request<>(
                rpcMethod,
                Arrays.asList(StringEscapeUtils.unescapeJavaScript(enode)),
                connectionFactory().getWeb3jService(node),
                RaftService.RaftAddPeer.class
        );
        return request.flowable().toObservable().map(raftAddPeer -> {
            raftAddPeer.setNode(node);
            return raftAddPeer;
        });
    }

    public Observable<RaftPromoteLearner> promoteToPeer(String frmNode, Integer learnerRaftId) {
        Request<?, RaftService.RaftPromoteLearner> request = new Request<>(
            "raft_promoteToPeer",
            Arrays.asList(learnerRaftId.intValue()),
            connectionFactory().getWeb3jService(QuorumNode.valueOf(frmNode)),
            RaftService.RaftPromoteLearner.class
        );
        return request.flowable().toObservable().map(rf -> {
            rf.setNode(QuorumNode.valueOf(frmNode));
            return rf;
        });
    }

    public Observable<RaftCluster> getCluster(String existingNode) {
        return getCluster(QuorumNode.valueOf(existingNode));
    }

    public Observable<RaftCluster> getCluster(QuorumNode node) {
        Request<String, RaftCluster> request = new Request<>(
                "raft_cluster",
                null,
                connectionFactory().getWeb3jService(node),
                RaftCluster.class);
        return request.flowable().toObservable();
    }

    /**
     * Retrieve the enode for the raft leader and compare it with the enode of
     * all the peers to convert it into a QuorumNode identity.
     */
    public QuorumNode getLeader(QuorumNode node) {
        Request<String, RaftLeader> request = new Request<>(
                "raft_leader",
                null,
                connectionFactory().getWeb3jService(node),
                RaftLeader.class);
        RaftLeader response = request.flowable().toObservable().blockingFirst();
        String leaderEnode = response.getResult();
        logger.debug("Retrieved leader enode: {}", leaderEnode);

        Map<String, Node> nodes = connectionFactory().getNetworkProperty().getNodes();
        for (Node n : nodes.values()) {
            Request<?, NodeInfo> nodeInfoRequest = new Request<>(
                    "admin_nodeInfo",
                    null,
                    connectionFactory().getWeb3jService(n),
                    NodeInfo.class
            );

            NodeInfo nodeInfo = nodeInfoRequest.flowable().toObservable().blockingFirst();
            String thisEnode = nodeInfo.getEnode();
            logger.debug("Retrieved enode info: {}", thisEnode);
            if (thisEnode.contains(leaderEnode)) {
                return connectionFactory().getNetworkProperty().getQuorumNode(n);
            }
        }

        throw new RuntimeException("Leader enode not found in peers: " + leaderEnode);
    }

    /**
     * Retrieve the enode for the raft leader and compare it with the enode of
     * all the peers to convert it into a QuorumNode identity.
     */
    public QuorumNode getLeaderWithLocalEnodeInfo(QuorumNode node) {
        Request<String, RaftLeader> request = new Request<>(
            "raft_leader",
            null,
            connectionFactory().getWeb3jService(node),
            RaftLeader.class);
        RaftLeader response = request.flowable().toObservable().blockingFirst();
        String leaderEnode = response.getResult();
        logger.debug("Retrieved leader enode: {}", leaderEnode);

        Map<String, Node> nodes = connectionFactory().getNetworkProperty().getNodes();
        for (Map.Entry<String, Node> nodeEntry : nodes.entrySet()) {
            String thisEnode = nodeEntry.getValue().getEnodeUrl();
            logger.debug("Retrieved enode info: {}", thisEnode);
            if (thisEnode.contains(leaderEnode)) {
                return connectionFactory().getNetworkProperty().getQuorumNode(nodeEntry.getValue());
            }
        }

        throw new RuntimeException("Leader enode not found in peers: " + leaderEnode);
    }

    public static class RaftAddPeer extends Response<Integer> {
        private QuorumNode node;

        // node that perform addPeer
        public QuorumNode getNode() {
            return node;
        }

        public void setNode(QuorumNode node) {
            this.node = node;
        }
    }

    public static class RaftPromoteLearner extends Response<Boolean> {
        private QuorumNode node;

        // node that perform promoteToPeer
        public QuorumNode getNode() {
            return node;
        }

        public void setNode(QuorumNode node) {
            this.node = node;
        }
    }
}
