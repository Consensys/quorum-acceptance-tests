package com.quorum.gauge.services;

import com.quorum.gauge.common.QuorumNode;
import com.quorum.gauge.ext.EthGetQuorumPayload;
import com.quorum.gauge.ext.EthSignTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.quorum.Quorum;
import org.web3j.quorum.methods.request.PrivateTransaction;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Map;

@Service
public class TransactionService extends AbstractService {
    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    @Autowired
    AccountService accountService;

    @Autowired
    PrivacyService privacyService;

    public Observable<EthGetTransactionReceipt> getTransactionReceipt(QuorumNode node, String transactionHash) {
        Quorum client = connectionFactory.getConnection(node);
        return client.ethGetTransactionReceipt(transactionHash).observable();
    }

    public Observable<EthSendTransaction> sendPublicTransaction(int value, QuorumNode from, QuorumNode to) {
        Web3j client = connectionFactory.getWeb3jConnection(from);
        return Observable.zip(
                accountService.getDefaultAccountAddress(from).subscribeOn(Schedulers.io()),
                accountService.getDefaultAccountAddress(to).subscribeOn(Schedulers.io()),
                (fromAddress, toAddress) -> Arrays.asList(fromAddress, toAddress))
                .flatMap(l -> {
                    String fromAddress = l.get(0);
                    String toAddress = l.get(1);
                    return client.ethGetTransactionCount(fromAddress, DefaultBlockParameterName.LATEST)
                            .observable()
                            .flatMap(ethGetTransactionCount -> {
                                Transaction tx = Transaction.createEtherTransaction(fromAddress,
                                        ethGetTransactionCount.getTransactionCount(),
                                        BigInteger.valueOf(0),
                                        DEFAULT_GAS_LIMIT,
                                        toAddress,
                                        BigInteger.valueOf(value));
                                return client.ethSendTransaction(tx).observable();
                            });
                });
    }

    public Observable<EthSendTransaction> sendSignedPublicTransaction(int value, QuorumNode from, QuorumNode to) {
        Web3j client = connectionFactory.getWeb3jConnection(from);
        return Observable.zip(
                accountService.getDefaultAccountAddress(from).subscribeOn(Schedulers.io()),
                accountService.getDefaultAccountAddress(to).subscribeOn(Schedulers.io()),
                (fromAddress, toAddress) -> Transaction.createEtherTransaction(fromAddress,
                        null,
                        null,
                        DEFAULT_GAS_LIMIT,
                        toAddress,
                        BigInteger.valueOf(value)))
                .flatMap(tx -> {
                    Request<?, EthSignTransaction> request = new Request<>(
                            "eth_signTransaction",
                            Arrays.asList(tx),
                            connectionFactory.getWeb3jService(from),
                            EthSignTransaction.class
                    );
                    return request.observable();
                })
                .flatMap(ethSignTransaction -> {
                    Map<String, Object> response = ethSignTransaction.getResult();
                    logger.debug("{}", response);
                    String rawHexString = (String) response.get("raw");
                    return client.ethSendRawTransaction(rawHexString).observable();
                });
    }

    public Observable<EthSendTransaction> sendPrivateTransaction(int value, QuorumNode from, QuorumNode to) {
        Quorum client = connectionFactory.getConnection(from);
        return Observable.zip(
                accountService.getDefaultAccountAddress(from).subscribeOn(Schedulers.io()),
                accountService.getDefaultAccountAddress(to).subscribeOn(Schedulers.io()),
                (fromAddress, toAddress) -> new PrivateTransaction(
                        fromAddress,
                        null,
                        DEFAULT_GAS_LIMIT,
                        toAddress,
                        BigInteger.valueOf(value),
                        null,
                        null,
                        Arrays.asList(privacyService.id(to))
                ))
                .flatMap(tx -> client.ethSendTransaction(tx).observable());
    }

    public Observable<EthSendTransaction> sendSignedPrivateTransaction(int value, QuorumNode from, QuorumNode to) {
        Web3j client = connectionFactory.getWeb3jConnection(from);
        return Observable.zip(
                accountService.getDefaultAccountAddress(from).subscribeOn(Schedulers.io()),
                accountService.getDefaultAccountAddress(to).subscribeOn(Schedulers.io()),
                (fromAddress, toAddress) -> new PrivateTransaction(
                        fromAddress,
                        null,
                        DEFAULT_GAS_LIMIT,
                        toAddress,
                        BigInteger.valueOf(value),
                        null,
                        null,
                        Arrays.asList(privacyService.id(to))
                ))
                .flatMap(tx -> {
                    Request<?, EthSignTransaction> request = new Request<>(
                            "eth_signTransaction",
                            Arrays.asList(tx),
                            connectionFactory.getWeb3jService(from),
                            EthSignTransaction.class
                    );
                    return request.observable();
                })
                .flatMap(ethSignTransaction -> {
                    Map<String, Object> response = ethSignTransaction.getResult();
                    logger.debug("{}", response);
                    String rawHexString = (String) response.get("raw");
                    return client.ethSendRawTransaction(rawHexString).observable();
                });
    }

    // Invoking eth_getQuorumPayload
    public Observable<EthGetQuorumPayload> getPrivateTransactionPayload(QuorumNode node, String transactionHash) {
        Web3j client = connectionFactory.getWeb3jConnection(node);
        return client.ethGetTransactionByHash(transactionHash).observable()
                .flatMap(ethTransaction -> Observable.just(ethTransaction.getTransaction().orElseThrow(() -> new RuntimeException("no such transaction")).getInput()))
                .flatMap(payloadHash -> {
                    Request<?, EthGetQuorumPayload> request = new Request<>(
                            "eth_getQuorumPayload",
                            Arrays.asList(payloadHash),
                            connectionFactory.getWeb3jService(node),
                            EthGetQuorumPayload.class
                    );
                    return request.observable();
                });
    }
}
