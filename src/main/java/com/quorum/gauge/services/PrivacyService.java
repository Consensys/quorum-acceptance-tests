package com.quorum.gauge.services;

import com.quorum.gauge.common.QuorumNetworkProperty;
import com.quorum.gauge.common.QuorumNode;
import org.springframework.stereotype.Service;

@Service
public class PrivacyService extends AbstractService {
    public String id(QuorumNode node) {
        QuorumNetworkProperty.Node nodeConfig = networkProperty.getNodes().get(node);
        if (nodeConfig == null) {
            throw new IllegalArgumentException("Node " + node + " not found in config");
        }
        return nodeConfig.getPrivacyAddress();
    }
}
