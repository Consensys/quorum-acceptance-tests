package com.quorum.gauge;

import com.quorum.gauge.common.QuorumNetworkProperty;
import com.quorum.gauge.core.AbstractSpecImplementation;
import com.quorum.gauge.ext.privacyprecompile.PrivacyPrecompileTransaction;
import com.thoughtworks.gauge.Step;
import com.thoughtworks.gauge.datastore.DataStoreFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@Service
public class PrivacyMarkerTransactions extends AbstractSpecImplementation {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrivacyMarkerTransactions.class);

    @Step("<contractName>'s creation transaction retrieved by <node> is a privacy marker transaction")
    public void validateContractCreationTransactionIsPMT(String contractName, QuorumNetworkProperty.Node node) {
        final String txHash = mustHaveValue(DataStoreFactory.getScenarioDataStore(), contractName + "_transactionHash", String.class);
        validate(node, txHash);
    }

    @Step("Transaction <txRef> retrieved by <node> is a privacy marker transaction")
    public void validateTransactionIsPMT(String txRef, QuorumNetworkProperty.Node node) {
        final String txHash = mustHaveValue(DataStoreFactory.getScenarioDataStore(), txRef + "_transactionHash", String.class);
        validate(node, txHash);
    }

    private void validate(QuorumNetworkProperty.Node node, String txHash) {
        String privacyPrecompileAddress = privacyMarkerTransactionService.getPrivacyPrecompileAddress(node).blockingFirst().getResult();

        PrivacyPrecompileTransaction pmt = privacyMarkerTransactionService.getTransaction(node, txHash).blockingFirst().getTransaction().get();

        PrivacyPrecompileTransaction pvtTx = privacyMarkerTransactionService.getPrivateTransaction(node, txHash).blockingFirst().getTransaction().get();

        validatePMT(pmt, privacyPrecompileAddress);
        validatePrivateTx(pvtTx, privacyPrecompileAddress);

        assertThat(pmt.getFrom()).withFailMessage("PMT and private tx should have same 'from'").isEqualTo(pvtTx.getFrom());
        assertThat(pmt.getNonce()).withFailMessage("PMT and private tx should have same 'nonce'").isEqualTo(pvtTx.getNonce());
    }

    private void validatePMT(PrivacyPrecompileTransaction pmt, String privacyPrecompileAddress) {
        assertThat(pmt.isPrivacyMarkerTransaction()).withFailMessage("PMT's 'isPrivacyMarkerTransaction' should be 'true'").isTrue();

        assertThat(Collections.singleton(pmt.getV())).withFailMessage("PMT should not be a private transaction").doesNotContain(37L, 38L);

        assertThat(pmt.getTo()).withFailMessage("PMT's 'to' should be privacy precompile contract address").isEqualTo(privacyPrecompileAddress);

        assertThat(pmt.getInput()).withFailMessage("PMT's 'data' should be 64 byte TM Hash").hasSize(128);
    }

    private void validatePrivateTx(PrivacyPrecompileTransaction tx, String privacyPrecompileAddress) {
        assertThat(tx.isPrivacyMarkerTransaction()).withFailMessage("Private tx's 'isPrivacyMarkerTransaction' should be 'false'").isFalse();

        assertThat(Collections.singleton(tx.getV())).withFailMessage("should be a private transaction").containsAnyOf(37L, 38L);

        assertThat(tx.getTo()).withFailMessage("Private tx's 'to' should not be privacy precompile contract address").isNotEqualTo(privacyPrecompileAddress);

        assertThat(tx.getInput()).withFailMessage("Private tx's 'data' should be 64 byte TM Hash").hasSize(128);
    }

}
