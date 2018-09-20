package com.quorum.gauge.services;

import com.quorum.gauge.common.QuorumNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.quorum.Quorum;
import org.web3j.quorum.methods.request.PrivateTransaction;
import rx.Observable;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Optional;

@Service
public class TransactionService extends AbstractService {
    @Autowired
    AccountService accountService;

    @Autowired
    PrivacyService privacyService;

    public Optional<TransactionReceipt> getTransactionReceipt(QuorumNode node, String transactionHash) {
        return getTransactionReceiptObservable(node, transactionHash).toBlocking().first().getTransactionReceipt();
    }

    public Observable<EthGetTransactionReceipt> getTransactionReceiptObservable(QuorumNode node, String transactionHash) {
        Quorum client = connectionFactory.getConnection(node);
        return client.ethGetTransactionReceipt(transactionHash).observable();
    }

    public String sendSignedTransaction(QuorumNode from, QuorumNode to) {
        Quorum client = connectionFactory.getConnection(from);
        String fromAddress = accountService.getFirstAccountAddress(from);
        String toAddress = accountService.getFirstAccountAddress(to);
        try {
            BigInteger nonce = client.ethGetTransactionCount(fromAddress, DefaultBlockParameterName.LATEST).send().getTransactionCount();
            PrivateTransaction privateTx = new PrivateTransaction(fromAddress, nonce,
                    new BigInteger("47b760", 16),
                    toAddress,
                    BigInteger.valueOf(0),
                    "",
                    Arrays.asList(privacyService.id(to)));
            return client.ethSendTransaction(privateTx).send().getTransactionHash();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
