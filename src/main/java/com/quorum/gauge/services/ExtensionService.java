package com.quorum.gauge.services;


import com.quorum.gauge.common.QuorumNetworkProperty;
import com.quorum.gauge.ext.contractextension.*;
import io.reactivex.Observable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.Request;
import org.web3j.quorum.PrivacyFlag;
import org.web3j.quorum.methods.request.PrivateTransaction;

import java.math.BigInteger;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;

@Service
public class ExtensionService extends AbstractService {

    private final AccountService accountService;

    @Autowired
    PrivacyService privacyService;

    @Autowired
    public ExtensionService(final PrivacyService privacyService, final AccountService accountService) {
        this.accountService = Objects.requireNonNull(accountService);
    }

    public Observable<QuorumExtendContract> initiateContractExtension(QuorumNetworkProperty.Node sourceNode, String sourceEthAccount, String sourceParty, String addressToExtend, QuorumNetworkProperty.Node targetNode, String targetEthAccount, String targetParty, PrivacyFlag privacyFlag) {
        return Observable.zip(accountService.getAccountAddress(targetNode, targetEthAccount), accountService.getAccountAddress(sourceNode, sourceEthAccount), (recipientAccount, senderAccount) -> {
            String newPartyPrivacyAddress = privacyService.id(targetParty);
            String sourcePartyPrivacyAddress = privacyService.id(sourceParty);
            PrivateTransaction transactionArgs = new PrivateTransaction(
                senderAccount, null, null, null, BigInteger.ZERO, null,
                sourcePartyPrivacyAddress, List.of(newPartyPrivacyAddress), privacyFlag
            );
            return Stream.of(addressToExtend, newPartyPrivacyAddress, recipientAccount, transactionArgs).collect(Collectors.toList());
        }).flatMap(arg -> {
            Request<?, QuorumExtendContract> request = new Request<>(
                "quorumExtension_extendContract",
                arg,
                connectionFactory().getWeb3jService(sourceNode),
                QuorumExtendContract.class
            );
            return request.flowable().toObservable();
        });
    }

    public Observable<QuorumExtendContract> initiateContractExtension(QuorumNetworkProperty.Node sourceNode, String addressToExtend, QuorumNetworkProperty.Node targetNode, String targetAddress, PrivacyFlag privacyFlag) {
        return Observable.zip(Observable.just(targetAddress), accountService.getDefaultAccountAddress(sourceNode), (recipientAccount, senderAccount) -> {
            String newPartyPrivacyAddress = privacyService.id(targetNode);
            String sourcePartyPrivacyAddress = privacyService.id(sourceNode);
            PrivateTransaction transactionArgs = new PrivateTransaction(
                senderAccount, null, null, null, BigInteger.ZERO, null,
                sourcePartyPrivacyAddress, List.of(newPartyPrivacyAddress), privacyFlag
            );
            return Stream.of(addressToExtend, newPartyPrivacyAddress, recipientAccount, transactionArgs).collect(Collectors.toList());
        }).flatMap(arg -> {
            Request<?, QuorumExtendContract> request = new Request<>(
                "quorumExtension_extendContract",
                arg,
                connectionFactory().getWeb3jService(sourceNode),
                QuorumExtendContract.class
            );
            return request.flowable().toObservable();
        });
    }

    public Observable<QuorumExtendContract> initiateContractExtension(final QuorumNetworkProperty.Node node,
                                                                      final String addressToExtend,
                                                                      final QuorumNetworkProperty.Node newParty,
                                                                      final PrivacyFlag privacyFlag) {


        String recipientKey = accountService.getDefaultAccountAddress(newParty).blockingFirst();

        final List<String> privateFor = Stream.of(newParty).map(n -> privacyService.id(n)).collect(Collectors.toList());

        final PrivateTransaction transactionArgs = new PrivateTransaction(
            accountService.getDefaultAccountAddress(node).blockingFirst(),
            null, null, null, BigInteger.ZERO, null, privacyService.id(node), privateFor, privacyFlag
        );

        final List<Object> arguments = Stream.of(
            addressToExtend,
            privacyService.id(newParty),
            recipientKey,
            transactionArgs
        ).collect(Collectors.toList());

        final Request<?, QuorumExtendContract> request = new Request<>(
            "quorumExtension_extendContract",
            arguments,
            connectionFactory().getWeb3jService(node),
            QuorumExtendContract.class
        );

        return request.flowable().toObservable();
    }
    
