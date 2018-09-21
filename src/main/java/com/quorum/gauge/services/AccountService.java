package com.quorum.gauge.services;

import com.quorum.gauge.common.QuorumNode;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import rx.Observable;

@Service
public class AccountService extends AbstractService {
    public Observable<String> getAccountAddresses(QuorumNode node) {
        return connectionFactory.getConnection(node).ethAccounts().observable()
                .flatMap(ethAccounts -> Observable.from(ethAccounts.getAccounts()));
    }

    public Observable<String> getDefaultAccountAddress(QuorumNode node) {
        return getAccountAddresses(node).first();
    }

    public Observable<EthGetBalance> getDefaultAccountBalance(QuorumNode node) {
        return getDefaultAccountAddress(node).flatMap(
                s -> connectionFactory.getConnection(node).ethGetBalance(s, DefaultBlockParameterName.LATEST).observable());
    }
}
