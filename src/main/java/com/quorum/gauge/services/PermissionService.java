package com.quorum.gauge.services;

import com.quorum.gauge.common.QuorumNode;
import com.quorum.gauge.ext.NodeInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.Request;
import org.web3j.quorum.Quorum;
import org.web3j.quorum.methods.request.PrivateTransaction;
import org.web3j.quorum.methods.response.permissioning.*;
import io.reactivex.Observable;

import java.math.BigInteger;

@Service
public class PermissionService extends AbstractService {

    @Autowired
    AccountService accountService;

    public String NodeInfo(QuorumNode node) {

        Request<?, NodeInfo> nodeInfoRequest = new Request<>(
                "admin_nodeInfo",
                null,
                connectionFactory().getWeb3jService(node),
                NodeInfo.class
        );
        NodeInfo nodeInfo = nodeInfoRequest.flowable().toObservable().blockingFirst();
        return nodeInfo.getEnode();
    }

    public Observable<PermissionAccountList> getPermissionAccountList(QuorumNode node) {
        Quorum client = connectionFactory().getConnection(node);
        return client.quorumPermissionGetAccountList().flowable().toObservable();
    }

    public Observable<PermissionNodeList> getPermissionNodeList(QuorumNode node) {
        Quorum client = connectionFactory().getConnection(node);
        return client.quorumPermissionGetNodeList().flowable().toObservable();
    }

    public Observable<PermissionRoleList> getPermissionRoleList(QuorumNode node) {
        Quorum client = connectionFactory().getConnection(node);
        return client.quorumPermissionGetRoleList().flowable().toObservable();
    }

    public Observable<PermissionOrgList> getPermissionOrgList(QuorumNode node) {
        Quorum client = connectionFactory().getConnection(node);
        return client.quorumPermissionGetOrgList().flowable().toObservable();
    }


    public Observable<ExecStatusInfo> addOrg(QuorumNode node, String org, String enode, String address) {
        Quorum client = connectionFactory().getConnection(node);
        String fromAccount = accountService.getDefaultAccountAddress(node).blockingFirst();
        return client.quorumPermissionAddOrg(org, enode, address, new PrivateTransaction(fromAccount, null, DEFAULT_GAS_LIMIT, null, BigInteger.ZERO, null, null, null)).flowable().toObservable();

    }

    public Observable<ExecStatusInfo> assignAccountRole(QuorumNode node, String address, String org, String role) {
        Quorum client = connectionFactory().getConnection(node);
        String fromAccount = accountService.getDefaultAccountAddress(node).blockingFirst();
        return client.quorumPermissionAddAccountToOrg(address, org, role, new PrivateTransaction(fromAccount, null, DEFAULT_GAS_LIMIT, null, BigInteger.ZERO, null, null, null)).flowable().toObservable();

    }

    public Observable<ExecStatusInfo> updateOrgStatus(QuorumNode node, String org, int status) {
        Quorum client = connectionFactory().getConnection(node);
        String fromAccount = accountService.getDefaultAccountAddress(node).blockingFirst();
        return client.quorumPermissionUpdateOrgStatus(org, status, new PrivateTransaction(fromAccount, null, DEFAULT_GAS_LIMIT, null, BigInteger.ZERO, null, null, null)).flowable().toObservable();
    }

    public Observable<ExecStatusInfo> approveOrgStatus(QuorumNode node, String org, int status) {
        Quorum client = connectionFactory().getConnection(node);
        String fromAccount = accountService.getDefaultAccountAddress(node).blockingFirst();
        return client.quorumPermissionApproveOrgStatus(org, status, new PrivateTransaction(fromAccount, null, DEFAULT_GAS_LIMIT, null, BigInteger.ZERO, null, null, null)).flowable().toObservable();
    }

    public Observable<ExecStatusInfo> addNodeToOrg(QuorumNode node, String org, String enode) {
        Quorum client = connectionFactory().getConnection(node);
        String fromAccount = accountService.getDefaultAccountAddress(node).blockingFirst();
        return client.quorumPermissionAddNode(org, enode, new PrivateTransaction(fromAccount, null, DEFAULT_GAS_LIMIT, null, BigInteger.ZERO, null, null, null)).flowable().toObservable();

    }

    public Observable<ExecStatusInfo> updateNode(QuorumNode node, String org, String enode, int status) {
        Quorum client = connectionFactory().getConnection(node);
        String fromAccount = accountService.getDefaultAccountAddress(node).blockingFirst();
        return client.quorumPermissionUpdateNodeStatus(org, enode, status, new PrivateTransaction(fromAccount, null, DEFAULT_GAS_LIMIT, null, BigInteger.ZERO, null, null, null)).flowable().toObservable();

    }

    public Observable<ExecStatusInfo> addNewRole(QuorumNode node, String org, String role, int access, boolean isVoter, boolean isAdmin) {
        Quorum client = connectionFactory().getConnection(node);
        String fromAccount = accountService.getDefaultAccountAddress(node).blockingFirst();
        return client.quorumPermissionAddNewRole(org, role, access, isVoter, isAdmin, new PrivateTransaction(fromAccount, null, DEFAULT_GAS_LIMIT, null, BigInteger.ZERO, null, null, null)).flowable().toObservable();

    }

    public Observable<ExecStatusInfo> removeRole(QuorumNode node, String org, String role) {
        Quorum client = connectionFactory().getConnection(node);
        String fromAccount = accountService.getDefaultAccountAddress(node).blockingFirst();
        return client.quorumPermissionRemoveRole(org, role, new PrivateTransaction(fromAccount, null, DEFAULT_GAS_LIMIT, null, BigInteger.ZERO, null, null, null)).flowable().toObservable();

    }

    public Observable<ExecStatusInfo> addSubOrg(QuorumNode node, String porg, String sorg, String enode) {
        Quorum client = connectionFactory().getConnection(node);
        String fromAccount = accountService.getDefaultAccountAddress(node).blockingFirst();
        return client.quorumPermissionAddSubOrg(porg, sorg, enode, new PrivateTransaction(fromAccount, null, DEFAULT_GAS_LIMIT, null, BigInteger.ZERO, null, null, null)).flowable().toObservable();

    }

    public Observable<ExecStatusInfo> approveOrg(QuorumNode node, String org, String enode, String address) {
        Quorum client = connectionFactory().getConnection(node);
        String fromAccount = accountService.getDefaultAccountAddress(node).blockingFirst();
        return client.quorumPermissionApproveOrg(org, enode, address, new PrivateTransaction(fromAccount, null, DEFAULT_GAS_LIMIT, null, BigInteger.ZERO, null, null, null)).flowable().toObservable();

    }
}
