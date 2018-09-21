package com.quorum.gauge.services;

import com.quorum.gauge.common.QuorumNode;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import rx.Observable;

@Service
public class AccountService extends AbstractService {
    public String getDefaultAccountAddress(QuorumNode node) {
        return connectionFactory.getConnection(node).ethAccounts().observable().toBlocking().first().getAccounts().get(0);
    }

    public Observable<EthGetBalance> getDefaultAccountBalanceObservable(QuorumNode node) {
        return connectionFactory.getConnection(node).ethGetBalance(getDefaultAccountAddress(node), DefaultBlockParameterName.LATEST).observable();
    }
}
