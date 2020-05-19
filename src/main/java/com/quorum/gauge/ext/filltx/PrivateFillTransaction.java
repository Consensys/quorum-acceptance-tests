package com.quorum.gauge.ext.filltx;

import org.web3j.protocol.core.methods.request.Transaction;

import java.math.BigInteger;
import java.util.List;

public class PrivateFillTransaction extends Transaction {
    private String privateFrom;
    private List<String> privateFor;

    public PrivateFillTransaction(String from, BigInteger nonce, BigInteger gasLimit, String to, BigInteger value, String data, String privateFrom, List<String> privateFor) {
        super(from, nonce, BigInteger.ZERO, gasLimit, to, value, data);
        this.privateFrom = privateFrom;
        this.privateFor = privateFor;
    }

    public String getPrivateFrom() {
        return this.privateFrom;
    }

    public void setPrivateFrom(String privateFrom) {
        this.privateFrom = privateFrom;
    }

    public List<String> getPrivateFor() {
        return this.privateFor;
    }

    public void setPrivateFor(List<String> privateFor) {
        this.privateFor = privateFor;
    }
}
