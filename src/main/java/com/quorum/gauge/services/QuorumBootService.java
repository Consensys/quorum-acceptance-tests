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
import com.quorum.gauge.ext.IstanbulPropose;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.io.IOException;
import java.util.*;

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

    @Autowired
    RaftService raftService;

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
                newNetworkProperty.setBootEndpoint(networkProperty.getBootEndpoint());
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
        QuorumNetworkConfiguration.GenericQuorumNodeConfiguration quorumConfig = QuorumNetworkConfiguration.GenericQuorumNodeConfiguration.New()
                .image(QuorumNetworkConfiguration.DEFAULT_QUORUM_DOCKER_IMAGE)
                .config("verbosity", "5");
        QuorumNetworkConfiguration.QuorumNodeConfiguration newNodeConfig =
                QuorumNetworkConfiguration.QuorumNodeConfiguration.New()
                        .quorum(quorumConfig)
                        .txManager(QuorumNetworkConfiguration.GenericQuorumNodeConfiguration.New()
                                .image(QuorumNetworkConfiguration.DEFAULT_TX_MANAGER_DOCKER_IMAGE)
                        );
        for (int i = 0; i < gethArgs.length; i += 2) {
            newNodeConfig.quorum.config(gethArgs[i], gethArgs[i + 1]);
        }
        return Observable.defer(() -> {
            // just create a node
            try {
                Request addNodeRequest = new Request.Builder()
                        .url(qn.operatorAddress + "/v1/nodes")
                        .put(RequestBody.create(MediaType.parse("application/json"), new ObjectMapper().writeValueAsString(Arrays.asList(newNodeConfig))))
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
                    List<Observable<IstanbulPropose>> proposals = new ArrayList<>();
                    for (QuorumNode n : qn.connectionFactory.getNetworkProperty().getNodes().keySet()) {
                        proposals.add(istanbulService.propose(n, newNode.getValue().getValidatorAddress()).subscribeOn(Schedulers.io()));
                    }
                    return Observable.zip(proposals, args -> null).flatMap(xx -> Observable.just(newNode));
                case raft:
                    List<Observable<RaftService.RaftAddPeer>> peers = new ArrayList<>();
                    for (QuorumNode n : qn.connectionFactory.getNetworkProperty().getNodes().keySet()) {
                        peers.add(raftService.addPeer(n, newNode.getValue().getEnode()));
                    }
                    return Observable.from(peers).flatMap(raftAddPeerObservable -> raftAddPeerObservable.flatMap(raftAddPeer -> Observable.defer(() -> {
                        try {
                            Map<String, Object> body = new HashMap<>();
                            body.put("target", "quorum");
                            body.put("action", "fn_setRaftId");
                            body.put("fnArgs", Arrays.asList(String.valueOf(raftAddPeer.getResult())));
                            Request writeRaftIdRequest = new Request.Builder()
                                    .url(qn.operatorAddress + "/v1/nodes/" + newNode.getKey().ordinal())
                                    .post(RequestBody.create(MediaType.parse("application/json"), new ObjectMapper().writeValueAsString(body)))
                                    .build();
                            return Observable.just(httpClient.newCall(writeRaftIdRequest).execute());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }))).flatMap(response -> {
                        if (response.isSuccessful()) {
                            return Observable.just(newNode);
                        } else {
                            return Observable.error(new RuntimeException(response.message()));
                        }
                    }).take(1);
                default:
                    throw new RuntimeException("consensus type not implemented");
            }
        }).map(newNode -> {
            qn.config.addNodes(newNodeConfig);
            qn.connectionFactory.getNetworkProperty().getNodes().put(newNode.getKey(), newNode.getValue());
            return newNode.getKey();
        });

    }

    public List<Observable<Response>> stopNodes(QuorumNetwork qn) {
        return performActionOnNetworkNodes(qn, "stop");
    }

    public List<Observable<Response>> startNodes(QuorumNetwork qn) {
        return performActionOnNetworkNodes(qn, "start");
    }


    protected List<Observable<Response>> performActionOnNetworkNodes(QuorumNetwork qn, String action) {
        List<Observable<Response>> allObservables = new ArrayList<>();
        try {
            for (QuorumNode node : qn.connectionFactory.getNetworkProperty().getNodes().keySet()) {
                Observable<Response> stopQuorumResponseObsevable = Observable.defer(() -> {
                    try {
                        Map<String, String> body = new HashMap<>();
                        body.put("target", "quorum");
                        body.put("action", action);
                        Request stopQuorumRequest = new Request.Builder()
                                .url(qn.operatorAddress + "/v1/nodes/" + node.ordinal())
                                .post(RequestBody.create(MediaType.parse("application/json"), new ObjectMapper().writeValueAsString(body)))
                                .build();
                        return Observable.just(httpClient.newCall(stopQuorumRequest).execute());
                    } catch (IOException e) {
                        return Observable.error(e);
                    }
                });
                Observable<Response> stopTxManagerResponseObsevable = Observable.defer(() -> {
                    try {
                        Map<String, String> body = new HashMap<>();
                        body.put("target", "tx_manager");
                        body.put("action", action);
                        Request stopQuorumRequest = new Request.Builder()
                                .url(qn.operatorAddress + "/v1/nodes/" + node.ordinal())
                                .post(RequestBody.create(MediaType.parse("application/json"), new ObjectMapper().writeValueAsString(body)))
                                .build();
                        return Observable.just(httpClient.newCall(stopQuorumRequest).execute());
                    } catch (IOException e) {
                        return Observable.error(e);
                    }
                });
                allObservables.add(stopQuorumResponseObsevable);
                allObservables.add(stopTxManagerResponseObsevable);
            }
            return allObservables;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
