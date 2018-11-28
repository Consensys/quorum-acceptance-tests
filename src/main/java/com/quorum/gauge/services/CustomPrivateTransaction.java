package com.quorum.gauge.services;

import org.web3j.protocol.core.methods.request.Transaction;

import java.math.BigInteger;
import java.util.List;

public class CustomPrivateTransaction extends Transaction {
    private String privateFrom;
    private List<String> privateFor;

    public CustomPrivateTransaction(
            String from, BigInteger nonce, BigInteger gasPrice, BigInteger gasLimit, String to, BigInteger value,
            String data, String privateFrom, List<String> privateFor) {
        super(from, nonce, gasPrice, gasLimit, to, value, data);
        this.privateFrom = privateFrom;
        this.privateFor = privateFor;
    }

    public String getPrivateFrom() {
        return privateFrom;
    }

    public void setPrivateFrom(final String privateFrom) {
        this.privateFrom = privateFrom;
    }

    public List<String> getPrivateFor() {
        return privateFor;
    }

    public void setPrivateFor(List<String> privateFor) {
        this.privateFor = privateFor;
    }
}
