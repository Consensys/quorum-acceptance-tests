package com.quorum.gauge.services;

import com.quorum.gauge.common.PrivacyFlag;
import com.quorum.gauge.common.QuorumNode;
import com.quorum.gauge.ext.EnhancedClientTransactionManager;
import com.quorum.gauge.ext.contractextension.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.Request;
import org.web3j.quorum.methods.request.PrivateTransaction;
import rx.Observable;
import rx.observables.BlockingObservable;

import java.math.BigInteger;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

@Service
public class ExtensionService extends AbstractService {

    private final PrivacyService privacyService;

    private final AccountService accountService;

    @Autowired
    public ExtensionService(final PrivacyService privacyService, final AccountService accountService) {
        this.privacyService = Objects.requireNonNull(privacyService);
        this.accountService = Objects.requireNonNull(accountService);
    }

    public Observable<QuorumExtendContract> initiateContractExtension(final QuorumNode node,
                                                                      final String addressToExtend,
                                                                      final QuorumNode newParty,
                                                                      final List<QuorumNode> voters,
                                                                      final PrivacyFlag privacyFlag) {

        final List<String> voterDefaultAddresses = voters
            .stream()
            .map(accountService::getDefaultAccountAddress)
            .map(Observable::toBlocking)
            .map(BlockingObservable::first)
            .collect(Collectors.toList());

        final List<String> privateFor = Stream
            .concat(Stream.of(newParty), voters.stream())
            .filter(n -> !n.equals(node))
            .map(privacyService::id)
            .collect(Collectors.toList());

        final EnhancedClientTransactionManager.EnhancedPrivateTransaction transactionArgs = new EnhancedClientTransactionManager.EnhancedPrivateTransaction(
            accountService.getDefaultAccountAddress(node).toBlocking().first(),
            null, null, null, BigInteger.ZERO, null, null, privateFor, singletonList(privacyFlag)
        );

        final List<Object> arguments = Stream.of(
            addressToExtend,
            privacyService.id(newParty),
            voterDefaultAddresses,
            transactionArgs
        ).collect(Collectors.toList());

        final Request<?, QuorumExtendContract> request = new Request<>(
            "quorumExtension_extendContract",
            arguments,
            connectionFactory().getWeb3jService(node),
            QuorumExtendContract.class
        );

        return request.observable();
    }

    public Observable<QuorumAccept> acceptExtension(final String address,
                                                    final QuorumNode node,
                                                    final Set<QuorumNode> allNodes,
                                                    final PrivacyFlag privacyFlag) {

        final List<String> privateFor = allNodes
            .stream()
            .filter(n -> !n.equals(node))
            .map(privacyService::id)
            .collect(Collectors.toList());

        final PrivateTransaction transactionArgs = new EnhancedClientTransactionManager.EnhancedPrivateTransaction(
            accountService.getDefaultAccountAddress(node).toBlocking().first(),
            null, BigInteger.valueOf(4700000), null, BigInteger.ZERO, null, null, privateFor, singletonList(privacyFlag)
        );

        return new Request<>(
            "quorumExtension_accept",
            Stream.of(address, transactionArgs).collect(Collectors.toList()),
            connectionFactory().getWeb3jService(node),
            QuorumAccept.class
        ).observable();

    }

    public Observable<QuorumVoteOnContract> voteOnExtension(final QuorumNode node,
                                                            final boolean vote,
                                                            final String address,
                                                            final Set<QuorumNode> allNodes,
                                                            final PrivacyFlag privacyFlag) {
        final List<String> privateFor = allNodes
            .stream()
            .filter(n -> !n.equals(node))
            .map(privacyService::id)
            .collect(Collectors.toList());

        final PrivateTransaction transactionArgs = new EnhancedClientTransactionManager.EnhancedPrivateTransaction(
            accountService.getDefaultAccountAddress(node).toBlocking().first(),
            null, BigInteger.valueOf(4700000), null, BigInteger.ZERO, null, null, privateFor, singletonList(privacyFlag)
        );

        return new Request<>(
            "quorumExtension_voteOnContract",
            Stream.of(address, vote, transactionArgs).collect(Collectors.toList()),
            connectionFactory().getWeb3jService(node),
            QuorumVoteOnContract.class
        ).observable();
    }

    public Observable<QuorumUpdateParties> updateParties(final QuorumNode initiator,
                                                         final String address,
                                                         final Set<QuorumNode> allNodes,
                                                         final PrivacyFlag privacyFlag) {

        final List<String> privateFor = allNodes
            .stream()
            .filter(n -> !n.equals(initiator))
            .map(privacyService::id)
            .collect(Collectors.toList());

        final PrivateTransaction transactionArgs = new EnhancedClientTransactionManager.EnhancedPrivateTransaction(
            accountService.getDefaultAccountAddress(initiator).toBlocking().first(),
            null, null, null, BigInteger.ZERO, null, null, privateFor, singletonList(privacyFlag)
        );

        return new Request<>(
            "quorumExtension_updateParties",
            Stream.of(address, transactionArgs).collect(Collectors.toList()),
            connectionFactory().getWeb3jService(initiator),
            QuorumUpdateParties.class
        ).observable();

    }

    public Observable<QuorumActiveExtensionContracts> getExtensionContracts(final QuorumNode node) {

        return new Request<>(
            "quorumExtension_activeExtensionContracts",
            emptyList(),
            connectionFactory().getWeb3jService(node),
            QuorumActiveExtensionContracts.class
        ).observable();

    }

    public Observable<QuorumCancel> cancelExtension(final QuorumNode node,
                                                    final String address,
                                                    final Set<QuorumNode> allNodes,
                                                    final PrivacyFlag privacyFlag) {
        final List<String> privateFor = allNodes
            .stream()
            .filter(n -> !n.equals(node))
            .map(privacyService::id)
            .collect(Collectors.toList());

        final PrivateTransaction transactionArgs = new EnhancedClientTransactionManager.EnhancedPrivateTransaction(
            accountService.getDefaultAccountAddress(node).toBlocking().first(),
            null, null, null, BigInteger.ZERO, null, null, privateFor, singletonList(privacyFlag)
        );

        return new Request<>(
            "quorumExtension_cancel",
            Stream.of(address, transactionArgs).collect(Collectors.toList()),
            connectionFactory().getWeb3jService(node),
            QuorumCancel.class
        ).observable();

    }

}
