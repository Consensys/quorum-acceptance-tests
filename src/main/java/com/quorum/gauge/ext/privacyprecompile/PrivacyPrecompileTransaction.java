package com.quorum.gauge.ext.privacyprecompile;

/**
 * Same as {@link org.web3j.protocol.core.methods.response.Transaction} but adding support for isPrivacyMarkerTransaction
 * field
 */
public class PrivacyPrecompileTransaction extends org.web3j.protocol.core.methods.response.Transaction {
    private boolean isPrivacyMarkerTransaction;

    public boolean isPrivacyMarkerTransaction() {
        return isPrivacyMarkerTransaction;
    }

    public void setPrivacyMarkerTransaction(boolean privacyMarkerTransaction) {
        isPrivacyMarkerTransaction = privacyMarkerTransaction;
    }
}
