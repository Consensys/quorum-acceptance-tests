package com.quorum.gauge.services;

import com.quorum.gauge.common.QuorumNode;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.quorum.Quorum;

import java.io.IOException;
import java.util.Optional;

@Service
public class TransactionService extends AbstractService {
    public Optional<TransactionReceipt> getTransactionReceipt(QuorumNode node, String transactionHash) {
        Quorum client = connectionFactory.getConnection(node);
        try {
            return client.ethGetTransactionReceipt(transactionHash).send().getTransactionReceipt();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
