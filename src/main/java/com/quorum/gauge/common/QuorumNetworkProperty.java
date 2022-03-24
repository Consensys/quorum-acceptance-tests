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

import com.quorum.gauge.common.config.WalletData;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Component
@ConfigurationProperties(prefix = "quorum")
public class QuorumNetworkProperty {
    private Map<String, Node> nodes = new HashMap<>();
    private Map<String, WalletData> wallets = new HashMap<>();
    private String consensus;
    private SocksProxy socksProxy;
    private HashicorpVaultServerProperty hashicorpVaultServer;
    private DockerInfrastructureProperty dockerInfrastructure = new DockerInfrastructureProperty();
    private OAuth2ServerProperty oauth2Server;

    public SocksProxy getSocksProxy() {
        return socksProxy;
    }

    public void setSocksProxy(SocksProxy socksProxy) {
        this.socksProxy = socksProxy;
    }

    public Map<String, Node> getNodes() {
        return nodes;
    }

    public void setNodes(Map<String, Node> nodes) {
        nodes.forEach( (key,val) -> val.setName(key));
        this.nodes = nodes;
    }

    public QuorumNode getQuorumNode(QuorumNetworkProperty.Node node) {
        return QuorumNode.valueOf(node.getName());
    }

    public Map<String, WalletData> getWallets() {
        return wallets;
    }

    public void setWallets(final Map<String, WalletData> wallets) {
        this.wallets = wallets;
    }

    public Node getNode(String nodeName) {
        Node node = Optional.ofNullable(nodes.get(nodeName)).orElseThrow(() -> new RuntimeException("no such node with name: " + nodeName));
        node.setName(nodeName);
        return node;
    }

    public HashicorpVaultServerProperty getHashicorpVaultServer() {
        return hashicorpVaultServer;
    }

    public void setHashicorpVaultServer(HashicorpVaultServerProperty hashicorpVaultServer) {
        this.hashicorpVaultServer = hashicorpVaultServer;
    }

    public DockerInfrastructureProperty getDockerInfrastructure() {
        return dockerInfrastructure;
    }

    public void setDockerInfrastructure(DockerInfrastructureProperty dockerInfrastructure) {
        this.dockerInfrastructure = dockerInfrastructure;
    }

    public String getConsensus() {
        return consensus;
    }

    public void setConsensus(String consensus) {
        this.consensus = consensus;
    }

    /**
     * @return holistic delay post a node or a network startup w.r.t {@link #getConsensus()} value
     */
    public Duration getConsensusGracePeriod() {
        Duration duration = Duration.ofSeconds(15);
        switch (Optional.ofNullable(getConsensus()).orElse("")) {
            case "raft":
                duration = Duration.ofSeconds(30);
                break;
            case "istanbul":
                duration = Duration.ofSeconds(45);
                break;
        }
        return duration;
    }

    public BigInteger getConsensusBlockHeight() {
        switch (Optional.ofNullable(getConsensus()).orElse("")) {
            case "raft":
                return BigInteger.ZERO;
            case "istanbul":
            case "qbft":
                return BigInteger.ONE;
        }
        throw new RuntimeException("Consensus not recognised");
    }

    public OAuth2ServerProperty getOauth2Server() {
        return oauth2Server;
    }

    public void setOauth2Server(OAuth2ServerProperty oauth2Server) {
        this.oauth2Server = oauth2Server;
    }

    public static class SocksProxy {
        /**
         * This configuration allows to create a proxy server that supports dynamic port forwarding
         */
        private SSHTunneling tunnel;

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

        public SSHTunneling getTunnel() {
            return tunnel;
        }

        public void setTunnel(SSHTunneling tunnel) {
            this.tunnel = tunnel;
        }

        public static class SSHTunneling {
            private boolean enabled;
            private boolean autoStart;
            private String user;
            private String host;
            private String privateKeyFile;

            public String getUser() {
                return user;
            }

            public void setUser(String user) {
                this.user = user;
            }

            public String getHost() {
                return host;
            }

            public void setHost(String host) {
                this.host = host;
            }

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public String getPrivateKeyFile() {
                return privateKeyFile;
            }

            public void setPrivateKeyFile(String privateKeyFile) {
                this.privateKeyFile = privateKeyFile;
            }

            public boolean isAutoStart() {
                return autoStart;
            }

            public void setAutoStart(boolean autoStart) {
                this.autoStart = autoStart;
            }
        }
    }

    public static class Node {
        // this value is not expected from YML
        private String name;
        private String privacyAddress;
        private Map<String, String> privacyAddressAliases = new LinkedHashMap<>();
        private Map<String, String> accountAliases = new LinkedHashMap<>();
        private String url;
        private String thirdPartyUrl;
        private String istanbulValidatorId;
        private String enodeUrl;
        private String graphqlUrl;
        private String psi;

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

        public String getIstanbulValidatorId() {
            return istanbulValidatorId;
        }

        public void setIstanbulValidatorId(String istanbulValidatorId) {
            this.istanbulValidatorId = istanbulValidatorId;
        }

        @Override
        public String toString() {
            final String template = "Node[url: %s, privacy-address: %s, validator-address: %s, enode: %s]";
            return String.format(template, url, privacyAddress, istanbulValidatorId, enodeUrl);
        }

        public String getEnodeUrl() {
            return enodeUrl;
        }

        public void setEnodeUrl(String enodeUrl) {
            this.enodeUrl = enodeUrl;
        }

        public String getThirdPartyUrl() {
            return thirdPartyUrl;
        }

        public void setThirdPartyUrl(String thirdPartyUrl) {
            this.thirdPartyUrl = thirdPartyUrl;
        }

