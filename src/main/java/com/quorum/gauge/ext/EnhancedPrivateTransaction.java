package com.quorum.gauge.ext;

import com.quorum.gauge.common.PrivacyFlag;
import org.web3j.quorum.methods.request.PrivateTransaction;

import java.math.BigInteger;
import java.util.List;

public class EnhancedPrivateTransaction extends PrivateTransaction {

    private int privacyFlag;

    private List<String> mandatoryFor;

    public EnhancedPrivateTransaction(String from, BigInteger nonce, BigInteger gasLimit, String to, BigInteger value, String data, String privateFrom, List<String> privateFor, List<PrivacyFlag> flags) {
        super(from, nonce, gasLimit, to, value, data, privateFrom, privateFor);
        int flag = 0;
        for (PrivacyFlag f : flags) {
            flag = flag | f.intValue();
        }
        this.privacyFlag = flag;
    }

    public EnhancedPrivateTransaction(String from, BigInteger nonce, BigInteger gasLimit, String to, BigInteger value, String data, String privateFrom, List<String> privateFor, List<String> mandatoryFor, List<PrivacyFlag> flags) {
        this(from, nonce, gasLimit, to, value, data, privateFrom, privateFor, flags);
        this.mandatoryFor = mandatoryFor;
    }

    public int getPrivacyFlag() {
        return privacyFlag;
    }

    public List<String> getMandatoryFor() {
        return mandatoryFor;
    }
}
