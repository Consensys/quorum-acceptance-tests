package com.quorum.gauge.ext;

import org.web3j.protocol.core.Response;

import java.util.LinkedHashMap;

public class NodeInfo extends Response<LinkedHashMap<String, String>> {

    public String getEnode() {
        final LinkedHashMap<String, String> result = this.getResult();

        if (result == null) {
            return null;
        }

        return result.get("enode");
    }

}
