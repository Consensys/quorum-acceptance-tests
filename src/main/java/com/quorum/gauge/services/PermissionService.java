/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.quorum.gauge.services;

import com.quorum.gauge.common.QuorumNetworkProperty;
import com.quorum.gauge.ext.BoolResponse;
import com.quorum.gauge.ext.NodeInfo;
import com.quorum.gauge.ext.StringResponse;
import io.reactivex.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.Request;
import org.web3j.quorum.Quorum;
import org.web3j.quorum.methods.request.PrivateTransaction;
import org.web3j.quorum.methods.response.permissioning.*;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;

@Service
public class PermissionService extends AbstractService {

    @Autowired
    AccountService accountService;

    private static final Logger logger = LoggerFactory.getLogger(PermissionService.class);

    public String NodeInfo(QuorumNetworkProperty.Node node) {

        Request<?, NodeInfo> nodeInfoRequest = new Request<>(
                "admin_nodeInfo",
                null,
                connectionFactory().getWeb3jService(node),
                NodeInfo.class
        );
        NodeInfo nodeInfo = nodeInfoRequest.flowable().toObservable().blockingFirst();
        return nodeInfo.getEnode();
    }

    public Observable<PermissionAccountList> getPermissionAccountList(QuorumNetworkProperty.Node node) {
        Quorum client = connectionFactory().getConnection(node);
        return client.quorumPermissionGetAccountList().flowable().toObservable();
    }

    public Observable<PermissionNodeList> getPermissionNodeList(QuorumNetworkProperty.Node node) {
        Quorum client = connectionFactory().getConnection(node);
        return client.quorumPermissionGetNodeList().flowable().toObservable();
    }

    public Observable<PermissionRoleList> getPermissionRoleList(QuorumNetworkProperty.Node node) {
        Quorum client = connectionFactory().getConnection(node);
        return client.quorumPermissionGetRoleList().flowable().toObservable();
    }

    public Observable<PermissionOrgList> getPermissionOrgList(QuorumNetworkProperty.Node node) {
        Quorum client = connectionFactory().getConnection(node);
        return client.quorumPermissionGetOrgList().flowable().toObservable();
    }


    public Observable<ExecStatusInfo> addOrg(QuorumNetworkProperty.Node node, String org, String enode, String address) {
        Quorum client = connectionFactory().getConnection(node);
        String fromAccount = accountService.getDefaultAccountAddress(node).blockingFirst();
        return client.quorumPermissionAddOrg(org, enode, address, new PrivateTransaction(fromAccount, null, DEFAULT_GAS_LIMIT, null, BigInteger.ZERO, null, null, null)).flowable().toObservable();

    }

    public Observable<ExecStatusInfo> addNode(QuorumNetworkProperty.Node node, String org, String enode) {
        Quorum client = connectionFactory().getConnection(node);
        String fromAccount = accountService.getDefaultAccountAddress(node).blockingFirst();
        return client.quorumPermissionAddNode(org, enode, new PrivateTransaction(fromAccount, null, DEFAULT_GAS_LIMIT, null, BigInteger.ZERO, null, null, null)).flowable().toObservable();
    }

    public Observable<ExecStatusInfo> assignAccountRole(QuorumNetworkProperty.Node node, String address, String org, String role) {
        Quorum client = connectionFactory().getConnection(node);
        String fromAccount = accountService.getDefaultAccountAddress(node).blockingFirst();
        return client.quorumPermissionAddAccountToOrg(address, org, role, new PrivateTransaction(fromAccount, null, DEFAULT_GAS_LIMIT, null, BigInteger.ZERO, null, null, null)).flowable().toObservable();

    }

    public Observable<ExecStatusInfo> assignAdminRole(QuorumNetworkProperty.Node node, String address, String org, String role) {
        Quorum client = connectionFactory().getConnection(node);
        String fromAccount = accountService.getDefaultAccountAddress(node).blockingFirst();
        return client.quorumPermissionAssignAdminRole(org, address, role, new PrivateTransaction(fromAccount, null, DEFAULT_GAS_LIMIT, null, BigInteger.ZERO, null, null, null)).flowable().toObservable();

    }

    public Observable<ExecStatusInfo> approveAdminRoleAssignment(QuorumNetworkProperty.Node node, String address, String org) {
        Quorum client = connectionFactory().getConnection(node);
        String fromAccount = accountService.getDefaultAccountAddress(node).blockingFirst();
        return client.quorumPermissionApproveAdminRole(org, address, new PrivateTransaction(fromAccount, null, DEFAULT_GAS_LIMIT, null, BigInteger.ZERO, null, null, null)).flowable().toObservable();

    }

