package com.quorum.gauge;

import com.quorum.gauge.common.QuorumNode;
import com.quorum.gauge.core.AbstractSpecImplementation;
import com.quorum.gauge.ext.FillTx.PrivateFillTransaction;
import com.quorum.gauge.sol.SimpleStorage;
import com.thoughtworks.gauge.Step;
import com.thoughtworks.gauge.datastore.DataStoreFactory;
import io.reactivex.functions.Predicate;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Service
public class FillTransaction extends AbstractSpecImplementation {
    @Step("Deploy Simple Storage contract using fillTransaction api with an initial value of <initValue> called from <from> private for <to>. Name this contract as <contractName>")
    public void sendFillTransaction(int initValue, QuorumNode from, QuorumNode to, String contractName) {
        com.quorum.gauge.ext.FillTx.FillTransaction filledTx = rawContractService.fillTransaction(from, to, initValue).blockingFirst().getResponseObject();
        assertThat(!filledTx.getRaw().isEmpty());

        PrivateFillTransaction tx = filledTx.getPrivateTransaction(accountService.getDefaultAccountAddress(from).blockingFirst(), Arrays.asList(privacyService.id(to)));

        com.quorum.gauge.ext.FillTx.FillTransaction fTx = rawContractService.signTransaction(from, tx).blockingFirst().getResponseObject();
        assertThat(!fTx.getRaw().isEmpty());

        String txHash = rawContractService.sendRawPrivateTransaction(from, fTx.getRaw(), to).blockingFirst().getTransactionHash();
        assertThat(!txHash.isEmpty());

        // if the transaction is accepted, its receipt must be available in any node
        Predicate<? super EthGetTransactionReceipt> isReceiptPresent
            = ethGetTransactionReceipt -> ethGetTransactionReceipt.getTransactionReceipt().isPresent();

        Optional<TransactionReceipt> receipt = transactionService.getTransactionReceipt(QuorumNode.Node1, txHash)
            .repeatWhen(completed -> completed.delay(2, TimeUnit.SECONDS))
            .takeUntil(isReceiptPresent)
            .timeout(10, TimeUnit.SECONDS)
            .blockingLast().getTransactionReceipt();

        assertThat(receipt.isPresent()).isTrue();
        assertThat(receipt.get().getBlockNumber()).isNotEqualTo(currentBlockNumber());
        assertThat(!receipt.get().getContractAddress().isEmpty());

        Contract c = loadSimpleStorageContract(receipt.get().getContractAddress(), receipt.get());
        DataStoreFactory.getSpecDataStore().put(contractName, c);

    }

    private SimpleStorage loadSimpleStorageContract(String contractAddress, TransactionReceipt transactionReceipt) {
        SimpleStorage c = SimpleStorage.load(
            contractAddress,
            null,
            (TransactionManager) null,
            null,
            null
        );
        c.setTransactionReceipt(transactionReceipt);

        return c;
    }

}
