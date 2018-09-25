package com.quorum.gauge;

import com.quorum.gauge.common.QuorumNode;
import com.quorum.gauge.services.AccountService;
import com.quorum.gauge.services.TransactionService;
import com.thoughtworks.gauge.Step;
import com.thoughtworks.gauge.datastore.DataStoreFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.math.BigInteger;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Service
public class ValueTransferPublicTransaction {

    @Autowired
    TransactionService transactionService;

    @Autowired
    AccountService accountService;

    @Step("Send <value> ETH from a default account in <from> to a default account in <to> in a public transaction.")
    public void sendTransaction(int value, QuorumNode from, QuorumNode to) {
        // backup the current balance
        String txHash = Observable.zip(
                accountService.getDefaultAccountBalance(from).subscribeOn(Schedulers.io()),
                accountService.getDefaultAccountBalance(to).subscribeOn(Schedulers.io()),
                (fromBalance, toBalance) -> {
                    DataStoreFactory.getScenarioDataStore().put(String.format("%s_balance", from), fromBalance.getBalance());
                    DataStoreFactory.getScenarioDataStore().put(String.format("%s_balance", to), toBalance.getBalance());
                    return true;
                })
                .flatMap( r -> transactionService.sendPublicTransaction(value, from, to))
                .toBlocking().first().getTransactionHash();

        DataStoreFactory.getScenarioDataStore().put("tx_hash", txHash);
    }

    @Step("Transaction is accepted in the blockchain.")
    public void verifyTransactionHash() {
        String txHash = (String) DataStoreFactory.getScenarioDataStore().get("tx_hash");
        // if the transaction is accepted, its receipt must be available in any node
        Optional<TransactionReceipt> receipt = transactionService.getTransactionReceiptObservable(QuorumNode.Node1, txHash)
                .repeatWhen(completed -> completed.delay(2, TimeUnit.SECONDS))
                .takeUntil(ethGetTransactionReceipt -> ethGetTransactionReceipt.getTransactionReceipt().isPresent())
                .timeout(10, TimeUnit.SECONDS)
                .toBlocking().last().getTransactionReceipt();

        assertThat(receipt.isPresent()).isTrue();
        assertThat(receipt.get().getBlockNumber()).isNotEqualTo(BigInteger.valueOf(0));
    }

    @Step("In <node>, the default account's balance is now less than its previous balance.")
    public void verifyLesserBalance(QuorumNode node) {
        BigInteger prevBalance = (BigInteger) DataStoreFactory.getScenarioDataStore().get(String.format("%s_balance", node));
        BigInteger actualBalance = accountService.getDefaultAccountBalance(node).toBlocking().first().getBalance();

        assertThat(actualBalance).isLessThan(prevBalance);
    }

    @Step("In <node>, the default account's balance is now greater than its previous balance.")
    public void verifyMoreBalance(QuorumNode node) {
        BigInteger prevBalance = (BigInteger) DataStoreFactory.getScenarioDataStore().get(String.format("%s_balance", node));
        BigInteger actualBalance = accountService.getDefaultAccountBalance(node).toBlocking().first().getBalance();

        assertThat(actualBalance).isGreaterThan(prevBalance);
    }

    @Step("Send <value> ETH from a default account in <from> to a default account in <to> in a signed public transaction.")
    public void sendSignedTransaction(int value, QuorumNode from, QuorumNode to) {
        // backup the current balance
        String txHash = Observable.zip(
                accountService.getDefaultAccountBalance(from).subscribeOn(Schedulers.io()),
                accountService.getDefaultAccountBalance(to).subscribeOn(Schedulers.io()),
                (fromBalance, toBalance) -> {
                    DataStoreFactory.getScenarioDataStore().put(String.format("%s_balance", from), fromBalance.getBalance());
                    DataStoreFactory.getScenarioDataStore().put(String.format("%s_balance", to), toBalance.getBalance());
                    return true;
                })
                .flatMap( r -> transactionService.sendSignedPublicTransaction(value, from, to))
                .toBlocking().first().getTransactionHash();

        DataStoreFactory.getScenarioDataStore().put("tx_hash", txHash);
    }
}
