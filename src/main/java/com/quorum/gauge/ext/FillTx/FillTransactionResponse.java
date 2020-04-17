package com.quorum.gauge.ext.FillTx;

import org.web3j.protocol.core.Response;


public class FillTransactionResponse extends Response<FillTransaction> {
    public FillTransaction getResponseObject() {
        return getResult();
    }
}
