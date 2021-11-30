package com.quorum.gauge.ext;

import org.web3j.exceptions.MessageDecodingException;
import org.web3j.protocol.core.Response;
import org.web3j.utils.Numeric;


public class EthChainId extends Response<String> {
    public EthChainId() {
    }

    public long getChainId() {
        try {
            return Numeric.decodeQuantity(this.getResult()).longValue();
        } catch (org.web3j.exceptions.MessageDecodingException ex) {
          throw new MessageDecodingException( "Can not decode '"+ this.getResult()+ "'", ex);
        }
    }
}
