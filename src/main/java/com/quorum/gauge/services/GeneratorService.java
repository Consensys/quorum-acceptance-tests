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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quorum.gauge.common.QuorumNetworkProperty;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import rx.Observable;

import java.io.IOException;

/**
 * This is to interact with qctl generator in order to provision a managed Quorum Network.
 * <p>
 * It also holds references to all the Quorum Network Operator endpoints
 */
@Service
public class GeneratorService {
    @Autowired
    QuorumNetworkProperty networkProperty;

    @Autowired
    OkHttpClient httpClient;

    /**
     * @param quorumNetworkConfiguration this is the YAML file following the quorum-tools format
     * @return network operator endpoint
     */
    public Observable<QuorumNodeConnectionFactory> createQuorumNetwork(String quorumNetworkConfiguration) {
        if (StringUtils.isEmpty(networkProperty.getGeneratorEndpoint())) {
            throw new UnsupportedOperationException("Quorum Generator endpoint is not configured");
        }
        return Observable.defer(() -> {
            Request request = new Request.Builder()
                    .url(networkProperty.getGeneratorEndpoint() + "/v1/networks")
                    .post(RequestBody.create(MediaType.parse("text/plain"), quorumNetworkConfiguration))
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
                QuorumNetworkProperty newNetworkProperty = new ObjectMapper().readValue(response.body().byteStream(), QuorumNetworkProperty.class);
                QuorumNodeConnectionFactory newFactory = new QuorumNodeConnectionFactory();
                newFactory.okHttpClient = httpClient;
                newFactory.networkProperty = newNetworkProperty;
                return Observable.just(newFactory);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static class CreateQuorumNetworkResponse {
        @JsonProperty("operator-address")
        public String operatorAddress;
    }
}
