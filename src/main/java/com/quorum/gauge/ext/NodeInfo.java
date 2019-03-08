package com.quorum.gauge.ext;

import org.web3j.protocol.core.Response;

import java.util.LinkedHashMap;

public class NodeInfo extends Response<LinkedHashMap> {

    @SuppressWarnings("unchecked")
    public String getEnode() {
        if (getResult() == null) {
            return null;
        }

        if (getResult() instanceof LinkedHashMap) {
            LinkedHashMap<String, String> result = getResult();
            return result.get("enode");
        }

        throw new RuntimeException("Unexpected error: expected NodeInfo response to be of type LinkedHashMap, but got: " + getResult().getClass());
    }

}