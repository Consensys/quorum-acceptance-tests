package com.quorum.gauge.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.quorum.gauge.common.QuorumNetworkProperty.Node;
import com.quorum.gauge.common.QuorumNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class QLightService extends AbstractService {
    private static final Logger LOGGER = LoggerFactory.getLogger(QLightService.class);

    @Autowired
    AdminService adminService;

    public boolean isQLightClient(QuorumNode node) {
        return networkProperty().getNode(node.name()).getQlight().getIsClient();
    }

    public boolean isQLightServer(Node node) {
        JsonNode qnodeInfo = adminService.qnodeInfo(node).blockingFirst().getResult();

        return Optional.ofNullable(qnodeInfo.get("protocols"))
            .filter(p -> 1 == p.size())
            .filter(p -> p.has("qlight"))
            .isPresent();
    }

}
