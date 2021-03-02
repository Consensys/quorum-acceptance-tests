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
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jService;
import org.web3j.protocol.http.HttpService;
import org.web3j.quorum.Quorum;

@Service
public class QuorumNodeConnectionFactory {
    @Autowired
    QuorumNetworkProperty networkProperty;

    @Autowired
    OkHttpClient okHttpClient;

    public Quorum getConnection(QuorumNode node) {
        return Quorum.build(getWeb3jService(node));
    }

    public Quorum getConnection(QuorumNetworkProperty.Node node) {
        return Quorum.build(getWeb3jService(node));
    }

    public Web3j getWeb3jConnection(QuorumNode node) {
        return Web3j.build(getWeb3jService(node));
    }

    public Web3j getWeb3jConnection(QuorumNetworkProperty.Node node) {
        return Web3j.build(getWeb3jService(node));
    }

    public Web3jService getWeb3jService(QuorumNode node) {
        QuorumNetworkProperty.Node nodeConfig = networkProperty.getNodes().get(node.name());
        if (nodeConfig == null) {
            throw new IllegalArgumentException("Can't find node " + node + " in the configuration");
        }
        return getWeb3jService(nodeConfig);
    }

    public Web3jService getWeb3jService(QuorumNetworkProperty.Node node) {
        return new HttpService(node.getUrl(), okHttpClient, false);
    }

    public QuorumNetworkProperty getNetworkProperty() {
        return this.networkProperty;
    }
}
