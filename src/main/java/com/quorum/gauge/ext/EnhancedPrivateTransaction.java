package com.quorum.gauge.ext;

import org.web3j.quorum.PrivacyFlag;
import org.web3j.quorum.methods.request.PrivateTransaction;

import java.math.BigInteger;
import java.util.List;

public class EnhancedPrivateTransaction extends PrivateTransaction {

    private final int privacyFlag;

    private List<String> mandatoryFor;

    public EnhancedPrivateTransaction(String from, BigInteger nonce, BigInteger gasLimit, String to, BigInteger value, String data, String privateFrom, List<String> privateFor, PrivacyFlag flag) {
        super(from, nonce, gasLimit, to, value, data, privateFrom, privateFor);
        this.privacyFlag = flag.getValue();
    }


    public int getPrivacyFlag() {
        return privacyFlag;
    }

    public List<String> getMandatoryFor() {
        return mandatoryFor;
    }
}
