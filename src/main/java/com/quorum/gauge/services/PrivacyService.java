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
import com.quorum.gauge.common.Wallet;
import org.springframework.stereotype.Service;

@Service
public class PrivacyService extends AbstractService {
    public String id(QuorumNode node) {
        return getQuorumNodeConfig(node).getPrivacyAddress();
    }

    public String thirdPartyUrl(QuorumNode node) {
        return getQuorumNodeConfig(node).getThirdPartyUrl();
    }

    private QuorumNetworkProperty.Node getQuorumNodeConfig(QuorumNode node){
        QuorumNetworkProperty.Node nodeConfig = networkProperty().getNodes().get(node);
        if (nodeConfig == null) {
            throw new IllegalArgumentException("Node " + node + " not found in config");
        }
        return nodeConfig;
    }

    public QuorumNetworkProperty.WalletData walletData(Wallet wallet){
        QuorumNetworkProperty.WalletData walletData = networkProperty().getWallets().get(wallet);
        if (walletData == null) {
            throw new IllegalArgumentException("Wallet " + wallet + " not found in config");
        }
        return walletData;
    }
}
