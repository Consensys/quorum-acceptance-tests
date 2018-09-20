package com.quorum.gauge.services;

import com.quorum.gauge.common.QuorumNode;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class AccountService extends AbstractService {
    public String getFirstAccountAddress(QuorumNode node) {
        try {
            return connectionFactory.getConnection(node).ethAccounts().send().getAccounts().get(0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
