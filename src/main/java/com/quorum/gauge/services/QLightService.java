package com.quorum.gauge.services;

import com.quorum.gauge.common.QuorumNode;
import org.springframework.stereotype.Service;

@Service
public class QLightService extends AbstractService {
    public boolean isQLightClient(QuorumNode node) {
        return networkProperty().getNode(node.name()).getIsQLightClient();
    }
}
