package com.quorum.gauge.ext.filltx;

import org.web3j.protocol.core.Response;


public class FillTransactionResponse extends Response<FillTransaction> {
    public FillTransaction getResponseObject() {
        return getResult();
    }
}
