package com.quorum.gauge.ext;

import org.web3j.protocol.core.Response;

public class EthSendTransactionAsync extends Response<String> {
    public String getTransactionHash() {
        return super.getResult();
    }
}
