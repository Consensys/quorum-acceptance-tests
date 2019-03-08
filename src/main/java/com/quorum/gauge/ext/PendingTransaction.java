package com.quorum.gauge.ext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.response.Transaction;

import java.util.ArrayList;

public class PendingTransaction extends Response<ArrayList> {
    private static final Logger logger = LoggerFactory.getLogger(PendingTransaction.class);

    @SuppressWarnings("unchecked")
    public ArrayList<Transaction> getTransactions() {
        if (getResult() == null) {
            return null;
        }

        if (getResult() instanceof ArrayList) {
            ArrayList<Transaction> result = getResult();
            return result;
        }

        throw new RuntimeException("Unexpected error: expected PendingTransaction response to be of type ArrayList, but got: " + getResult().getClass());
    }

}