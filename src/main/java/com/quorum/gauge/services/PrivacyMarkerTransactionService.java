package com.quorum.gauge.services;

import com.quorum.gauge.common.QuorumNetworkProperty;
import com.quorum.gauge.common.QuorumNode;
import com.quorum.gauge.ext.StringResponse;
import com.quorum.gauge.ext.privacyprecompile.PMTEnabledTransactionReceiptResponse;
import io.reactivex.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.response.EthTransaction;

import java.util.Arrays;
import java.util.Collections;

/**
 * Service methods for PMT-specific APIs or APIs with PMT-specific response types
 */
@Service
public class PrivacyMarkerTransactionService extends AbstractService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrivacyMarkerTransactionService.class);

    public Observable<StringResponse> getPrivacyPrecompileAddress(QuorumNetworkProperty.Node node) {
        return new Request<>(
            "eth_getPrivacyPrecompileAddress",
            Collections.emptyList(),
            connectionFactory().getWeb3jService(node),
            StringResponse.class
        ).flowable().toObservable();
    }

    // Returns the result of eth_getTransactionByHash without applying any PMT correction (i.e. without returning the internal private tx if the tx is a PMT)
    public Observable<EthTransaction> getTransaction(QuorumNetworkProperty.Node node, String transactionHash) {
        Request<?, EthTransaction> request = new Request<Object, EthTransaction>(
            "eth_getTransactionByHash",
            Arrays.asList(transactionHash),
            connectionFactory().getWeb3jService(node),
            EthTransaction.class
        );
        return request.flowable().toObservable();
    }

    public Observable<EthTransaction> getPrivateTransaction(QuorumNetworkProperty.Node node, String transactionHash) {
        Request<?, EthTransaction> request = new Request<Object, EthTransaction>(
            "eth_getPrivateTransactionByHash",
            Arrays.asList(transactionHash),
            connectionFactory().getWeb3jService(node),
            EthTransaction.class
        );
        return request.flowable().toObservable();
    }

    // Invoking eth_getPrivateTransactionByHash
    public Observable<EthTransaction> getPrivateTransaction(QuorumNode node, String transactionHash) {
        Request<?, EthTransaction> request = new Request<Object, EthTransaction>(
            "eth_getPrivateTransactionByHash",
            Arrays.asList(transactionHash),
            connectionFactory().getWeb3jService(node),
            EthTransaction.class
        );
        return request.flowable().toObservable();
    }

    public Observable<PMTEnabledTransactionReceiptResponse> getTransactionReceipt(QuorumNetworkProperty.Node node, String transactionHash) {
        Request<?, PMTEnabledTransactionReceiptResponse> request = new Request<Object, PMTEnabledTransactionReceiptResponse>(
            "eth_getTransactionReceipt",
            Arrays.asList(transactionHash),
            connectionFactory().getWeb3jService(node),
            PMTEnabledTransactionReceiptResponse.class
        );
        return request.flowable().toObservable();
    }

    public Observable<PMTEnabledTransactionReceiptResponse> getPrivateTransactionReceipt(QuorumNetworkProperty.Node node, String transactionHash) {
        Request<?, PMTEnabledTransactionReceiptResponse> request = new Request<Object, PMTEnabledTransactionReceiptResponse>(
            "eth_getPrivateTransactionReceipt",
            Arrays.asList(transactionHash),
            connectionFactory().getWeb3jService(node),
            PMTEnabledTransactionReceiptResponse.class
        );
        return request.flowable().toObservable();
    }



}
