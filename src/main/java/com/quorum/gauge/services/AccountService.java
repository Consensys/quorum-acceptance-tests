package com.quorum.gauge.services;

import com.quorum.gauge.common.QuorumNode;
import org.springframework.stereotype.Service;

@Service
public class AccountService extends AbstractService {
    public String getFirstAccountAddress(QuorumNode node) {
        return connectionFactory.getConnection(node).ethAccounts().observable().toBlocking().first().getAccounts().get(0);
    }
}
