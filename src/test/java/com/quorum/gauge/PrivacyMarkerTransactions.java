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
import org.web3j.utils.Numeric;
import org.web3j.utils.Strings;

import java.util.Collections;
import java.util.Optional;

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

    @Step("<txRef> retrieved from <node> is a public transaction")
    public void isPublicTransaction(String txRef, QuorumNetworkProperty.Node node) {
        Transaction tx = getTransaction(txRef, node).get();

        assertThat(Collections.singleton(tx.getV())).doesNotContain(37L, 38L);
    }

    @Step("<txRef> retrieved from <node> is a private transaction")
    public void isPrivateTransaction(String txRef, QuorumNetworkProperty.Node node) {
        Transaction tx = getTransaction(txRef, node).get();

        assertThat(Collections.singleton(tx.getV())).containsAnyOf(37L, 38L);
    }

    @Step("recipient of <txRef> retrieved from <node> is the privacy precompile contract")
    public void recipientIsPrivacyPrecompile(String txRef, QuorumNetworkProperty.Node node) {
        String privacyPrecompileAddress = getPrivacyPrecompileAddress(node);
        Transaction tx = getTransaction(txRef, node).get();

        assertThat(tx.getTo()).isEqualTo(privacyPrecompileAddress);
    }

    @Step("recipient of <txRef> retrieved from <node> is not the privacy precompile contract")
    public void recipientIsNotPrivacyPrecompile(String txRef, QuorumNetworkProperty.Node node) {
        String privacyPrecompileAddress = getPrivacyPrecompileAddress(node);
        Transaction tx = getTransaction(txRef, node).get();

        assertThat(tx.getTo()).isNotEqualTo(privacyPrecompileAddress);
    }

    @Step("data of <txRef> retrieved from <node> has been replaced by a Transaction Manager hash")
    public void dataIsTmHash(String txRef, QuorumNetworkProperty.Node node) {
        Transaction tx = getTransaction(txRef, node).get();

        assertThat(Numeric.hexStringToByteArray(tx.getInput())).hasSize(64);
    }

    @Step("<txRef>'s PMT and private transaction retrieved from <node> share sender")
    public void senderSharedByPmtAndPvtTx(String txRef, QuorumNetworkProperty.Node node) {
        Transaction pmt = getTransaction(txRef, node).get();
        Transaction pvtTx = getPrivateTransaction(txRef, node).get();

        assertThat(pmt.getFrom()).isEqualTo(pvtTx.getFrom());
    }

    @Step("<txRef>'s PMT and private transaction retrieved from <node> share nonce")
    public void nonceSharedByPmtAndPvtTx(String txRef, QuorumNetworkProperty.Node node) {
        Transaction pmt = getTransaction(txRef, node).get();
        Transaction pvtTx = getPrivateTransaction(txRef, node).get();

        assertThat(pmt.getNonce()).isEqualTo(pvtTx.getNonce());
    }

    @Step("<txRef>'s internal transaction retrieved from <node> is not null")
    public void internalTransactionRetrieved(String txRef, QuorumNetworkProperty.Node node) {
        assertThat(getPrivateTransaction(txRef, node)).isPresent();
    }

    @Step("<txRef>'s internal transaction retrieved from <node> is null")
    public void internalTransactionNotRetrieved(String txRef, QuorumNetworkProperty.Node node) {
        assertThat(getPrivateTransaction(txRef, node)).isNotPresent();
    }

    @Step("<txRef>'s internal transaction retrieved from <node> is a private transaction")
    public void internalTransactionIsPrivate(String txRef, QuorumNetworkProperty.Node node) {
        Transaction tx = getPrivateTransaction(txRef, node).get();
        assertThat(Collections.singleton(tx.getV())).containsAnyOf(37L, 38L);
    }

    @Step("recipient of <txRef>'s internal transaction retrieved from <node> is not the privacy precompile contract")
    public void internalTxRecipientIsNotPrivacyPrecompile(String txRef, QuorumNetworkProperty.Node node) {
        String privacyPrecompileAddress = privacyMarkerTransactionService.getPrivacyPrecompileAddress(node).blockingFirst().getResult();
        Transaction tx = getPrivateTransaction(txRef, node).get();
        assertThat(tx.getTo()).isNotEqualTo(privacyPrecompileAddress);
    }

    @Step("data of <txRef>'s internal transaction retrieved from <node> has been replaced by a Transaction Manager hash")
    public void internalTxDataIsTmHash(String txRef, QuorumNetworkProperty.Node node) {
        Transaction tx = getPrivateTransaction(txRef, node).get();
        assertThat(Numeric.hexStringToByteArray(tx.getInput())).hasSize(64);
    }

    private Optional<Transaction> getTransaction(String txRef, QuorumNetworkProperty.Node node) {
        String ref = String.format("%s_%s_getTransaction", txRef, node.getName());
        Optional<Transaction> alreadyGot = haveValue(DataStoreFactory.getScenarioDataStore(), ref, Transaction.class);
        if (alreadyGot.isPresent()) {
            System.out.println("already got " + ref);

            return alreadyGot;
        }

        System.out.println("getting " + ref);

        String txHash = mustHaveValue(DataStoreFactory.getScenarioDataStore(), txRef, String.class);
        Optional<Transaction> tx = privacyMarkerTransactionService.getTransaction(node, txHash).blockingFirst().getTransaction();

        DataStoreFactory.getScenarioDataStore().put(ref, tx.orElse(null));

        return tx;
    }

    private Optional<Transaction> getPrivateTransaction(String txRef, QuorumNetworkProperty.Node node) {
        String ref = String.format("%s_%s_getPrivateTransaction", txRef, node.getName());
        Optional<Transaction> alreadyGot = haveValue(DataStoreFactory.getScenarioDataStore(), ref, Transaction.class);
        if (alreadyGot.isPresent()) {
            System.out.println("already got " + ref);

            return alreadyGot;
        }

        System.out.println("getting " + ref);

        String txHash = mustHaveValue(DataStoreFactory.getScenarioDataStore(), txRef, String.class);
        Optional<Transaction> tx = privacyMarkerTransactionService.getPrivateTransaction(node, txHash).blockingFirst().getTransaction();

        DataStoreFactory.getScenarioDataStore().put(ref, tx.orElse(null));

        return tx;
    }

    private String getPrivacyPrecompileAddress(QuorumNetworkProperty.Node node) {
        String ref = String.format("%s_getPrivacyPrecompileAddress", node.getName());
        Optional<String> alreadyGot = haveValue(DataStoreFactory.getScenarioDataStore(), ref, String.class);
        if (alreadyGot.isPresent()) {
            System.out.println("already got " + ref);

            return alreadyGot.get();
        }

        System.out.println("getting " + ref);

        String privacyPrecompileAddress = privacyMarkerTransactionService.getPrivacyPrecompileAddress(node).blockingFirst().getResult();

        DataStoreFactory.getScenarioDataStore().put(ref, privacyPrecompileAddress);

        return privacyPrecompileAddress;
    }
}
