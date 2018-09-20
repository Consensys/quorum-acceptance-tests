package com.quorum.gauge.common;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "quorum")
public class QuorumNetworkProperty {
    private Map<QuorumNode, Node> nodes = new HashMap<>();
    private SocksProxy socksProxy;

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
        private String privacyAddress;
        private String url;

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
    }
}
