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

import com.quorum.gauge.common.Context;
import com.quorum.gauge.common.QuorumNetworkProperty;
import com.quorum.gauge.common.QuorumNode;
import com.quorum.gauge.core.AbstractSpecImplementation;
import com.quorum.gauge.ext.BatchRequest;
import com.quorum.gauge.ext.ObjectResponse;
import com.thoughtworks.gauge.Step;
import com.thoughtworks.gauge.Table;
import com.thoughtworks.gauge.datastore.DataStoreFactory;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.Response;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@Service
public class PluginSecurity extends AbstractSpecImplementation {

    private static final Logger logger = LoggerFactory.getLogger(PluginSecurity.class);

    @Step("Configure the authorization server to grant `<clientId>` access to scopes `<scopes>` in `<nodes>`")
    public void configure(String clientId, String scopes, String nodes) {
        List<String> nodeList = Arrays.stream(nodes.split(","))
                .map(String::trim)
                .collect(Collectors.toList());
        List<String> scopeList = Arrays.stream(scopes.split(","))
                .map(StringUtils::trim)
                .collect(Collectors.toList());
        assertThat(oAuth2Service.updateOrInsert(clientId, scopeList, nodeList).blockingFirst())
                .as("Configuring must be successful").isEqualTo(true);
    }

    @Step("`<clientId>` is responded with <policy> when trying to: <table>")
    public void invokeMultiple(String clientId, String policy, Table table) {
        boolean expectAuthorized = "success".equalsIgnoreCase(policy);
        String token = mustHaveValue(DataStoreFactory.getScenarioDataStore(), clientId, String.class);
        Context.storeAccessToken(token);
        Map<String, QuorumNetworkProperty.Node> nodeMap = networkProperty.getNodes();
        table.getTableRows().stream()
                .map(r -> new ApiCall(r.getCell("callApi"), r.getCell("targetNode")))
                .onClose(Context::removeAccessToken)
                .forEach(a -> {
                    if (a.isBatch) {
                        rpcService.call(nodeMap.get(a.node), a.collector)
                                .blockingForEach(batchResponse -> {
                                    assertThat(batchResponse.getResponses()).as("Number of returned responses").hasSize(a.collector.size());
                                    for (ObjectResponse res : batchResponse.getResponses()) {
                                        assertThat(res.getId()).as("Response must have id").isNotZero();
                                        Request r = a.collector.getByID(res.getId());
                                        String description = policy + ": " + r.getMethod() + "@" + a.node;
                                        if (expectAuthorized) {
                                            assertThat(Optional.ofNullable(res.getError()).orElse(new Response.Error()).getMessage())
                                                    .as(description).isNullOrEmpty();
                                        }
                                        assertThat(res.hasError())
                                                .as(description).isNotEqualTo(expectAuthorized);
                                        if (res.hasError()) {
                                            assertThat(res.getError().getMessage())
                                                    .as(description).endsWith(policy);
                                        }
                                    }
                                });
                    } else {
                        rpcService.call(nodeMap.get(a.node), a.name, Collections.emptyList(), ObjectResponse.class)
                                .blockingForEach(res -> {
                                    assertThat(res.getId()).as("Response must have ID").isNotZero();
                                    String description = policy + ": " + a.name + "@" + a.node;
                                    if (expectAuthorized) {
                                        assertThat(Optional.ofNullable(res.getError()).orElse(new Response.Error()).getMessage())
                                                .as(description).isNullOrEmpty();
                                    }
                                    assertThat(res.hasError())
                                            .as(description).isNotEqualTo(expectAuthorized);
                                    if (res.hasError()) {
                                        assertThat(res.getError().getMessage())
                                                .as(description).endsWith(policy);
                                    }
                                });
                    }
                });
    }

    @Step("`<clientId>` doesn't request access token from the authorization server")
    public void noAccessToken(String clientId) {
        DataStoreFactory.getScenarioDataStore().put(clientId, "");
    }

    @Step("Setup `<clientId>` access token for subsequent calls")
    public void setupAccessToken(String clientId) {
        String token = mustHaveValue(DataStoreFactory.getScenarioDataStore(), clientId, String.class);
        Context.storeAccessToken(token);
    }

