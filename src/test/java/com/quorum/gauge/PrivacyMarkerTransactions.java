package com.quorum.gauge;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.quorum.gauge.common.QuorumNetworkProperty;
import com.quorum.gauge.core.AbstractSpecImplementation;
import com.quorum.gauge.ext.privacyprecompile.PMTEnabledTransactionReceipt;
import com.thoughtworks.gauge.Step;
import com.thoughtworks.gauge.datastore.DataStoreFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.methods.response.Transaction;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@Service
public class PrivacyMarkerTransactions extends AbstractSpecImplementation {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrivacyMarkerTransactions.class);

    @Step("Store <contractName>'s PMT receipt retrieved by <node> as <receiptRef>")
    public void storePmtReceipt(String contractName, QuorumNetworkProperty.Node node, String receiptRef) {
        final String txHash = mustHaveValue(DataStoreFactory.getScenarioDataStore(), contractName + "_transactionHash", String.class);

        PMTEnabledTransactionReceipt r = privacyMarkerTransactionService.getTransactionReceipt(node, txHash).blockingFirst().getTransactionReceipt().get();
        DataStoreFactory.getScenarioDataStore().put(receiptRef, r);
    }

    @Step("Store <contractName>'s private tx receipt retrieved by <node> as <receiptRef>")
    public void storePrivateTxReceipt(String contractName, QuorumNetworkProperty.Node node, String receiptRef) {
        final String txHash = mustHaveValue(DataStoreFactory.getScenarioDataStore(), contractName + "_transactionHash", String.class);

        PMTEnabledTransactionReceipt r = privacyMarkerTransactionService.getPrivateTransactionReceipt(node, txHash).blockingFirst().getTransactionReceipt().get();
        DataStoreFactory.getScenarioDataStore().put(receiptRef, r);
    }

    @Step("<receiptRef> has privacyMarkerTransaction equal to <want>")
    public void validateReceiptIsPrivacyMarkerTransaction(String receiptRef, Boolean want) {
        final PMTEnabledTransactionReceipt r = mustHaveValue(DataStoreFactory.getScenarioDataStore(), receiptRef, PMTEnabledTransactionReceipt.class);

        assertThat(r.getIsPrivacyMarkerTransaction()).isEqualTo(want);
    }

    @Step("<receiptRef> has contractAddress equal to null is <want>")
    public void validateReceiptContractAddress(String receiptRef, Boolean wantNull) {
        final PMTEnabledTransactionReceipt r = mustHaveValue(DataStoreFactory.getScenarioDataStore(), receiptRef, PMTEnabledTransactionReceipt.class);

        if (wantNull) {
            assertThat(r.getContractAddress()).isNull();
        } else {
            assertThat(r.getContractAddress()).isNotNull();
        }
    }

    @Step("<receiptRefA> and <receiptRefB> have same transactionHash")
    public void validateReceiptTransactionHash(String receiptRefA, String receiptRefB) {
        PMTEnabledTransactionReceipt rA = mustHaveValue(DataStoreFactory.getScenarioDataStore(), receiptRefA, PMTEnabledTransactionReceipt.class);
        PMTEnabledTransactionReceipt rB = mustHaveValue(DataStoreFactory.getScenarioDataStore(), receiptRefB, PMTEnabledTransactionReceipt.class);

        assertThat(rA.getTransactionHash()).isEqualTo(rB.getTransactionHash());
    }

    @Step("<receiptRef> has gasUsed equal to 0 is <wantZero>")
    public void validateGasUsed(String receiptRef, Boolean wantZero) {
        final PMTEnabledTransactionReceipt r = mustHaveValue(DataStoreFactory.getScenarioDataStore(), receiptRef, PMTEnabledTransactionReceipt.class);

        if (wantZero) {
            assertThat(r.getGasUsed()).isZero();
        } else {
            assertThat(r.getGasUsed()).isNotZero();
        }
    }

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

        Transaction pmt = privacyMarkerTransactionService.getTransaction(node, txHash).blockingFirst().getTransaction().get();

        Transaction pvtTx = privacyMarkerTransactionService.getPrivateTransaction(node, txHash).blockingFirst().getTransaction().get();

        validatePMT(pmt, privacyPrecompileAddress);
        validatePrivateTx(pvtTx, privacyPrecompileAddress);

        assertThat(pmt.getFrom()).as("PMT and private tx should have same 'from'").isEqualTo(pvtTx.getFrom());
        assertThat(pmt.getNonce()).as("PMT and private tx should have same 'nonce'").isEqualTo(pvtTx.getNonce());
    }

    private void validatePMT(Transaction pmt, String privacyPrecompileAddress) {
        assertThat(Collections.singleton(pmt.getV())).withFailMessage("PMT should not be a private transaction").doesNotContain(37L, 38L);

        assertThat(pmt.getTo()).as("PMT's 'to' should be privacy precompile contract address").isEqualTo(privacyPrecompileAddress);

        assertThat(pmt.getInput()).as("PMT's 'data' should be 64 byte TM Hash").hasSize(130);
    }

    private void validatePrivateTx(Transaction tx, String privacyPrecompileAddress) {
        assertThat(Collections.singleton(tx.getV())).withFailMessage("should be a private transaction").containsAnyOf(37L, 38L);

        assertThat(tx.getTo()).as("Private tx's 'to' should not be privacy precompile contract address").isNotEqualTo(privacyPrecompileAddress);

        assertThat(tx.getInput()).as("Private tx's 'data' should be 64 byte TM Hash").hasSize(130);
    }

}
