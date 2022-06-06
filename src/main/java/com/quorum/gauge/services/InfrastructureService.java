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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quorum.gauge.common.GethArgBuilder;
import io.reactivex.Observable;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Interact with Quorum Network infrastructure
 */
public interface InfrastructureService {
    int STATUS_RUNNING = 1;
    int STATUS_HEALTHY = STATUS_RUNNING << 1;

    Observable<Boolean> startNode(NodeAttributes attributes, ResourceCreationCallback callback);

    Observable<Boolean> deleteResources(List<String> resourceIds);

    Observable<Boolean> deleteNetwork(NetworkResources networkResources);

    Observable<Integer> checkNetwork(NetworkResources existingNetworkResources);

    /**
     * @param resourceId
     * @param filePath
     * @param modifier
     * @return resourceId if successful. Throws runtime exception otherwise
     */
    Observable<String> modifyFile(String resourceId, String filePath, FileContentModifier modifier);

    Observable<String> writeFile(String resourceId, String filePath, String fileContent);

    Observable<Boolean> isGeth(String resourceId);

    Observable<String> getName(String resourceId);

    Observable<Boolean> isBesu(String resourceId);

    Observable<Boolean> deleteDatadirs(NetworkResources networkResources);

    Observable<Boolean> stopResource(String resourceId);

    Observable<Boolean> startResource(String resourceId);

    Observable<Boolean> restartResource(String resourceId);

    Observable<Boolean> wait(String resourceId);

    Observable<Boolean> grepLog(String resourceId, String grepStr, long timeoutAmount, TimeUnit timeoutUnit);

    interface ResourceCreationCallback {
        void onCreate(String resourceId);
    }
    interface FileContentModifier {
        String modify(String content);
    }
    class NodeAttributes {
        private String node;
        private boolean startFresh;
        private String quorumVersionKey;
        private String tesseraVersionKey;
        private GethArgBuilder additionalGethArgsBuilder;

        private NodeAttributes(String node) { this.node = node; this.startFresh = false;this.additionalGethArgsBuilder = GethArgBuilder.newBuilder(); }
        public static NodeAttributes forNode(String node) {
            return new NodeAttributes(node);
        }
        public NodeAttributes withFreshStart() {
            this.startFresh = true;
            return this;
        }
        public NodeAttributes withQuorumVersionKey(String versionKey) {
            this.quorumVersionKey = versionKey;
            return this;
        }
        public NodeAttributes withTesseraVersionKey(String versionKey) {
            this.tesseraVersionKey = versionKey;
            return this;
        }
        public NodeAttributes withAdditionalGethArgs(GethArgBuilder builder) {
            this.additionalGethArgsBuilder = builder;
            return this;
        }

        public String getNode() {
            return node;
        }

        public boolean isStartFresh() {
            return startFresh;
        }

        public String getQuorumVersionKey() {
            return quorumVersionKey;
        }

        public String getTesseraVersionKey() {
            return tesseraVersionKey;
        }

        public String getAdditionalGethArgs() { return additionalGethArgsBuilder.toString(); }

        public GethArgBuilder getAdditionalGethArgsBuilder() { return additionalGethArgsBuilder; }
    }
    class NetworkResources extends ConcurrentHashMap<String, Vector<String>> {
        public synchronized void add(String nodeName, String resourceId) {
            Vector<String> resources = super.get(nodeName);
            if (CollectionUtils.isEmpty(resources)) {
                resources = new Vector<>();
                super.put(nodeName, resources);
            }
            resources.add(resourceId);
        }
        public Set<String> getNodeNames() {
            return super.keySet();
        }
        public String aNodeName() {
            return this.getNodeNames().stream().findFirst().orElseThrow(() -> new RuntimeException("no nodes available"));
        }
        public List<String> allResourceIds() {
            return super.values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        }

        public Vector<String> getResourceId(String nodeName) {
            if (containsKey(nodeName)) {
                return get(nodeName);
            }
            return new Vector<>();
        }
    }
    class JSONListModifier<T> implements FileContentModifier {
        private T additionalElement;

        private JSONListModifier(T additionalElement) {
            this.additionalElement = additionalElement;
        }

        public static <T> JSONListModifier with(T additionalElement) {
            return new JSONListModifier(additionalElement);
        }

        @Override
        public String modify(String content) {
            ObjectMapper m = new ObjectMapper();
            try {
                List<T> data = m.readValue(content, new TypeReference<>() {});
                data.add(additionalElement);
                return m.writeValueAsString(data);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("unable to modify the content", e);
            }
        }
    }

}