    @Step("`<clientId>` sends a batch `<apis>` to `<node>` and expect: <table>")
    public void invokeMultipleWithExpectation(String clientId, String apis, QuorumNetworkProperty.Node node, Table table) {
        String token = mustHaveValue(DataStoreFactory.getScenarioDataStore(), clientId, String.class);
        Context.storeAccessToken(token);
        BatchRequest.Collector collector = BatchRequest.Collector.create();
        Arrays.stream(apis.split(",")).map(String::trim).forEach(n -> collector.add(n, Collections.emptyList()));
        Map<String, String> expect = table.getTableRows().stream()
                .collect(Collectors.toMap(r -> StringUtils.removeEnd(StringUtils.removeStart(r.getCell("callApi"), "`"), "`"), r -> r.getCell("expectation")));
        rpcService.call(node, collector).blockingForEach(batchResponse -> {
            assertThat(batchResponse.getResponses()).as("Number of returned responses").hasSize(collector.size());
            for (ObjectResponse res : batchResponse.getResponses()) {
                assertThat(res.getId()).as("Response must have id").isNotZero();
                Request r = collector.getByID(res.getId());
                String policy = Optional.ofNullable(expect.get(r.getMethod())).orElseThrow(() -> new IllegalStateException("no such method in expectation table: " + r.getMethod()));
                boolean expectAuthorized = "success".equalsIgnoreCase(policy);
                String description = policy + ": " + r.getMethod() + "@" + node.getName();
                if (expectAuthorized) {
                    assertThat(Optional.ofNullable(res.getError()).orElse(new Response.Error()).getMessage())
                            .as(description).isNullOrEmpty();
                }
                assertThat(res.hasError())
                        .as(description).isNotEqualTo(expectAuthorized);
                if (res.hasError()) {
                    assertThat(res.getError().getMessage())
                            .as(description).endsWith(policy);
                }
            }
        });
    }

    @Step("`<clientId>` requests access token for scope(s) `<scopes>` and audience(s) `<nodes>` from the authorization server")
    public void requestAccessToken(String clientId, String scopes, String nodes) {
        oAuth2Service.requestAccessToken(
            clientId,
            Arrays.stream(nodes.split(",")).map(this::cleanData).collect(Collectors.toList()),
            Arrays.stream(scopes.split(",")).map(this::cleanData).collect(Collectors.toList())
        )
            .doOnTerminate(Context::removeAccessToken)
            .blockingForEach(t -> {
                DataStoreFactory.getScenarioDataStore().put(clientId, t);
            });
    }

    @Step("`<clientId>` is responded with <policy> when trying to access graphql on <targetNode>")
    public void invokeGraphql(String clientId, String policy, QuorumNode targetNode) {
        boolean expectAuthorized = "success".equalsIgnoreCase(policy);
        String token = mustHaveValue(DataStoreFactory.getScenarioDataStore(), clientId, String.class);
        Context.storeAccessToken(token);
        String description = policy + ": graphql_getBlockNumber @" + targetNode.name();
        graphQLService.getBlockNumber(targetNode)
            .subscribe(
                result -> {
                    assertThat(expectAuthorized).as(description).isTrue();},
                e -> {
                    assertThat(expectAuthorized).as(description).isFalse();
                    assertThat(e.getMessage())
                    .as(description).contains(policy);
                if (expectAuthorized) {
                    assertThat(Optional.ofNullable(e).orElse(new Throwable()).getMessage())
                        .as(description).isNullOrEmpty();
                }}
            );
    }

    private String cleanData(String d) {
        return Stream.of(d).map(StringUtils::trim)
                .map(s -> StringUtils.removeStart(s, "`"))
                .map(s -> StringUtils.removeEnd(s, "`"))
                .map(StringUtils::trim).collect(Collectors.joining());
    }

    private static class ApiCall {
        public String name;
        public String node;
        public boolean isBatch;
        public BatchRequest.Collector collector;

        public ApiCall(String name, String node) {
            List<String> v = Stream.of(name, node)
                    .map(StringUtils::trim)
                    .map(s -> StringUtils.removeStart(s, "`"))
                    .map(s -> StringUtils.removeEnd(s, "`"))
                    .map(StringUtils::trim)
                    .collect(Collectors.toList());
            this.name = v.get(0);
            this.node = v.get(1);
            List<String> names = Arrays.stream(this.name.split(",")).map(StringUtils::trim).collect(Collectors.toList());
            this.isBatch = names.size() > 1;
            if (this.isBatch) {
                this.collector = BatchRequest.Collector.create();
                for (String n : names) {
                    collector.add(n, Collections.emptyList());
                }
            }
        }
    }
}