    public Observable<ExecStatusInfo> updateOrgStatus(QuorumNetworkProperty.Node node, String org, int status) {
        Quorum client = connectionFactory().getConnection(node);
        String fromAccount = accountService.getDefaultAccountAddress(node).blockingFirst();
        return client.quorumPermissionUpdateOrgStatus(org, status, new PrivateTransaction(fromAccount, null, DEFAULT_GAS_LIMIT, null, BigInteger.ZERO, null, null, null)).flowable().toObservable();
    }

    public Observable<ExecStatusInfo> approveOrgStatus(QuorumNetworkProperty.Node node, String org, int status) {
        Quorum client = connectionFactory().getConnection(node);
        String fromAccount = accountService.getDefaultAccountAddress(node).blockingFirst();
        return client.quorumPermissionApproveOrgStatus(org, status, new PrivateTransaction(fromAccount, null, DEFAULT_GAS_LIMIT, null, BigInteger.ZERO, null, null, null)).flowable().toObservable();
    }

    public Observable<ExecStatusInfo> addNodeToOrg(QuorumNetworkProperty.Node node, String org, String enode) {
        Quorum client = connectionFactory().getConnection(node);
        String fromAccount = accountService.getDefaultAccountAddress(node).blockingFirst();
        return client.quorumPermissionAddNode(org, enode, new PrivateTransaction(fromAccount, null, DEFAULT_GAS_LIMIT, null, BigInteger.ZERO, null, null, null)).flowable().toObservable();

    }

    public Observable<ExecStatusInfo> addAccountToOrg(QuorumNetworkProperty.Node node, String account, String org, String roleId) {
        Quorum client = connectionFactory().getConnection(node);
        String fromAccount = accountService.getDefaultAccountAddress(node).blockingFirst();
        return client.quorumPermissionAddAccountToOrg(account, org, roleId, new PrivateTransaction(fromAccount, null, DEFAULT_GAS_LIMIT, null, BigInteger.ZERO, null, null, null)).flowable().toObservable();
    }

    public Observable<ExecStatusInfo> changeAccountRole(QuorumNetworkProperty.Node node, String account, String org, String roleId) {
        Quorum client = connectionFactory().getConnection(node);
        String fromAccount = accountService.getDefaultAccountAddress(node).blockingFirst();
        return client.quorumPermissionChangeAccountRole(account, org, roleId, new PrivateTransaction(fromAccount, null, DEFAULT_GAS_LIMIT, null, BigInteger.ZERO, null, null, null)).flowable().toObservable();
    }

    public Observable<ExecStatusInfo> updateAccountStatus(QuorumNetworkProperty.Node node, String org, String account, int status) {
        Quorum client = connectionFactory().getConnection(node);
        String fromAccount = accountService.getDefaultAccountAddress(node).blockingFirst();
        return client.quorumPermissionUpdateAccountStatus(org, account, status, new PrivateTransaction(fromAccount, null, DEFAULT_GAS_LIMIT, null, BigInteger.ZERO, null, null, null)).flowable().toObservable();
    }

    public Observable<ExecStatusInfo> recoverBlacklistedAccount(QuorumNetworkProperty.Node node, String org, String account) {
        String fromAccount = accountService.getDefaultAccountAddress(node).blockingFirst();
        Request<?, ExecStatusInfo> nodeInfoRequest = new Request<>(
            "quorumPermission_recoverBlackListedAccount",
            List.of(org, account, new PrivateTransaction(fromAccount, null, DEFAULT_GAS_LIMIT, null, BigInteger.ZERO, null, null, null)),
            connectionFactory().getWeb3jService(node),
            ExecStatusInfo.class
        );
        return nodeInfoRequest.flowable().toObservable();
    }

    public Observable<ExecStatusInfo> approveBlacklistedAccountRecovery(QuorumNetworkProperty.Node node, String org, String account) {
        String fromAccount = accountService.getDefaultAccountAddress(node).blockingFirst();

        Request<?, ExecStatusInfo> nodeInfoRequest = new Request<>(
            "quorumPermission_approveBlackListedAccountRecovery",
            List.of(org, account, new PrivateTransaction(fromAccount, null, DEFAULT_GAS_LIMIT, null, BigInteger.ZERO, null, null, null)),
            connectionFactory().getWeb3jService(node),
            ExecStatusInfo.class
        );
        return nodeInfoRequest.flowable().toObservable();
    }


