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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quorum.gauge.common.QuorumNetworkProperty;
import com.quorum.gauge.common.QuorumNode;
import io.reactivex.Single;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Optional;

@Service
public class GraphQLService extends AbstractService {
    private static final Logger logger = LoggerFactory.getLogger(GraphQLService.class);

    @Autowired
    OkHttpClient httpClient;

    public Single<Integer> getBlockNumber(QuorumNode node) {
        String query = "{ \"query\": \"{ block { number } }\" }";
        return executeGraphQL(node, query)
            .map( jsonObject -> Integer.decode(jsonObject.get("data").get("block").get("number").asText()));
    }

    public Single<Boolean> getIsPrivate(QuorumNode node, String hash) {
        String query = "{ \"query\": \"{ transaction(hash: \\\"" + hash + "\\\") { isPrivate } }\" }";
        return executeGraphQL(node, query)
            .map( jsonObject -> Boolean.parseBoolean(jsonObject.get("data").get("transaction").get("isPrivate").asText()));
    }

    public Single<Optional<Boolean>> getInternalIsPrivate(QuorumNode node, String hash) {
        String query = "{ \"query\": \"{ transaction(hash: \\\"" + hash + "\\\") { privateTransaction{ isPrivate } } }\" }";
        return executeGraphQL(node, query)
            .map(jsonObject -> Optional.ofNullable(jsonObject.get("data"))
                .map(o -> o.get("transaction"))
                .map(o -> o.get("privateTransaction"))
                .map(o -> o.get("isPrivate"))
                .map(JsonNode::asText)
                .map(Boolean::parseBoolean));
    }

    public Single<String> getPrivatePayload(QuorumNode node, String hash) {
        String query = "{ \"query\": \"{ transaction(hash: \\\"" + hash + "\\\") { privateInputData } }\" }";
        return executeGraphQL(node, query)
            .map( jsonObject -> (jsonObject.get("data").get("transaction").get("privateInputData").asText()));
    }

    public Single<Optional<String>> getInternalPrivatePayload(QuorumNode node, String hash) {
        String query = "{ \"query\": \"{ transaction(hash: \\\"" + hash + "\\\") { privateTransaction { privateInputData } } }\" }";
        return executeGraphQL(node, query)
            .map(jsonObject -> Optional.ofNullable(jsonObject.get("data"))
                .map(o -> o.get("transaction"))
                .map(o -> o.get("privateTransaction"))
                .map(o -> o.get("privateInputData"))
                .map(JsonNode::asText));
    }

    private String graphqlUrl(QuorumNode node) {
        QuorumNetworkProperty.Node nodeConfig = networkProperty().getNodes().get(node.name());
        if (nodeConfig == null) {
            throw new IllegalArgumentException("Node " + node + " not found in config");
        }
        return nodeConfig.getGraphqlUrl();
    }

    private Single<JsonNode> executeGraphQL(QuorumNode node, String query) {
        return Single.create( subscriber -> {
            RequestBody body = RequestBody.create(
                MediaType.parse("application/json"), query);
            Request request = new Request.Builder()
                .url(graphqlUrl(node))
                .post(body)
                .build();
            Call call = httpClient.newCall(request);
            try {
                Response response = call.execute();
                if (response.isSuccessful()) {
                    InputStream responseBody = response.body().byteStream();
                    subscriber.onSuccess(new ObjectMapper().readTree(responseBody));
                } else {
                    String error = response.body().string();
                    subscriber.onError(new Exception(error));
                }
            } catch (Exception e) {
                subscriber.onError(e);
            }
        });
    }
}
