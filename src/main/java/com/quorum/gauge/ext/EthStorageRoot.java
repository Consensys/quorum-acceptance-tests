package com.quorum.gauge.ext;

import org.web3j.protocol.core.Response;

public class EthStorageRoot extends Response<String> {
    public String getData() {
        return getResult();
    }
}
