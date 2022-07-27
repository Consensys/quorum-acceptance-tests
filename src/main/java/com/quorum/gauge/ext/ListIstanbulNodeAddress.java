package com.quorum.gauge.ext;

import org.web3j.protocol.core.Response;

import java.util.List;

public class ListIstanbulNodeAddress extends Response<List<String>> {
    public List<String> get() {
        return getResult();
    }
}
