package com.quorum.gauge.services;

import com.quorum.gauge.common.QuorumNetworkProperty;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractService {
    @Autowired
    QuorumNodeConnectionFactory connectionFactory;

    @Autowired
    QuorumNetworkProperty networkProperty;
}
