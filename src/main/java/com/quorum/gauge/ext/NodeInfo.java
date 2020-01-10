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
    public String getConsensus() {
        final LinkedHashMap<Object, Object> result = this.getResult();

        if ((result == null) || (result.get("protocols") == null)) {
            return null;
        }
        Object o1 = ((LinkedHashMap)result.get("protocols")).get("eth");
        Object o2 = ((LinkedHashMap)result.get("protocols")).get("istanbul");
        if(o1 != null){
            return ((LinkedHashMap)o1).get("consensus").toString();
        }

        if (o2 != null){
            return ((LinkedHashMap)o2).get("consensus").toString();
        }

        return "";
    }

}
