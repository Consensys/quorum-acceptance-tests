package com.quorum.gauge.services;

import com.quorum.gauge.common.QuorumNetworkProperty;
import com.quorum.gauge.ext.JsonResponse;
import io.reactivex.Observable;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.Request;

@Service
public class AdminService extends AbstractService {

    public Observable<JsonResponse> nodeInfo(QuorumNetworkProperty.Node node) {

        Request<?, JsonResponse> request = new Request<>(
            "admin_nodeInfo",
            null,
            connectionFactory().getWeb3jService(node), JsonResponse.class
        );

        return request.flowable().toObservable();
    }

    public Observable<JsonResponse> qnodeInfo(QuorumNetworkProperty.Node node) {

        Request<?, JsonResponse> request = new Request<>(
            "admin_qnodeInfo",
            null,
            connectionFactory().getWeb3jService(node), JsonResponse.class
        );

        return request.flowable().toObservable();
    }

}
