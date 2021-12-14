package com.quorum.gauge.ext;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.exceptions.TransactionException;
import org.web3j.tx.ClientTransactionManager;

import java.io.IOException;
import java.math.BigInteger;

public class PublicClientTransactionManager extends ClientTransactionManager {
    private Web3j web3j;
    private long chainId;

    public PublicClientTransactionManager(final Web3j web3j, final String fromAddress, final long chainId) {
        super(web3j,fromAddress);
        this.web3j = web3j;
        this.chainId = chainId;
    }

    @Override
    public EthSendTransaction sendTransaction(BigInteger gasPrice, BigInteger gasLimit, String to, String data, BigInteger value, boolean constructor) throws IOException {

        Transaction transaction = new Transaction(this.getFromAddress(), null, gasPrice, gasLimit, to, value, data, chainId, null, null);
        return this.web3j.ethSendTransaction(transaction).send();
    }
}
