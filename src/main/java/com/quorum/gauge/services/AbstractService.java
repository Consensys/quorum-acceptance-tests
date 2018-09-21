package com.quorum.gauge.services;

import com.quorum.gauge.common.QuorumNetworkProperty;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigInteger;

public abstract class AbstractService {

    BigInteger DEFAULT_GAS_LIMIT = new BigInteger("47b760", 16);

    @Autowired
    QuorumNodeConnectionFactory connectionFactory;

    @Autowired
    QuorumNetworkProperty networkProperty;
}
