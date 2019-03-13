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
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "quorum")
public class QuorumNetworkProperty {
    private Map<QuorumNode, Node> nodes = new HashMap<>();
    private Map<Wallet, WalletData> wallets = new HashMap<>();
    private SocksProxy socksProxy;
    private String bootEndpoint;

    public SocksProxy getSocksProxy() {
        return socksProxy;
    }

    public void setSocksProxy(SocksProxy socksProxy) {
        this.socksProxy = socksProxy;
    }

    public Map<QuorumNode, Node> getNodes() {
        return nodes;
    }

    public void setNodes(Map<QuorumNode, Node> nodes) {
        this.nodes = nodes;
    }

    public String getBootEndpoint() {
        return bootEndpoint;
    }

    public void setBootEndpoint(String bootEndpoint) {
        this.bootEndpoint = bootEndpoint;
    }

    public Map<Wallet, WalletData> getWallets() {
        return wallets;
    }

    public void setWallets(Map<Wallet, WalletData> wallets) {
        this.wallets = wallets;
    }

    public static class SocksProxy {
        private String host;
        private int port;

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }
    }

    public static class Node {
        @JsonProperty("privacy-address")
        private String privacyAddress;
        private String url;
        @JsonProperty("third-party-url")
        private String thirdPartyUrl;
        @JsonProperty("validator-address")
        private String validatorAddress;
        @JsonProperty("enode-address")
        private String enode;

        public String getPrivacyAddress() {
            return privacyAddress;
        }

        public void setPrivacyAddress(String privacyAddress) {
            this.privacyAddress = privacyAddress;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getValidatorAddress() {
            return validatorAddress;
        }

        public void setValidatorAddress(String validatorAddress) {
            this.validatorAddress = validatorAddress;
        }

        @Override
        public String toString() {
            final String template = "Node[url: %s, privacy-address: %s, validator-address: %s, enode: %s]";
            return String.format(template, url, privacyAddress, validatorAddress, enode);
        }

        public String getEnode() {
            return enode;
        }

        public void setEnode(String enode) {
            this.enode = enode;
        }

        public String getThirdPartyUrl() {
            return thirdPartyUrl;
        }

        public void setThirdPartyUrl(String thirdPartyUrl) {
            this.thirdPartyUrl = thirdPartyUrl;
        }
    }

    public static class WalletData {
        @JsonProperty("path")
        private String walletPath;
        @JsonProperty("pass")
        private String walletPass;

        public String getWalletPath() {
            return walletPath;
        }

        public void setWalletPath(String walletPath) {
            this.walletPath = walletPath;
        }

        public String getWalletPass() {
            return walletPass;
        }

        public void setWalletPass(String walletPass) {
            this.walletPass = walletPass;
        }
    }
}
