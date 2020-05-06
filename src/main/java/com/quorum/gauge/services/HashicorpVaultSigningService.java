package com.quorum.gauge.services;

import org.springframework.stereotype.Service;
import org.web3j.protocol.core.methods.request.Transaction;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;

/**
 * Client to Hashicorp Vault server and keeps track of new account details across test steps
 */
@Service
public class HashicorpVaultSigningService extends HashicorpVaultAbstractService {

    public Transaction toSign(BigInteger nonce, String from) {
        return new PrivateTransactionGasPrice(from, nonce, DEFAULT_GAS_LIMIT, BigInteger.ZERO, null, null, "0x000000", null, Collections.singletonList("0xaaaaaa"));
    }

    private static class PrivateTransactionGasPrice extends Transaction {
        private String privateFrom;
        private List<String> privateFor;

        public PrivateTransactionGasPrice(String from, BigInteger nonce, BigInteger gasLimit, BigInteger gasPrice, String to, BigInteger value,String data, String privateFrom, List<String> privateFor) {
            super(from, nonce, gasPrice, gasLimit, to, value, data);
            this.privateFrom = privateFrom;
            this.privateFor = privateFor;
        }

        public String getPrivateFrom() {
            return privateFrom;
        }

        public void setPrivateFrom(final String privateFrom) {
            this.privateFrom = privateFrom;
        }

        public List<String> getPrivateFor() {
            return privateFor;
        }

        public void setPrivateFor(List<String> privateFor) {
            this.privateFor = privateFor;
        }
    }

}
