package com.quorum.gauge.ext;

import org.web3j.quorum.methods.request.PrivateTransaction;

import java.math.BigInteger;
import java.util.List;

public class PrivateTransactionAsync extends PrivateTransaction {
    private String callbackUrl;

    public PrivateTransactionAsync(String from, BigInteger nonce, BigInteger gasLimit, String to, BigInteger value, String data, String privateFrom, List<String> privateFor, String callbackUrl) {
        super(from, nonce, gasLimit, to, value, data, privateFrom, privateFor);
        this.callbackUrl = callbackUrl;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }
}
