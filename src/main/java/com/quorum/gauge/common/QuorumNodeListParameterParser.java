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

import com.thoughtworks.gauge.datastore.DataStoreFactory;
import com.thoughtworks.gauge.execution.parameters.parsers.base.CustomParameterParser;
import gauge.messages.Spec;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.quorum.gauge.common.QuorumNetworkProperty.Node;

/**
 * Comma-separated list of nodes
 */
public class QuorumNodeListParameterParser extends CustomParameterParser<List<Node>> {

    @Override
    protected List<Node> customParse(final Class<?> aClass, final Spec.Parameter parameter) {
        final String nodes = parameter.getValue();

        final QuorumNetworkProperty props
            = (QuorumNetworkProperty) DataStoreFactory.getSuiteDataStore().get("networkProperties");

        return Arrays.stream(nodes.split(","))
                .map(String::trim)
                .map(nodeName -> {
                    final Node node = props.getNodes().get(nodeName);
                    if(node == null) {
                        throw new IllegalArgumentException("Node " + nodeName + " not found in network properties");
                    }
                    node.setName(nodeName);
                    return node;
                }).collect(Collectors.toList());
    }

    @Override
    public boolean canParse(final Class<?> aClass, final Spec.Parameter parameter) {
        return aClass.equals(List.class);
    }
}