        public Map<String, String> getPrivacyAddressAliases() {
            return privacyAddressAliases;
        }

        public void setPrivacyAddressAliases(Map<String, String> privacyAddressAliases) {
            this.privacyAddressAliases = privacyAddressAliases;
        }

        public String getGraphqlUrl() {
            return graphqlUrl;
        }

        public void setGraphqlUrl(String graphqlUrl) {
            this.graphqlUrl = graphqlUrl;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Map<String, String> getAccountAliases() {
            return accountAliases;
        }

        public void setAccountAliases(Map<String, String> accountAliases) {
            this.accountAliases = accountAliases;
        }

        public String getPsi() {
            return psi;
        }

        public void setPsi(String psi) {
            this.psi = psi;
        }
    }

    public static class DockerInfrastructureProperty {
        private boolean enabled;
        private String host;
        private Map<String, DockerContainerProperty> nodes = new HashMap<>();
        // Quorum image provided by Terraform var.quorum_docker_image
        private String targetQuorumImage;
        // Tessera image provided by Terraform var.tessera_docker_image
        private String targetTesseraImage;

        public DockerInfrastructureProperty() {
            this.enabled = false;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Map<String, DockerContainerProperty> getNodes() {
            return nodes;
        }

        public void setNodes(Map<String, DockerContainerProperty> nodes) {
            this.nodes = nodes;
        }

        public String getTargetQuorumImage() {
            return targetQuorumImage;
        }

        public void setTargetQuorumImage(String targetQuorumImage) {
            this.targetQuorumImage = targetQuorumImage;
        }

        public String getTargetTesseraImage() {
            return targetTesseraImage;
        }

        public void setTargetTesseraImage(String targetTesseraImage) {
            this.targetTesseraImage = targetTesseraImage;
        }

        public static class DockerContainerProperty {
            private String quorumContainerId;
            private String tesseraContainerId;
            private String ethSignerContainerId;

            public String getQuorumContainerId() {
                return quorumContainerId;
            }

            public void setQuorumContainerId(String quorumContainerId) {
                this.quorumContainerId = quorumContainerId;
            }

            public String getTesseraContainerId() {
                return tesseraContainerId;
            }

            public void setTesseraContainerId(String tesseraContainerId) {
                this.tesseraContainerId = tesseraContainerId;
            }

            public Optional<String> getEthSignerContainerId() {
                return Optional.ofNullable(ethSignerContainerId);
            }

            public void setEthSignerContainerId(String ethSignerContainerId) {
                this.ethSignerContainerId = ethSignerContainerId;
            }
        }
    }

    public static class OAuth2ServerProperty {
        private String clientEndpoint;
        private String adminEndpoint;

        public OAuth2ServerProperty() {
        }

        public String getClientEndpoint() {
            return clientEndpoint;
        }

        public void setClientEndpoint(String clientEndpoint) {
            this.clientEndpoint = clientEndpoint;
        }

        public String getAdminEndpoint() {
            return adminEndpoint;
        }

        public void setAdminEndpoint(String adminEndpoint) {
            this.adminEndpoint = adminEndpoint;
        }
    }

    public static class HashicorpVaultServerProperty {
        private String url;
        private String caCertPath;
        private String clientCertPath;
        private String clientKeyPath;
        private String tlsTrustStorePath;
        private String tlsTrustStorePassword;
        private String tlsKeyStorePath;
        private String tlsKeyStorePassword;
        private String authToken;
        private Map<String, NodeAcctDirData> nodeAcctDirs;

        public HashicorpVaultServerProperty() {
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getCaCertPath() {
            return caCertPath;
        }

        public void setCaCertPath(String caCertPath) {
            this.caCertPath = caCertPath;
        }

        public String getClientCertPath() {
            return clientCertPath;
        }

        public void setClientCertPath(String clientCertPath) {
            this.clientCertPath = clientCertPath;
        }

        public String getClientKeyPath() {
            return clientKeyPath;
        }

        public void setClientKeyPath(String clientKeyPath) {
            this.clientKeyPath = clientKeyPath;
        }

        public String getTlsTrustStorePath() {
            return tlsTrustStorePath;
        }

        public void setTlsTrustStorePath(String tlsTrustStorePath) {
            this.tlsTrustStorePath = tlsTrustStorePath;
        }

        public String getTlsTrustStorePassword() {
            return tlsTrustStorePassword;
        }

        public void setTlsTrustStorePassword(String tlsTrustStorePassword) {
            this.tlsTrustStorePassword = tlsTrustStorePassword;
        }

        public String getTlsKeyStorePath() {
            return tlsKeyStorePath;
        }

        public void setTlsKeyStorePath(String tlsKeyStorePath) {
            this.tlsKeyStorePath = tlsKeyStorePath;
        }

        public String getTlsKeyStorePassword() {
            return tlsKeyStorePassword;
        }

        public void setTlsKeyStorePassword(String tlsKeyStorePassword) {
            this.tlsKeyStorePassword = tlsKeyStorePassword;
        }

        public String getAuthToken() {
            return authToken;
        }

        public void setAuthToken(String authToken) {
            this.authToken = authToken;
        }

        public Map<String, NodeAcctDirData> getNodeAcctDirs() {
            return nodeAcctDirs;
        }

        public void setNodeAcctDirs(Map<String, NodeAcctDirData> nodeAcctDirs) {
            this.nodeAcctDirs = nodeAcctDirs;
        }
    }

    public static class NodeAcctDirData {
        private String pluginAcctDir;

        public String getPluginAcctDir() {
            return pluginAcctDir;
        }

        public void setPluginAcctDir(String pluginAcctDir) {
            this.pluginAcctDir = pluginAcctDir;
        }
    }
}
