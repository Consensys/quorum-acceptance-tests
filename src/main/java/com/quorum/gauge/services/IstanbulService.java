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

import com.quorum.gauge.common.QuorumNetworkProperty;
import com.quorum.gauge.common.QuorumNode;
import com.quorum.gauge.ext.IstanbulNodeAddress;
import com.quorum.gauge.ext.IstanbulPropose;
import com.quorum.gauge.ext.ListIstanbulNodeAddress;
import com.quorum.gauge.ext.MinerStartStop;
import io.reactivex.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.Request;

import java.util.Arrays;
import java.util.Collections;

@Service
public class IstanbulService extends AbstractService {

    private static final Logger logger = LoggerFactory.getLogger(IstanbulService.class);

    @Autowired
    private InfrastructureService infraService;

    @Autowired
    private BesuQBFTService besuService;

    public Observable<MinerStartStop> stopMining(final QuorumNode node) {
        logger.debug("Request {} to stop mining", node);

        return new Request<>(
            "miner_stop",
            null,
            connectionFactory().getWeb3jService(node),
            MinerStartStop.class
        ).flowable().toObservable();
    }

    public Observable<MinerStartStop> startMining(final QuorumNode node) {
        logger.debug("Request {} to start mining", node);

        return new Request<>(
            "miner_start",
            Collections.emptyList(),
            connectionFactory().getWeb3jService(node),
            MinerStartStop.class
        ).flowable().toObservable();
    }

    public Observable<ListIstanbulNodeAddress> getValidators(final QuorumNode n) {
        QuorumNetworkProperty.Node node = networkProperty().getNode(n.name());
        logger.debug("Request node {} to get validators", node);

        return new Request<>(
            "istanbul_getValidators",
            Arrays.asList(),
            connectionFactory().getWeb3jService(node),
            ListIstanbulNodeAddress.class
        ).flowable().toObservable();
    }

    public Observable<IstanbulPropose> propose(final QuorumNetworkProperty.Node node, final String proposedValidatorAddress, boolean vote) {
        logger.debug("Node {} proposing {}", node, proposedValidatorAddress);

        // Check if the node is a besu node, if yes call besu specific propose validator RPC call
        if(isBesuNode(node)) {
            return besuService.propose(node, proposedValidatorAddress, vote);
        }

        return new Request<>(
            "istanbul_propose",
            Arrays.asList(proposedValidatorAddress, vote),
            connectionFactory().getWeb3jService(node),
            IstanbulPropose.class
        ).flowable().toObservable();
    }

    public Observable<IstanbulNodeAddress> nodeAddress(final QuorumNetworkProperty.Node node) {
        logger.debug("node address of node {}", node);
        // Check if it is a besu node, if yes get the nodeAddress from besu
        if(isBesuNode(node)) {
            return besuService.nodeAddress(node);
        }

        return new Request<>(
            "istanbul_nodeAddress",
            Arrays.asList(),
            connectionFactory().getWeb3jService(node),
            IstanbulNodeAddress.class
        ).flowable().toObservable();
    }

    public boolean isBesuNode(QuorumNetworkProperty.Node node) {
        QuorumNetworkProperty.DockerInfrastructureProperty.DockerContainerProperty property = networkProperty().getDockerInfrastructure().getNodes().get(node.getName());
        String containerId = property.getQuorumContainerId();

        return infraService.isBesu(containerId).blockingFirst();
    }

}
