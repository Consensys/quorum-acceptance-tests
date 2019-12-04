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

package com.quorum.gauge.common;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.*;

public class QuorumNetworkConfiguration {

    public static final String DEFAULT_QUORUM_DOCKER_IMAGE = "vsmk98/geth-upgrade-1.8.12";
    public static final String DEFAULT_TX_MANAGER_DOCKER_IMAGE = "quorumengineering/tessera:latest";

    public String name;
    public QuorumNetworkGenesis genesis;
    public QuorumNetworkConsensus consensus;
    public List<QuorumNodeConfiguration> nodes = new ArrayList<>();

    private QuorumNetworkConfiguration() {

    }

    public static QuorumNetworkConfiguration New() {
        return new QuorumNetworkConfiguration();
    }

    public QuorumNetworkConfiguration name(String n) {
        this.name = n;
        return this;
    }

    public QuorumNetworkConfiguration genesis(QuorumNetworkGenesis g) {
        this.genesis = g;
        return this;
    }

    public QuorumNetworkConfiguration consensus(QuorumNetworkConsensus c) {
        this.consensus = c;
        return this;
    }

    public QuorumNetworkConfiguration addNodes(QuorumNodeConfiguration... n) {
        this.nodes.addAll(Arrays.asList(n));
        return this;
    }

    public String toYAML() {
        StringBuilder buf = new StringBuilder();
        buf.append("# This is auto generated configuration").append((System.getProperty("line.separator")));
        buf.append("name: ").append(this.name).append(System.getProperty("line.separator"));
        buf.append("consensus: ").append(System.getProperty("line.separator"));
        buf.append("  name: ").append(this.consensus.name).append(System.getProperty("line.separator"));
        buf.append("  config: ").append(System.getProperty("line.separator"));
        for (Map.Entry<String, String> entry : this.consensus.config.entrySet()) {
            buf.append("    ").append(entry.getKey()).append(": ").append(entry.getValue()).append(System.getProperty("line.separator"));
        }
        buf.append("nodes:").append(System.getProperty("line.separator"));
        for (QuorumNodeConfiguration node : this.nodes) {
            buf.append("- quorum:").append(System.getProperty("line.separator"));
            buf.append("    image: ").append(node.quorum.image).append(System.getProperty("line.separator"));
            buf.append("    config: ").append(System.getProperty("line.separator"));
            for (Map.Entry<String, String> entry : node.quorum.config.entrySet()) {
                buf.append("      ").append(entry.getKey()).append(": ").append(entry.getValue()).append(System.getProperty("line.separator"));
            }
            buf.append("  tx_manager:").append(System.getProperty("line.separator"));
            buf.append("    image: ").append(node.txManager.image).append(System.getProperty("line.separator"));
            buf.append("    config: ").append(System.getProperty("line.separator"));
            for (Map.Entry<String, String> entry : node.txManager.config.entrySet()) {
                buf.append("      ").append(entry.getKey()).append(": ").append(entry.getValue()).append(System.getProperty("line.separator"));
            }
        }
        return buf.toString();
    }

    public static class QuorumNodeConfiguration {
        public GenericQuorumNodeConfiguration quorum;
        @JsonProperty("tx_manager")
        public GenericQuorumNodeConfiguration txManager;

        private QuorumNodeConfiguration() {

        }

        public static QuorumNodeConfiguration New() {
            return new QuorumNodeConfiguration();
        }

        public QuorumNodeConfiguration quorum(GenericQuorumNodeConfiguration q) {
            this.quorum = q;
            return this;
        }

        public QuorumNodeConfiguration txManager(GenericQuorumNodeConfiguration g) {
            this.txManager = g;
            return this;
        }
    }

    public static class GenericQuorumNodeConfiguration {
        public String image;
        public Map<String, String> config = new HashMap<>();

        private GenericQuorumNodeConfiguration() {

        }

        public static GenericQuorumNodeConfiguration New() {
            return new GenericQuorumNodeConfiguration();
        }

        public GenericQuorumNodeConfiguration image(String img) {
            this.image = img;
            return this;
        }

        public GenericQuorumNodeConfiguration config(String key, String value) {
            this.config.put(key, value);
            return this;
        }
    }

    public static class QuorumNetworkGenesis {

    }

    public static class QuorumNetworkConsensus {
        public enum ConsensusType {
            raft, istanbul
        }

        public ConsensusType name;
        public Map<String, String> config = new HashMap<>();

        private QuorumNetworkConsensus() {

        }

        public static QuorumNetworkConsensus New() {
            return new QuorumNetworkConsensus();
        }

        public QuorumNetworkConsensus name(ConsensusType type) {
            this.name = type;
            return this;
        }

        public QuorumNetworkConsensus config(String key, String value) {
            this.config.put(key, value);
            return this;
        }
    }
}
