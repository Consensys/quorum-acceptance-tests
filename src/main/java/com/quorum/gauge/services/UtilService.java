package com.quorum.gauge.services;

import com.quorum.gauge.common.QuorumNode;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthBlockNumber;
import rx.Observable;

@Service
public class UtilService extends AbstractService {

    public Observable<EthBlockNumber> getCurrentBlockNumber() {
        Web3j client = connectionFactory.getWeb3jConnection(QuorumNode.Node1);
        return client.ethBlockNumber().observable();
    }
}