    public Observable<ExecStatusInfo> updateNode(QuorumNetworkProperty.Node node, String org, String enode, int status) {
        Quorum client = connectionFactory().getConnection(node);
        String fromAccount = accountService.getDefaultAccountAddress(node).blockingFirst();
        return client.quorumPermissionUpdateNodeStatus(org, enode, status, new PrivateTransaction(fromAccount, null, DEFAULT_GAS_LIMIT, null, BigInteger.ZERO, null, null, null)).flowable().toObservable();
    }

    public Observable<ExecStatusInfo> recoverBlacklistedNode(QuorumNetworkProperty.Node node, String org, String enode) {
        Quorum client = connectionFactory().getConnection(node);
        String fromAccount = accountService.getDefaultAccountAddress(node).blockingFirst();
        return client.quorumPermissionRecoverBlackListedNode(org, enode, new PrivateTransaction(fromAccount, null, DEFAULT_GAS_LIMIT, null, BigInteger.ZERO, null, null, null)).flowable().toObservable();
    }

    public Observable<ExecStatusInfo> approveBlackListedNodeRecovery(QuorumNetworkProperty.Node node, String org, String enode) {
        Quorum client = connectionFactory().getConnection(node);
        String fromAccount = accountService.getDefaultAccountAddress(node).blockingFirst();
        return client.quorumPermissionApproveBlackListedNodeRecovery(org, enode, new PrivateTransaction(fromAccount, null, DEFAULT_GAS_LIMIT, null, BigInteger.ZERO, null, null, null)).flowable().toObservable();
    }

    public Observable<ExecStatusInfo> addNewRole(QuorumNetworkProperty.Node node, String org, String role, int access, boolean isVoter, boolean isAdmin) {
        Quorum client = connectionFactory().getConnection(node);
        String fromAccount = accountService.getDefaultAccountAddress(node).blockingFirst();
        return client.quorumPermissionAddNewRole(org, role, access, isVoter, isAdmin, new PrivateTransaction(fromAccount, null, DEFAULT_GAS_LIMIT, null, BigInteger.ZERO, null, null, null)).flowable().toObservable();

    }

    public Observable<ExecStatusInfo> removeRole(QuorumNetworkProperty.Node node, String org, String role) {
        Quorum client = connectionFactory().getConnection(node);
        String fromAccount = accountService.getDefaultAccountAddress(node).blockingFirst();
        return client.quorumPermissionRemoveRole(org, role, new PrivateTransaction(fromAccount, null, DEFAULT_GAS_LIMIT, null, BigInteger.ZERO, null, null, null)).flowable().toObservable();

    }

    public Observable<ExecStatusInfo> addSubOrg(QuorumNetworkProperty.Node node, String porg, String sorg, String enode) {
        Quorum client = connectionFactory().getConnection(node);
        String fromAccount = accountService.getDefaultAccountAddress(node).blockingFirst();
        return client.quorumPermissionAddSubOrg(porg, sorg, enode, new PrivateTransaction(fromAccount, null, DEFAULT_GAS_LIMIT, null, BigInteger.ZERO, null, null, null)).flowable().toObservable();

    }

    public Observable<ExecStatusInfo> approveOrg(QuorumNetworkProperty.Node node, String org, String enode, String address) {
        Quorum client = connectionFactory().getConnection(node);
        String fromAccount = accountService.getDefaultAccountAddress(node).blockingFirst();
        return client.quorumPermissionApproveOrg(org, enode, address, new PrivateTransaction(fromAccount, null, DEFAULT_GAS_LIMIT, null, BigInteger.ZERO, null, null, null)).flowable().toObservable();

    }

    public Observable<StringResponse> newAccount(QuorumNetworkProperty.Node from) {
        Request<?, StringResponse> request = new Request<>(
            "personal_newAccount",
            Collections.singletonList(""),
            connectionFactory().getWeb3jService(from),
            StringResponse.class
        );

        return request.flowable().toObservable();
    }

    public Observable<BoolResponse> unlockAccount(QuorumNetworkProperty.Node fromNode, String acct) {
        Request<?, BoolResponse> request = new Request<>(
            "personal_unlockAccount",
            List.of(acct, "", 0),
            connectionFactory().getWeb3jService(fromNode),
            BoolResponse.class
        );

        return request.flowable().toObservable();
    }
}
