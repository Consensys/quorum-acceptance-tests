package com.quorum.gauge.services;

import com.quorum.gauge.common.QuorumNetworkProperty;
import com.quorum.gauge.ext.StringResponse;
import com.quorum.gauge.ext.privacyprecompile.PrivacyPrecompileTransactionResponse;
import io.reactivex.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.Request;

import java.util.Arrays;
import java.util.Collections;

/**
 * Exposes service methods for PMT-specific APIs or APIs with PMT-specific response types
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

    public Observable<PrivacyPrecompileTransactionResponse> getTransaction(QuorumNetworkProperty.Node node, String transactionHash) {
        Request<?, PrivacyPrecompileTransactionResponse> request = new Request<Object, PrivacyPrecompileTransactionResponse>(
            "eth_getTransactionByHash",
            Arrays.asList(transactionHash),
            connectionFactory().getWeb3jService(node),
            PrivacyPrecompileTransactionResponse.class
        );
        return request.flowable().toObservable();
    }

    public Observable<PrivacyPrecompileTransactionResponse> getPrivateTransaction(QuorumNetworkProperty.Node node, String transactionHash) {
        Request<?, PrivacyPrecompileTransactionResponse> request = new Request<Object, PrivacyPrecompileTransactionResponse>(
            "eth_getPrivateTransactionByHash",
            Arrays.asList(transactionHash),
            connectionFactory().getWeb3jService(node),
            PrivacyPrecompileTransactionResponse.class
        );
        return request.flowable().toObservable();
    }





}
