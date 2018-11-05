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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quorum.gauge.common.QuorumNetworkConfiguration;
import com.quorum.gauge.common.QuorumNetworkProperty;
import com.quorum.gauge.common.QuorumNode;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This is to interact with qctl generator in order to provision a managed Quorum Network.
 * <p>
 * It also holds references to all the Quorum Network Operator endpoints
 */
@Service
public class QuorumBootService {
    private static final Logger logger = LoggerFactory.getLogger(QuorumBootService.class);
    @Autowired
    QuorumNetworkProperty networkProperty;

    @Autowired
    OkHttpClient httpClient;

    @Autowired
    IstanbulService istanbulService;

    /**
     * @param quorumNetworkConfiguration this is the YAML file following the quorum-tools format
     * @return network operator endpoint
     */
    public Observable<QuorumNetwork> createQuorumNetwork(QuorumNetworkConfiguration quorumNetworkConfiguration) {
        String conf = quorumNetworkConfiguration.toYAML();
        logger.debug("Using network configuration: {}", conf);
        if (StringUtils.isEmpty(networkProperty.getBootEndpoint())) {
            throw new UnsupportedOperationException("Quorum Generator endpoint is not configured");
        }
        return Observable.defer(() -> {
            Request request = new Request.Builder()
                    .url(networkProperty.getBootEndpoint() + "/v1/networks")
                    .post(RequestBody.create(MediaType.parse("text/plain"), conf))
                    .build();
            try {
                Response response = httpClient.newCall(request).execute();
                CreateQuorumNetworkResponse networkResponse = new ObjectMapper().readValue(response.body().byteStream(), CreateQuorumNetworkResponse.class);
                return Observable.just(networkResponse.operatorAddress);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).flatMap(operatorAddress -> {
            Request request = new Request.Builder()
                    .url(operatorAddress + "/v1/nodes")
                    .get().build();
            try {
                Response response = httpClient.newCall(request).execute();
                Map<QuorumNode, QuorumNetworkProperty.Node> newNodes = new ObjectMapper()
                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                        .readValue(response.body().byteStream(), new TypeReference<Map<QuorumNode, QuorumNetworkProperty.Node>>() {
                        });
                QuorumNetworkProperty newNetworkProperty = new QuorumNetworkProperty();
                newNetworkProperty.setNodes(newNodes);
                QuorumNodeConnectionFactory newFactory = new QuorumNodeConnectionFactory();
                newFactory.okHttpClient = httpClient;
                newFactory.networkProperty = newNetworkProperty;
                QuorumNetwork qn = new QuorumNetwork();
                qn.connectionFactory = newFactory;
                qn.operatorAddress = operatorAddress;
                qn.config = quorumNetworkConfiguration;
                return Observable.just(qn);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Add a new node to an existing network then join using the current consensus algorithm
     *
     * @param qn
     * @param gethArgs
     * @return
     */
    public synchronized Observable<QuorumNode> addNode(QuorumNetwork qn, String... gethArgs) {
        QuorumNetworkConfiguration.QuorumNodeConfiguration newNodeConfig =
                QuorumNetworkConfiguration.QuorumNodeConfiguration.New()
                        .quorum(QuorumNetworkConfiguration.GenericQuorumNodeConfiguration.New()
                                .image(QuorumNetworkConfiguration.DEFAULT_QUORUM_DOCKER_IMAGE)
                                .config("verbosity", "5")
                        ).txManager(QuorumNetworkConfiguration.GenericQuorumNodeConfiguration.New()
                        .image(QuorumNetworkConfiguration.DEFAULT_TX_MANAGER_DOCKER_IMAGE)
                );
        for (int i = 0; i < gethArgs.length; i += 2) {
            newNodeConfig.quorum.config(gethArgs[i], gethArgs[i + 1]);
        }
        return Observable.defer(() -> {
            // just create a node
            try {
                Request addNodeRequest = new Request.Builder()
                        .url(qn.connectionFactory.networkProperty.getBootEndpoint() + "/v1/nodes")
                        .put(RequestBody.create(MediaType.parse("application/json"), new ObjectMapper().writeValueAsString(newNodeConfig)))
                        .build();
                Response response = httpClient.newCall(addNodeRequest).execute();
                Map<QuorumNode, QuorumNetworkProperty.Node> newNodes = new ObjectMapper()
                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                        .readValue(response.body().byteStream(), new TypeReference<Map<QuorumNode, QuorumNetworkProperty.Node>>() {
                        });
                return Observable.from(newNodes.entrySet()).first();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).flatMap(newNode -> {
            // using the current consensus to join the network
            switch (qn.config.consensus.name) {
                case istanbul:
                    List<Observable<IstanbulService.IstanbulPropose>> proposals = new ArrayList<>();
                    for (QuorumNode n : qn.connectionFactory.getNetworkProperty().getNodes().keySet()) {
                        proposals.add(istanbulService.propose(n, newNode.getValue().getValidatorAddress()).subscribeOn(Schedulers.io()));
                    }
                    return Observable.zip(proposals, args -> null).flatMap(xx -> Observable.just(newNode));
                case raft:
                    break;
                default:
                    throw new RuntimeException("consensus type not implemented");
            }
            return Observable.just(newNode);
        }).map(newNode -> {
            qn.config.addNodes(newNodeConfig);
            return newNode.getKey();
        });

    }

    public static class CreateQuorumNetworkResponse {
        @JsonProperty("operator-address")
        public String operatorAddress;
    }

    public static class QuorumNetwork {
        public QuorumNodeConnectionFactory connectionFactory;
        public String operatorAddress;
        public QuorumNetworkConfiguration config;
    }
}
