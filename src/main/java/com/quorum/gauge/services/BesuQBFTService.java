package com.quorum.gauge.services;

import com.quorum.gauge.common.QuorumNetworkProperty;
import com.quorum.gauge.ext.IstanbulNodeAddress;
import com.quorum.gauge.ext.IstanbulPropose;
import com.quorum.gauge.ext.ListIstanbulNodeAddress;
import io.reactivex.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.Request;

import java.util.Arrays;

@Service
public class BesuQBFTService extends AbstractService {
    private static final Logger logger = LoggerFactory.getLogger(BesuQBFTService.class);

    /**
     * propose proposes the given node to be added as a validator if vote is true and remove as a validator if vote is false
     * @param node
     * @param proposedValidatorAddress
     * @param vote
     * @return
     */
    public Observable<IstanbulPropose> propose(final QuorumNetworkProperty.Node node, final String proposedValidatorAddress, boolean vote) {
        logger.debug("Node {} proposing {}", node, proposedValidatorAddress);

        return new Request<>(
            "qbft_proposeValidatorVote",
            Arrays.asList(proposedValidatorAddress, vote),
            connectionFactory().getWeb3jService(node),
            IstanbulPropose.class
        ).flowable().toObservable();
    }

    /**
     * nodeAddress returns the QBFT validator address
     * @param node
     * @return
     */
    public Observable<IstanbulNodeAddress> nodeAddress(final QuorumNetworkProperty.Node node) {
        logger.debug("node address of node {}", node);
        return new Request<>(
            "eth_coinbase",
            Arrays.asList(),
            connectionFactory().getWeb3jService(node),
            IstanbulNodeAddress.class
        ).flowable().toObservable();
    }

    public Observable<ListIstanbulNodeAddress> getValidators(QuorumNetworkProperty.Node node) {
        logger.debug("qbft validators from node {}", node);
        return new Request<>(
            "qbft_getValidatorsByBlockNumber",
            Arrays.asList("latest"),
            connectionFactory().getWeb3jService(node),
            ListIstanbulNodeAddress.class
        ).flowable().toObservable();
    }
}
