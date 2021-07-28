package com.quorum.gauge.services;

import com.quorum.gauge.common.QuorumNetworkProperty;
import com.quorum.gauge.ext.StringResponse;
import io.reactivex.Observable;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.Request;

import java.util.Collections;

@Service
public class PrecompiledContractService extends AbstractService {

    public Observable<StringResponse> getPrivacyPrecompileAddress(QuorumNetworkProperty.Node node) {
        Request<?, StringResponse> request = new Request<>(
            "eth_getPrivacyPrecompileAddress",
            Collections.emptyList(),
            connectionFactory().getWeb3jService(node),
            StringResponse.class
        );
        return request.flowable().toObservable();
    }

}
