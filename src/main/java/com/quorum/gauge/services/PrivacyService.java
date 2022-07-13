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
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PrivacyService extends AbstractService {

    private String getPrivacyAddress(QuorumNetworkProperty.Node node){
        String v = node.getPrivacyAddress();
        if (StringUtils.isEmpty(v)) {
            if (node.getPrivacyAddressAliases().isEmpty()) {
                throw new RuntimeException("no privacy address is defined for node: " + node);
            }
            v = node.getPrivacyAddressAliases().values().iterator().next();
        }

        return v;
    }

    public String id(QuorumNode node) {
        QuorumNetworkProperty.Node quorumNodeConfig = getQuorumNodeConfig(node);
        return getPrivacyAddress(quorumNodeConfig);
    }

    public String id(QuorumNetworkProperty.Node node) {
        return getPrivacyAddress(node);
    }

    public String id(QuorumNetworkProperty.Node node, String alias) {
        if (node.getPrivacyAddressAliases().containsKey(alias)) {
            return node.getPrivacyAddressAliases().get(alias);
        }
        throw new RuntimeException("private address alias not found: " + alias);
    }

    public String id(String alias) {
        List<String> matches = new ArrayList<>();
        for (QuorumNetworkProperty.Node node : networkProperty().getNodes().values()) {
            if (node.getPrivacyAddressAliases().containsKey(alias)) {
                matches.add(node.getPrivacyAddressAliases().get(alias));
            }
        }
        if (matches.size() == 0) {
            throw new RuntimeException("private address alias not found: " + alias);
        }
        if (matches.size() > 1) {
            throw new RuntimeException("there are " + matches.size() + " nodes having this privacy address alias: " + alias);
        }
        return matches.get(0);
    }

    public List<String> ids(final List<String> nodeAliases) {
        return nodeAliases.stream().map(this::id).collect(Collectors.toList());
    }

    public String id(QuorumNode node, String name) {
        // if this is a qlight client we don't want its privacy addresses, we want its server's
        if (getQuorumNodeConfig(node).getQlight().getIsClient()) {
            node = QuorumNode.valueOf(getQuorumNodeConfig(node).getQlight().getServerId());
        }
        return getQuorumNodeConfig(node).getPrivacyAddressAliases().get(name);
    }

    public String thirdPartyUrl(QuorumNode node) {
        return getQuorumNodeConfig(node).getThirdPartyUrl();
    }

    private QuorumNetworkProperty.Node getQuorumNodeConfig(QuorumNode node) {
        QuorumNetworkProperty.Node nodeConfig = networkProperty().getNodes().get(node.name());
        if (nodeConfig == null) {
            throw new IllegalArgumentException("Node " + node + " not found in config");
        }
        return nodeConfig;
    }
}
