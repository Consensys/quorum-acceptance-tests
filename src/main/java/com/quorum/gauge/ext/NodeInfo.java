package com.quorum.gauge.ext;

import org.web3j.protocol.core.Response;

import java.util.LinkedHashMap;

public class NodeInfo extends Response<LinkedHashMap<Object, Object>> {

    public String getEnode() {
        final LinkedHashMap<Object, Object> result = this.getResult();

        if ((result == null) || (result.get("enode") == null)) {
            return null;
        }

        return result.get("enode").toString();
    }

}
