package com.quorum.gauge;

import com.quorum.gauge.common.QuorumNetworkProperty.Node;
import com.quorum.gauge.common.QuorumNode;
import com.quorum.gauge.core.AbstractSpecImplementation;
import com.quorum.gauge.services.QLightService;
import com.thoughtworks.gauge.Step;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static org.assertj.core.api.Assertions.assertThat;

@Service
public class QLight extends AbstractSpecImplementation {

    @Autowired
    QLightService qLightService;
    
    @Step("<node> is a qlight server")
    public void isQLightServer(Node node) {
        assertThat(qLightService.isQLightServer(node)).isTrue();
    }
    
    @Step("<node> is a qlight client")
    public void isQLightClient(Node node) {
        QuorumNode n = networkProperty.getQuorumNode(node);
        assertThat(qLightService.isQLightClient(n)).isTrue();
    }

}
