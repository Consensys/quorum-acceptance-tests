package com.quorum.gauge.ext.privacyprecompile;

import org.web3j.protocol.core.methods.response.TransactionReceipt;

/**
 * Same as {@link org.web3j.protocol.core.methods.response.Transaction} but adding support for isPrivacyMarkerTransaction
 * field
 */
public class PMTEnabledTransactionReceipt extends TransactionReceipt {
    private boolean isPrivacyMarkerTransaction;

    public boolean getIsPrivacyMarkerTransaction() {
        return isPrivacyMarkerTransaction;
    }

    public void setIsPrivacyMarkerTransaction(boolean privacyMarkerTransaction) {
        this.isPrivacyMarkerTransaction = privacyMarkerTransaction;
    }
}