    public Observable<QuorumVoteOnContract> acceptExtension(QuorumNetworkProperty.Node node, boolean vote, String ethAccount, String privateFromKey, String address, List<String> privateForKeys, PrivacyFlag privacyFlag) {
        final PrivateTransaction transactionArgs = new PrivateTransaction(
            accountService.getAccountAddress(node, ethAccount).blockingFirst(),
            null, BigInteger.valueOf(4700000), null, BigInteger.ZERO, null, privateFromKey, privateForKeys, privacyFlag
        );

        return new Request<>(
            "quorumExtension_approveExtension",
            Stream.of(address, vote, transactionArgs).collect(Collectors.toList()),
            connectionFactory().getWeb3jService(node),
            QuorumVoteOnContract.class
        ).flowable().toObservable();
    }

    public Observable<QuorumVoteOnContract> acceptExtension(final QuorumNetworkProperty.Node node,
                                                            final boolean vote,
                                                            final String address,
                                                            final Set<QuorumNetworkProperty.Node> allNodes,
                                                            final PrivacyFlag privacyFlag) {

        final List<String> privateFor = allNodes
            .stream()
            .filter(n -> !n.equals(node))
            .map(n -> privacyService.id(n))
            .collect(Collectors.toList());
        return acceptExtension(node, vote, null, privacyService.id(node), address, privateFor, privacyFlag);
    }

    public Observable<QuorumUpdateParties> updateParties(final QuorumNetworkProperty.Node initiator,
                                                         final String address,
                                                         final Set<QuorumNetworkProperty.Node> allNodes,
                                                         final PrivacyFlag privacyFlag) {

        final List<String> privateFor = allNodes
            .stream()
            .filter(n -> !n.equals(initiator))
            .map(n -> privacyService.id(n))
            .collect(Collectors.toList());


        final PrivateTransaction transactionArgs = new PrivateTransaction(
            accountService.getDefaultAccountAddress(initiator).blockingFirst(),
            null, null, null, BigInteger.ZERO, null, null, privateFor, privacyFlag
        );

        return new Request<>(
            "quorumExtension_updateParties",
            Stream.of(address, transactionArgs).collect(Collectors.toList()),
            connectionFactory().getWeb3jService(initiator),
            QuorumUpdateParties.class
        ).flowable().toObservable();

    }

    public Observable<QuorumActiveExtensionContracts> getExtensionContracts(final QuorumNetworkProperty.Node node) {

        return new Request<>(
            "quorumExtension_activeExtensionContracts",
            emptyList(),
            connectionFactory().getWeb3jService(node),
            QuorumActiveExtensionContracts.class
        ).flowable().toObservable();

    }

    public Observable<QuorumCancel> cancelExtension(final QuorumNetworkProperty.Node node,
                                                    final String address,
                                                    final Set<QuorumNetworkProperty.Node> allNodes,
                                                    final PrivacyFlag privacyFlag) {
        final List<String> privateFor = allNodes
            .stream()
            .filter(n -> !n.equals(node))
            .map(n -> privacyService.id(n))
            .collect(Collectors.toList());

        final PrivateTransaction transactionArgs = new PrivateTransaction(
            accountService.getDefaultAccountAddress(node).blockingFirst(),
            null, null, null, BigInteger.ZERO, null, privacyService.id(node), privateFor, privacyFlag
        );

        return new Request<>(
            "quorumExtension_cancelExtension",
            Stream.of(address, transactionArgs).collect(Collectors.toList()),
            connectionFactory().getWeb3jService(node),
            QuorumCancel.class
        ).flowable().toObservable();

    }

    public String getExtensionStatus(final QuorumNetworkProperty.Node node,
                                                    final String address) {

        final List<Object> arguments = Stream.of(
            address
        ).collect(Collectors.toList());

        Request<?, QuorumGetExtensionStatus> extensionInfo = new Request<>(
            "quorumExtension_getExtensionStatus",
            arguments,
            connectionFactory().getWeb3jService(node),
            QuorumGetExtensionStatus.class
        );
        return extensionInfo.flowable().toObservable().blockingFirst().getResult();

    }

    public Observable<QuorumGenerateExtensionApprovalUuid> generateExtensionApprovalUuid(final QuorumNetworkProperty.Node node, 
                                                                                         final String approverAddress,
                                                                                         final String address, 
                                                                                         final PrivacyFlag privacyFlag) {

        final PrivateTransaction transactionArgs = new PrivateTransaction(
            accountService.getDefaultAccountAddress(node).blockingFirst(),
            null, BigInteger.valueOf(4700000), null, BigInteger.ZERO, null, privacyService.id(node), List.of(privacyService.id(node)), privacyFlag);

        return new Request<>(
            "quorumExtension_generateExtensionApprovalUuid",
            Stream.of(address, approverAddress, transactionArgs).collect(Collectors.toList()),
            connectionFactory().getWeb3jService(node),
            QuorumGenerateExtensionApprovalUuid.class
        ).flowable().toObservable();

    }

}
