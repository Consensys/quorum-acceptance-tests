package com.quorum.gauge.services;

import com.quorum.gauge.common.QuorumNetworkProperty;
import com.quorum.gauge.common.QuorumNode;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.protocol.http.HttpService;
import org.web3j.quorum.Quorum;

@Service
public class QuorumNodeConnectionFactory {
    @Autowired
    QuorumNetworkProperty networkProperty;

    @Autowired
    OkHttpClient okHttpClient;

    public Quorum getConnection(QuorumNode node) {
        QuorumNetworkProperty.Node nodeConfig = networkProperty.getNodes().get(node);
        if (nodeConfig == null) {
            throw new IllegalArgumentException("Can't find node " + node + " in the configuration");
        }
        return Quorum.build(new HttpService(nodeConfig.getUrl(), okHttpClient, false));
    }
}
