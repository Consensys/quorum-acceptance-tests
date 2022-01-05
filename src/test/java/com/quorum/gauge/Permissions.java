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

package com.quorum.gauge;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.quorum.gauge.common.*;
import com.quorum.gauge.core.AbstractSpecImplementation;
import com.quorum.gauge.services.InfrastructureService;
import com.quorum.gauge.services.RaftService;
import com.quorum.gauge.services.UtilService;
import com.thoughtworks.gauge.Step;
import com.thoughtworks.gauge.datastore.DataStoreFactory;
import io.reactivex.Observable;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.response.EthBlockNumber;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.quorum.PrivacyFlag;
import org.web3j.quorum.methods.response.permissioning.*;
import org.web3j.tx.Contract;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Service
public class Permissions extends AbstractSpecImplementation {

    @Autowired
    UtilService utilService;

    @Autowired
    private InfrastructureService infraService;

    @Autowired
    private RaftService raftService;


    private static final Map<String, Integer> accountAccessMap = new HashMap<>();
    private static final Map<String, Integer> nodeStatusMap = new HashMap<>();
    private static final Map<String, Integer> orgStatusMap = new HashMap<>();
    private static final Map<String, Integer> acctStatusMap = new HashMap<>();
    private static final Map<String, String> configMap = new HashMap<>();

    static {
        configMap.put("NWADMIN-org", "NWADMIN");
        configMap.put("NWADMIN-role", "NWADMIN");
        configMap.put("OADMIN-role", "ORGDMIN");

        accountAccessMap.put("ReadOnly", 0);
        accountAccessMap.put("Transact", 1);
        accountAccessMap.put("ContractDeploy", 2);
        accountAccessMap.put("FullAccess", 3);

        nodeStatusMap.put("Pending", 1);
        nodeStatusMap.put("Approved", 2);
        nodeStatusMap.put("Deactivated", 3);
        nodeStatusMap.put("Blacklisted", 4);
        nodeStatusMap.put("BlacklistedRecovery", 5);

        acctStatusMap.put("Pending", 1);
        acctStatusMap.put("Active", 2);
        acctStatusMap.put("InActive", 3);
        acctStatusMap.put("Suspended", 4);
        acctStatusMap.put("Blacklisted", 5);
        acctStatusMap.put("Revoked", 6);
        acctStatusMap.put("BlacklistedRecovery", 7);

        orgStatusMap.put("Proposed", 1);
        orgStatusMap.put("Approved", 2);
        orgStatusMap.put("PendingSuspension", 3);
        orgStatusMap.put("Suspended", 4);
        orgStatusMap.put("RevokeSuspension", 5);
    }

    private static final Logger logger = LoggerFactory.getLogger(SmartContractDualState.class);

    public Permissions() {
    }

    @Step("Deploy <permissionVersion> <contractName> smart contract setting default account of <node> as the guardian account, name this as <contractNameKey>")
    public void deployPermissionsContracts(String permissionVersion, String contractName, QuorumNetworkProperty.Node node, String contractNameKey) {
        Contract c = permissionContractService.createPermissionsGenericContracts(node, contractName, null, permissionVersion).blockingFirst();
        logger.debug("{} contract address is:{}", contractName, c.getContractAddress());

        assertThat(c.getContractAddress()).isNotBlank();
        DataStoreFactory.getScenarioDataStore().put(contractNameKey, c);
    }

    @Step("From <node> deploy <permissionVersion> <contractName> contract passing <upgrContractKey> address, name it <contractNameKey>")
    public void deployPermissionsGenericContracts(QuorumNetworkProperty.Node node, String permissionVersion, String contractName, String upgrContractKey, String contractNameKey) {
        // get the upgradable contract address from store, pass it to deploy call
        String upgrContractAddress = mustHaveValue(DataStoreFactory.getScenarioDataStore(), upgrContractKey, Contract.class).getContractAddress();
        logger.debug("upgradable contract address is:{}", upgrContractAddress);
        Contract c = permissionContractService.createPermissionsGenericContracts(node, contractName, upgrContractAddress, permissionVersion).blockingFirst();
        logger.debug("{} contract address is:{}", contractName, c.getContractAddress());

        assertThat(c.getContractAddress()).isNotBlank();
        DataStoreFactory.getScenarioDataStore().put(contractNameKey, c);
    }

    @Step("From <node> deploy <permissionVersion> implementation contract passing addresses of <upgrContractKey>, <orgContractKey>, <roleContractKey>, <accountContractKey>, <voterContractKey>, <nodeContractKey>. Name this as <contractNameKey>")
    public void deployPermissionsImpementation(QuorumNetworkProperty.Node node, String permissionVersion, String upgrContractKey, String orgContractKey, String roleContractKey, String accountContractKey, String voterContractKey, String nodeContractKey, String contractNameKey) {
        // get the address of all deployed contracts
        String upgrContractAddress = mustHaveValue(DataStoreFactory.getScenarioDataStore(), upgrContractKey, Contract.class).getContractAddress();
        String orgMgrAddress = mustHaveValue(DataStoreFactory.getScenarioDataStore(), orgContractKey, Contract.class).getContractAddress();
        String roleMgrAddress = mustHaveValue(DataStoreFactory.getScenarioDataStore(), roleContractKey, Contract.class).getContractAddress();
        String acctMgrAddress = mustHaveValue(DataStoreFactory.getScenarioDataStore(), accountContractKey, Contract.class).getContractAddress();
        String voterMgrAddress = mustHaveValue(DataStoreFactory.getScenarioDataStore(), voterContractKey, Contract.class).getContractAddress();
        String nodeMgrAddress = mustHaveValue(DataStoreFactory.getScenarioDataStore(), nodeContractKey, Contract.class).getContractAddress();
        Contract c = permissionContractService.createPermissionsImplementationContract(node, upgrContractAddress, orgMgrAddress, roleMgrAddress, acctMgrAddress, voterMgrAddress, nodeMgrAddress, permissionVersion).blockingFirst();
        assertThat(c.getContractAddress()).isNotBlank();
        DataStoreFactory.getScenarioDataStore().put(contractNameKey, c);
    }

    @Step("Create permissions-config.json object using the contract addersses of <upgrContractKey>, <interfaceContractKey>, <implContractKey>, <orgContractKey>, <roleContractKey>,  <acctContractKey>, <voterContractKey>, <nodeContractKey>. Name it <objectName>")
    public void createPermissionsConfig(String upgrContractKey, String interfaceContractKey, String implContractKey, String orgContractKey, String roleContractKey, String acctContractKey, String voterContractKey, String nodeContractKey, String objectName) {
        String upgrContractAddress = mustHaveValue(DataStoreFactory.getScenarioDataStore(), upgrContractKey, Contract.class).getContractAddress();
        String interfaceContractAddress = mustHaveValue(DataStoreFactory.getScenarioDataStore(), interfaceContractKey, Contract.class).getContractAddress();
        String implContractAddress = mustHaveValue(DataStoreFactory.getScenarioDataStore(), implContractKey, Contract.class).getContractAddress();
        String orgMgrAddress = mustHaveValue(DataStoreFactory.getScenarioDataStore(), orgContractKey, Contract.class).getContractAddress();
        String roleMgrAddress = mustHaveValue(DataStoreFactory.getScenarioDataStore(), roleContractKey, Contract.class).getContractAddress();
        String acctMgrAddress = mustHaveValue(DataStoreFactory.getScenarioDataStore(), acctContractKey, Contract.class).getContractAddress();
        String voterMgrAddress = mustHaveValue(DataStoreFactory.getScenarioDataStore(), voterContractKey, Contract.class).getContractAddress();
        String nodeMgrAddress = mustHaveValue(DataStoreFactory.getScenarioDataStore(), nodeContractKey, Contract.class).getContractAddress();

        PermissionsConfig permissionsConfig = new PermissionsConfig();
        permissionsConfig.setUpgradableAddress(upgrContractAddress);
        permissionsConfig.setInterfaceAddress(interfaceContractAddress);
        permissionsConfig.setImplAddress(implContractAddress);
        permissionsConfig.setNodeMgrAddress(nodeMgrAddress);
        permissionsConfig.setAccountMgrAddress(acctMgrAddress);
        permissionsConfig.setRoleMgrAddress(roleMgrAddress);
        permissionsConfig.setVoterMgrAddress(voterMgrAddress);
        permissionsConfig.setOrgMgrAddress(orgMgrAddress);

        logger.debug("perm config object is {}", permissionsConfig.toString());
        DataStoreFactory.getScenarioDataStore().put(objectName, permissionsConfig);
    }

    @Step("Update <objectName>. Add <node>'s default account to accounts in config")
    public void addAccountToConfig(String objectName, QuorumNetworkProperty.Node node) {
        PermissionsConfig permConfig = mustHaveValue(DataStoreFactory.getScenarioDataStore(), objectName, PermissionsConfig.class);
        logger.debug("perm config object is {}", permConfig.toString());

        List<String> accounts = new ArrayList<>();
        accounts.add(accountService.getDefaultAccountAddress(node).blockingFirst());
        permConfig.setAccounts(accounts);

        logger.debug("perm config object is {}", permConfig.toString());
        DataStoreFactory.getScenarioDataStore().put(objectName, permConfig);
    }

    @Step("Update <objectName>. Add permission <permissionVersion> in config")
    public void addVersionToConfig(String objectName, String permissionVersion) {
        PermissionsConfig permConfig = mustHaveValue(DataStoreFactory.getScenarioDataStore(), objectName, PermissionsConfig.class);
        logger.debug("perm config object is {}", permConfig.toString());

        switch (permissionVersion.toLowerCase().trim()) {
            case "v2":
                permConfig.setPermissionModel("v2");
                break;
            case "v1":
                permConfig.setPermissionModel("v1");
                break;
            default:
                throw new RuntimeException("unsupported permission version");
        }


        logger.debug("perm config object is {}", permConfig.toString());
        DataStoreFactory.getScenarioDataStore().put(objectName, permConfig);
    }

    @Step("Update <objectName>. Add <nwAdminOrg> as network admin org, <nwAdminRole> network admin role, <orgAdminRole> as the org admin role")
    public void setNetworkDetails(String objectName, String nwAdminOrg, String nwAdminRole, String orgAdminRole) {
        PermissionsConfig permConfig = mustHaveValue(DataStoreFactory.getScenarioDataStore(), objectName, PermissionsConfig.class);
        permConfig.setNwAdminOrg(nwAdminOrg);
        permConfig.setNwAdminRole(nwAdminRole);
        permConfig.setOrgAdminRole(orgAdminRole);

        logger.debug("perm config object is {}", permConfig.toString());
        DataStoreFactory.getScenarioDataStore().put(objectName, permConfig);
    }

    @Step("Update <objectName>. Set suborg depth as <subOrgDepth>, suborg breadth as <subOrgBreadth>")
    public void setSubDepthBreadth(String objectName, int subOrgDepth, int subOrgBreadth) {
        PermissionsConfig permConfig = mustHaveValue(DataStoreFactory.getScenarioDataStore(), objectName, PermissionsConfig.class);
        permConfig.setSubOrgBreadth(subOrgBreadth);
        permConfig.setSubOrgDepth(subOrgDepth);

        logger.debug("perm cofnig object is {}", permConfig.toString());
        DataStoreFactory.getScenarioDataStore().put(objectName, permConfig);
    }

    @Step("Write <objectName> to the data directory of <nodes>")
    public void writePermissionConfig(String objectName, List<QuorumNetworkProperty.Node> nodes) {
        InfrastructureService.NetworkResources networkResources = mustHaveValue(DataStoreFactory.getScenarioDataStore(), "networkResources", InfrastructureService.NetworkResources.class);
        PermissionsConfig permConfig = mustHaveValue(DataStoreFactory.getScenarioDataStore(), objectName, PermissionsConfig.class);
        ObjectMapper mapper = new ObjectMapper();
        Observable.fromIterable(networkResources.allResourceIds())
            .filter(containerId -> infraService.isGeth(containerId).blockingFirst())
            .doOnNext(gethContainerId -> logger.debug("Writing permissions-config.json {}", StringUtils.substring(gethContainerId, 0, 12)))
            .flatMap(gethContainerId -> infraService.writeFile(gethContainerId,
                "/data/qdata/permission-config.json",
                mapper.writerWithDefaultPrettyPrinter().writeValueAsString(permConfig)))
            .blockingSubscribe();
        logger.debug("perm config copied");
    }

    @Step("From <node> execute <permissionVersion> permissions init on <upgrContractKey> passing <interfaceContractKey> and <implContractKey> contract addresses")
    public void executePermInit(QuorumNetworkProperty.Node node, String permissionVersion, String upgrContractKey, String interfaceContractKey, String implContractKey) {
        String upgrContractAddress = mustHaveValue(DataStoreFactory.getScenarioDataStore(), upgrContractKey, Contract.class).getContractAddress();
        String interfaceContractAddress = mustHaveValue(DataStoreFactory.getScenarioDataStore(), interfaceContractKey, Contract.class).getContractAddress();
        String implContractAddress = mustHaveValue(DataStoreFactory.getScenarioDataStore(), implContractKey, Contract.class).getContractAddress();

        TransactionReceipt tx = permissionContractService.executeNetworkInit(node, upgrContractAddress, interfaceContractAddress, implContractAddress, permissionVersion).blockingFirst();

        assertThat(tx.getTransactionHash()).isNotBlank();
    }

    @Step("Get network details from <node>")
    public void getNetworkDetails(QuorumNetworkProperty.Node node) {
        PermissionOrgList orgList = permissionService.getPermissionOrgList(node).blockingFirst();
        int c = 0;
        for (PermissionOrgInfo o : orgList.getPermissionOrgList()) {
            ++c;
            logger.debug("{} org -> {}", c, o);
        }

        DataStoreFactory.getScenarioDataStore().put("permOrgList", orgList.getPermissionOrgList());

        PermissionAccountList acctList = permissionService.getPermissionAccountList(node).blockingFirst();
        List<PermissionAccountInfo> pa = acctList.getPermissionAccountList();
        c = 0;
        for (PermissionAccountInfo o : pa) {
            ++c;
            logger.debug("{} acct -> {}", c, o);
        }

        DataStoreFactory.getScenarioDataStore().put("permAcctList", pa);


        PermissionNodeList nodeList = permissionService.getPermissionNodeList(node).blockingFirst();
        List<PermissionNodeInfo> pn = nodeList.getPermissionNodeList();
        c = 0;
        for (PermissionNodeInfo o : pn) {
            ++c;
            logger.debug("{} node -> {}", c, o);
        }
        DataStoreFactory.getScenarioDataStore().put("permNodeList", pn);

        PermissionRoleList roleList = permissionService.getPermissionRoleList(node).blockingFirst();
        List<PermissionRoleInfo> pr = roleList.getPermissionRoleList();
        c = 0;
        for (PermissionRoleInfo o : pr) {
            ++c;
            logger.debug("{} role -> {}", c, o);
        }
        DataStoreFactory.getScenarioDataStore().put("permRoleList", pr);
    }

    @Step("Check org <org> is <status> with no parent, level <level> and empty sub orgs")
    public void checkOrgExists3(String org, String status, int level) throws Exception {
        boolean isPresent = orgExists("TYPE3", org, "", status, level, "");
        assertThat(isPresent).isTrue();
    }

    private String getNetworkAdminOrg(String org) {
        String key = org + "-org";
        if (configMap.get(key) != null) {
            logger.debug("key " + key + " has value");
            return configMap.get(key);
        }
        return org;
    }

    private String getNetworkOrgAdminRole(String role) {
        String key = role + "-role";
        if (configMap.get(key) != null) {
            return configMap.get(key);
        }
        return role;
    }

    private boolean orgExists(String checkType, String org, String porg, String status, int level, String slist) {
        org = getNetworkAdminOrg(org);
        porg = getNetworkAdminOrg(porg);
        List<PermissionOrgInfo> permOrgList = (ArrayList<PermissionOrgInfo>) DataStoreFactory.getScenarioDataStore().get("permOrgList");
        assertThat(permOrgList.size()).isNotEqualTo(0);
        int c = 0;
        boolean isPresent = false;
        l1:
        for (PermissionOrgInfo i : permOrgList) {
            ++c;
            logger.debug("{} org id: {} parent id: {} level: {}", c, i.getFullOrgId(), i.getParentOrgId(), i.getLevel());
            switch (checkType) {
                case "TYPE1":
                    if (i.getFullOrgId().equals(org) && (i.getParentOrgId() == null || i.getParentOrgId().equals(""))
                        && i.getLevel() == level && (i.getSubOrgList().size() > 0 && i.getSubOrgList().containsAll(Arrays.asList(slist.split(",")))) &&
                        i.getStatus() == orgStatusMap.get(status)) {
                        isPresent = true;
                        break l1;
                    }
                    break;
                case "TYPE2":
                    if (i.getFullOrgId().equals(org) && i.getParentOrgId().equals(porg)
                        && i.getLevel() == level && (i.getSubOrgList() == null || i.getSubOrgList().size() == 0) &&
                        i.getStatus() == orgStatusMap.get(status)) {
                        isPresent = true;
                        break l1;
                    }
                    break;
                case "TYPE3":
                    if (i.getFullOrgId().equals(org) && (i.getParentOrgId() == null || i.getParentOrgId().equals(""))
                        && i.getLevel() == level && (i.getSubOrgList() == null || i.getSubOrgList().size() == 0) &&
                        i.getStatus() == orgStatusMap.get(status)) {
                        isPresent = true;
                        break l1;
                    }
                    break;
            }
        }
        return isPresent;
    }

    public String getEnodeId(QuorumNetworkProperty.Node node) {
        String enode = permissionService.NodeInfo(node);
        String enodeId = enode.substring(0, enode.indexOf("@")).substring("enode://".length());
        return enodeId;
    }

    @Step("Check org <org> has <node> with status <status>")
    public void checkNodeExists(String org, QuorumNetworkProperty.Node node, String status) throws Exception {
        org = getNetworkAdminOrg(org);
        String enodeId = getEnodeId(node);
        List<PermissionNodeInfo> permNodeList = (ArrayList<PermissionNodeInfo>) DataStoreFactory.getScenarioDataStore().get("permNodeList");
        assertThat(permNodeList.size()).isNotEqualTo(0);
        int c = 0;
        boolean isPresent = false;
        for (PermissionNodeInfo i : permNodeList) {
            ++c;
            logger.debug("{} node: {} status: {}", c, i.getUrl(), i.getStatus());
            if (i.getOrgId().equals(org) && i.getUrl().contains(enodeId) && i.getStatus() == nodeStatusMap.get(status)) {
                isPresent = true;
                break;
            }
        }
        assertThat(isPresent).isTrue();
    }

    private boolean roleExists(String checkType, String org, String role, String access) {
        org = getNetworkAdminOrg(org);
        role = getNetworkOrgAdminRole(role);
        List<PermissionRoleInfo> permRoleList = (ArrayList<PermissionRoleInfo>) DataStoreFactory.getScenarioDataStore().get("permRoleList");
        assertThat(permRoleList.size()).isNotEqualTo(0);
        int c = 0;
        boolean isPresent = false;
        l1:
        for (PermissionRoleInfo i : permRoleList) {
            ++c;
            switch (checkType) {
                case "TYPE1":
                    if (i.getOrgId().equals(org) && i.getRoleId().equals(role) && i.getAccess() == accountAccessMap.get(access) && i.getActive() && i.isVoter()) {
                        isPresent = true;
                        break l1;
                    }
                    break;
                case "TYPE2":
                    if (i.getOrgId().equals(org) && i.getRoleId().equals(role) && i.getAccess() == accountAccessMap.get(access) && i.getActive() && !i.isVoter()) {
                        isPresent = true;
                        break l1;
                    }
                    break;
                case "TYPE3":
                    if (i.getOrgId().equals(org) && i.getRoleId().equals(role) && i.getAccess() == accountAccessMap.get(access) && !i.getActive() && !i.isVoter()) {
                        isPresent = true;
                        break l1;
                    }
                    break;
            }

        }
        return isPresent;
    }

    @Step("Check org <org> has role <role> with access <access> and permission to vote and is active")
    public void checkRoleExists1(String org, String role, String access) throws Exception {
        boolean isPresent = roleExists("TYPE1", org, role, access);
        assertThat(isPresent).isTrue();
    }

    private boolean accountExists(String acct, String org, String role, boolean orgAdmin) {
        org = getNetworkAdminOrg(org);
        role = getNetworkOrgAdminRole(role);
        String defaultAcct = acct;
        List<PermissionAccountInfo> pacctList = (ArrayList<PermissionAccountInfo>) DataStoreFactory.getScenarioDataStore().get("permAcctList");
        assertThat(pacctList.size()).isNotEqualTo(0);
        int c = 0;
        boolean isPresent = false;
        for (PermissionAccountInfo i : pacctList) {
            ++c;
            logger.debug("{} address: {} role: {} org: {}", c, i.getAcctId(), i.getRoleId(), i.getOrgId());
            if (orgAdmin) {
                if (i.getAcctId().equalsIgnoreCase(defaultAcct) && i.getOrgId().equals(org) &&
                    i.getRoleId().equals(role) && i.isOrgAdmin() && i.getStatus() == acctStatusMap.get("Active")) {
                    isPresent = true;
                    break;
                }
            } else {
                if (i.getAcctId().equalsIgnoreCase(defaultAcct) && i.getOrgId().equals(org) &&
                    i.getRoleId().equals(role) && !i.isOrgAdmin() && i.getStatus() == acctStatusMap.get("Active")) {
                    isPresent = true;
                    break;
                }
            }
        }
        return isPresent;
    }

    @Step("Check <node>'s default account is from org <org> and has role <role> and is org admin and is active")
    public void checkAccountExists3(QuorumNetworkProperty.Node node, String org, String role) throws Exception {
        String acct = accountService.getDefaultAccountAddress(node).blockingFirst();
        boolean isPresent = accountExists(acct, org, role, true);
        assertThat(isPresent).isTrue();
    }

    private void waitForOrgStatus(QuorumNetworkProperty.Node proposingNode, String orgId, String status) {
        assertThat(permissionService.getPermissionOrgList(proposingNode)
            .map(orgList -> {
                boolean found = false;
                for (PermissionOrgInfo orgInfo : orgList.getPermissionOrgList()) {
                    if (orgId.equalsIgnoreCase(orgInfo.getOrgId()) && orgInfo.getStatus() == orgStatusMap.get(status)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    throw new Exception("not found yet"); // to trigger retry
                }
                return true;
            })
            .doOnNext(found -> logger.debug("Org = {}, expected status = {}, ready = {}", orgId, status, found))
            .retryWhen(errors -> errors
                .zipWith(Observable.range(1, 5), (n, i) -> i) // how many retries
                .flatMap(retryCount -> Observable.timer(1, TimeUnit.SECONDS))) // sleep x seconds between retry
            .blockingFirst()
        ).as("wait for the org " + orgId + " status to be " + status).isTrue();
    }

    private void waitForNodeStatus(QuorumNetworkProperty.Node proposingNode, String enodeId, String status) {
        assertThat(permissionService.getPermissionNodeList(proposingNode)
            .map(nodeList -> {
                boolean found = false;
                for (PermissionNodeInfo nodeInfo : nodeList.getPermissionNodeList()) {
                    if (nodeInfo.getUrl().contains(enodeId) && nodeInfo.getStatus() == nodeStatusMap.get(status)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    throw new Exception("not found yet"); // to trigger retry
                }
                return true;
            })
            .doOnNext(found -> logger.debug("Enode Id = {}, expected status = {}, ready = {}", enodeId, status, found))
            .retryWhen(errors -> errors
                .zipWith(Observable.range(1, 5), (n, i) -> i) // how many retries
                .flatMap(retryCount -> Observable.timer(1, TimeUnit.SECONDS))) // sleep x seconds between retry
            .blockingFirst()
        ).as("wait for the node id " + enodeId + " status to be " + status).isTrue();
    }

    private void waitForRoleStatus(QuorumNetworkProperty.Node proposingNode, String orgId, String roleId, boolean roleStatus) {
        assertThat(permissionService.getPermissionRoleList(proposingNode)
            .map(roleList -> {
                boolean found = false;
                for (PermissionRoleInfo roleInfo : roleList.getPermissionRoleList()) {
                    if (orgId.equalsIgnoreCase(roleInfo.getOrgId()) && roleId.equalsIgnoreCase(roleInfo.getRoleId()) && roleInfo.getActive() == roleStatus) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    throw new Exception("not found yet"); // to trigger retry
                }
                return true;
            })
            .doOnNext(found -> logger.debug("Org = {}, Role Id = {} is active", orgId, roleId))
            .retryWhen(errors -> errors
                .zipWith(Observable.range(1, 5), (n, i) -> i) // how many retries
                .flatMap(retryCount -> Observable.timer(1, TimeUnit.SECONDS))) // sleep x seconds between retry
            .blockingFirst()
        ).as("wait for the role id " + roleId + " status to be active").isTrue();
    }

    private void waitForAccountStatus(QuorumNetworkProperty.Node proposingNode, String account, String status) {
        assertThat(permissionService.getPermissionAccountList(proposingNode)
            .map(accountList -> {
                boolean found = false;
                for (PermissionAccountInfo accountInfo : accountList.getPermissionAccountList()) {
                    if (account.equalsIgnoreCase(accountInfo.getAcctId()) && accountInfo.getStatus() == acctStatusMap.get(status)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    throw new Exception("not found yet"); // to trigger retry
                }
                return true;
            })
            .doOnNext(found -> logger.debug("AccountId = {}, status = {}", account, status))
            .retryWhen(errors -> errors
                .zipWith(Observable.range(1, 5), (n, i) -> i) // how many retries
                .flatMap(retryCount -> Observable.timer(1, TimeUnit.SECONDS))) // sleep x seconds between retry
            .blockingFirst()
        ).as("wait for the account id " + account + " status to be {}" + status).isTrue();
    }

    private void waitForAccountStatusWithRole(QuorumNetworkProperty.Node proposingNode, String account, String status, String roleId) {
        assertThat(permissionService.getPermissionAccountList(proposingNode)
            .map(accountList -> {
                boolean found = false;
                for (PermissionAccountInfo accountInfo : accountList.getPermissionAccountList()) {
                    if (account.equalsIgnoreCase(accountInfo.getAcctId()) && roleId.equals(accountInfo.getRoleId()) && accountInfo.getStatus() == acctStatusMap.get(status)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    throw new Exception("not found yet"); // to trigger retry
                }
                return true;
            })
            .doOnNext(found -> logger.debug("AccountId = {}, status = {}", account, status))
            .retryWhen(errors -> errors
                .zipWith(Observable.range(1, 5), (n, i) -> i) // how many retries
                .flatMap(retryCount -> Observable.timer(1, TimeUnit.SECONDS))) // sleep x seconds between retry
            .blockingFirst()
        ).as("wait for the account id " + account + " status to be {}" + status).isTrue();
    }

    @Step("From <proposingNode> propose new org <orgId> into the network with <node>'s enode id and <accountKey> account")
    public void proposeOrg(QuorumNetworkProperty.Node proposingNode, String orgId, QuorumNetworkProperty.Node node, String accountKey) {
        String acctId = node.getAccountAliases().get(accountKey);
        ExecStatusInfo execStatus = permissionService.addOrg(proposingNode, orgId, node.getEnodeUrl(), acctId).blockingFirst();
        assertThat(!execStatus.hasError());
        waitForOrgStatus(proposingNode, orgId, "Proposed");
    }

    @Step("From <proposingNode> approve new org <orgId> into the network with <node>'s enode id and <accountKey> account")
    public void approveOrg(QuorumNetworkProperty.Node proposingNode, String orgId, QuorumNetworkProperty.Node node, String accountKey) {
        String acctId = node.getAccountAliases().get(accountKey);
        ExecStatusInfo execStatus = permissionService.approveOrg(proposingNode, orgId, node.getEnodeUrl(), acctId).blockingFirst();
        assertThat(!execStatus.hasError());
        waitForOrgStatus(proposingNode, orgId, "Approved");
    }

    @Step("From <proposingNode> add new node <url> to network org <orgId>")
    public void addNode(QuorumNetworkProperty.Node proposingNode, String url, String orgId) {
        ExecStatusInfo execStatus = permissionService.addNode(proposingNode, orgId, url).blockingFirst();
        assertThat(!execStatus.hasError());
        waitForNodeStatus(proposingNode, url, "Approved");
    }

    public void updateNodeAction(String action, QuorumNetworkProperty.Node fromNode, String url, String orgId) {
        switch (action) {
            case "deactivate":
                ExecStatusInfo e1 = permissionService.updateNode(fromNode, orgId, url, 3).blockingFirst();
                assertThat(!e1.hasError());
                waitForNodeStatus(fromNode, url, "Blacklisted");
                break;
            case "recoverBlacklist":
                ExecStatusInfo e2 = permissionService.recoverBlacklistedNode(fromNode, orgId, url).blockingFirst();
                assertThat(!e2.hasError());
                waitForNodeStatus(fromNode, url, "BlacklistedRecovery");
                break;
            case "approveRecoverBlacklist":
                ExecStatusInfo e3 = permissionService.approveBlackListedNodeRecovery(fromNode, orgId, url).blockingFirst();
                assertThat(!e3.hasError());
                waitForNodeStatus(fromNode, url, "Approved");
                break;
        }
    }

    @Step("Start stand alone <node>")
    public void startOneNodes(QuorumNetworkProperty.Node node) {
        GethArgBuilder additionalGethArgs = GethArgBuilder.newBuilder();
        InfrastructureService.NetworkResources networkResources = mustHaveValue(DataStoreFactory.getScenarioDataStore(), "networkResources", InfrastructureService.NetworkResources.class);
        raftService.addPeer(networkResources.aNodeName(), node.getEnodeUrl(), NodeType.peer)
            .doOnNext(res -> {
                Response.Error err = Optional.ofNullable(res.getError()).orElse(new Response.Error());
                assertThat(err.getMessage()).as("raft.addPeer must succeed").isBlank();
            })
            .map(Response::getResult)
            .flatMap(raftId -> infraService.startNode(
                InfrastructureService.NodeAttributes.forNode(node.getName())
                    .withAdditionalGethArgs(additionalGethArgs.raftjoinexisting(raftId))
                    .withAdditionalGethArgs(additionalGethArgs.permissioned(true)),
                resourceId -> networkResources.add(node.getName(), resourceId)
            ))
            .blockingSubscribe();
    }

    @Step("From <node> suspend org <org>, confirm that org status is <status>")
    public void suspendOrg(QuorumNetworkProperty.Node node, String org, String status) {
        org = getNetworkAdminOrg(org);
        ExecStatusInfo execStatus = permissionService.updateOrgStatus(node, org, 1).blockingFirst();
        assertThat(!execStatus.hasError());
        waitForOrgStatus(node, org, status);
    }

    @Step("From <node> approve org <org>'s suspension, confirm that org status is <status>")
    public void approveSuspension(QuorumNetworkProperty.Node node, String org, String status) {
        org = getNetworkAdminOrg(org);
        ExecStatusInfo execStatus = permissionService.approveOrgStatus(node, org, 1).blockingFirst();
        assertThat(!execStatus.hasError());
        waitForOrgStatus(node, org, status);
    }

    @Step("From <node> revoke suspension of org <org>, confirm that org status is <status>")
    public void revokeSuspension(QuorumNetworkProperty.Node node, String org, String status) {
        org = getNetworkAdminOrg(org);
        ExecStatusInfo execStatus = permissionService.updateOrgStatus(node, org, 2).blockingFirst();
        assertThat(!execStatus.hasError());
        waitForOrgStatus(node, org, status);
    }

    @Step("From <node> approve org <org>'s suspension revoke, confirm that org status is <status>")
    public void approveSuspensionRevoke(QuorumNetworkProperty.Node node, String org, String status) {
        org = getNetworkAdminOrg(org);
        ExecStatusInfo execStatus = permissionService.approveOrgStatus(node, org, 2).blockingFirst();
        assertThat(!execStatus.hasError());
        waitForOrgStatus(node, org, status);
    }

    @Step("Deploy <contractName> smart contract with initial value <initialValue> from a default account in <node> fails with error <error>")
    public void setupStorecAsPublicDependentContract(String contractName, int initialValue, QuorumNetworkProperty.Node node, String error) {
        Contract c = null;
        String exMsg = "";
        try {
            c = contractService.createGenericStoreContract(node, contractName, initialValue, null, false, null, null).blockingFirst();

        } catch (Exception ex) {
            exMsg = ex.getMessage();
            logger.debug("deploy contract failed " + ex.getMessage());
        }
        assertThat(c).isNull();
        //assertThat(exMsg.contains(error)).isTrue();
    }

    @Step("Deploy <contractName> smart contract with initial value <initialValue> from a default account in <node> until block height reaches <qip714block>")
    public void setupStorecAsPublicDependentContractUntilBlockHeightReached(String contractName, int initialValue, QuorumNetworkProperty.Node node, int qip714block) {
        int curBlockHeight = utilService.getCurrentBlockNumber().blockingFirst().getBlockNumber().intValue();
        while (curBlockHeight < qip714block) {
            Contract c = contractService.createGenericStoreContract(
                node,
                contractName,
                initialValue,
                null,
                false,
                null,
                PrivacyFlag.STANDARD_PRIVATE
            ).blockingFirst();
            assertThat(c).isNotNull();
            curBlockHeight = utilService.getCurrentBlockNumber().blockingFirst().getBlockNumber().intValue();
            logger.debug("curBlockHeight:{} height:{}", curBlockHeight, qip714block);
        }
        logger.debug("block height reached");
    }


    private String getFullEnode(String enode, QuorumNetworkProperty.Node node) {
        List<PermissionNodeInfo> permNodeList = permissionService.getPermissionNodeList(node).blockingFirst().getPermissionNodeList();
        assertThat(permNodeList.size()).isNotEqualTo(0);
        int c = 0;
        boolean isPresent = false;
        for (PermissionNodeInfo i : permNodeList) {
            ++c;
            if (i.getUrl().contains(enode)) {
                return i.getUrl();
            }
        }
        return null;
    }

    @Step("From <fromNode> deactivate org <org>'s node <node>")
    public void deactivateNode(QuorumNetworkProperty.Node fromNode, String org, QuorumNetworkProperty.Node node) {
        org = getNetworkAdminOrg(org);
        String enodeId = getEnodeId(node);
        String fullEnodeId = getFullEnode(enodeId, node);
        assertThat(fullEnodeId).isNotNull();
        ExecStatusInfo execStatus = permissionService.updateNode(fromNode, org, fullEnodeId, 1).blockingFirst();
        waitForNodeStatus(fromNode, enodeId, "Deactivated");
        getNetworkDetails(fromNode);
    }

    @Step("Save current blocknumber from <node>")
    public void saveCurrentBlockNumber(QuorumNetworkProperty.Node node) {
        EthBlockNumber blkNumber = utilService.getCurrentBlockNumberFrom(node).blockingFirst();
        DataStoreFactory.getScenarioDataStore().put(node.getName() + "blockNumber", blkNumber);
        logger.debug("current block number from {} is {}", node.getName(), blkNumber.getBlockNumber().intValue());
        assertThat(blkNumber.getBlockNumber().intValue()).isNotEqualTo(0);
    }

    @Step("Ensure current blocknumber from <node> has not changed")
    public void checkBlockNumberHasNotChanged(QuorumNetworkProperty.Node node) {
        EthBlockNumber oldBlkNumber = (EthBlockNumber) DataStoreFactory.getScenarioDataStore().get(node.getName() + "blockNumber");
        EthBlockNumber newBlkNumber = utilService.getCurrentBlockNumberFrom(node).blockingFirst();
        logger.debug("block number old:{} new:{}", oldBlkNumber.getBlockNumber().intValue(), newBlkNumber.getBlockNumber().intValue());
        assertThat(newBlkNumber.getBlockNumber().intValue()).isEqualTo(oldBlkNumber.getBlockNumber().intValue());
    }

    @Step("From <fromNode> add a new role <roleId> to org <org> with <right>")
    public void addNewRole(QuorumNetworkProperty.Node fromNode, String roleId, String org, String right) {
        int rightId = 0;
        switch (right) {
            case "full access":
                rightId = 3;
                break;
            case "contract deploy":
                rightId = 2;
                break;
            default:
                throw new IllegalArgumentException("unsupported right");
        }

        org = getNetworkAdminOrg(org);
        ExecStatusInfo execStatus = permissionService.addNewRole(fromNode, org, roleId, rightId, false, false).blockingFirst();
        assertThat(!execStatus.hasError());
        waitForRoleStatus(fromNode, org, roleId, true);
    }

    @Step("From <fromNode> add account <accountKey> to org <orgId> role <roleId>")
    public void addAccountToRole(QuorumNetworkProperty.Node fromNode, String accountKey, String orgId, String roleId) {
        orgId = getNetworkAdminOrg(orgId);
        String account = (String) DataStoreFactory.getScenarioDataStore().get(accountKey);
        ExecStatusInfo execStatus = permissionService.addAccountToOrg(fromNode, account, orgId, roleId).blockingFirst();
        assertThat(!execStatus.hasError());
        waitForAccountStatusWithRole(fromNode, account, "Active", roleId);
    }


    @Step("From <fromNode> blacklist account <accountKey> of org <orgId>")
    public void blacklistAccount(QuorumNetworkProperty.Node fromNode, String accountKey, String orgId) {
        orgId = getNetworkAdminOrg(orgId);
        String account = (String) DataStoreFactory.getScenarioDataStore().get(accountKey);
        ExecStatusInfo execStatus = permissionService.updateAccountStatus(fromNode, orgId, account, 3).blockingFirst();
        assertThat(!execStatus.hasError());
        waitForAccountStatus(fromNode, account, "Blacklisted");
    }

    @Step("From <fromNode> recover blacklisted account <accountKey> of org <orgId>")
    public void recoverBlacklistAccount(QuorumNetworkProperty.Node fromNode, String accountKey, String orgId) {
        orgId = getNetworkAdminOrg(orgId);
        String account = (String) DataStoreFactory.getScenarioDataStore().get(accountKey);
        ExecStatusInfo execStatus = permissionService.recoverBlacklistedAccount(fromNode, orgId, account).blockingFirst();
        assertThat(!execStatus.hasError());
        waitForAccountStatus(fromNode, account, "BlacklistedRecovery");
    }

    @Step("From <fromNode> approve recovery of blacklisted account <accountKey> of org <orgId>")
    public void approveRecoveryOfBlacklistedAccount(QuorumNetworkProperty.Node fromNode, String accountKey, String orgId) {
        orgId = getNetworkAdminOrg(orgId);
        String account = (String) DataStoreFactory.getScenarioDataStore().get(accountKey);
        ExecStatusInfo execStatus = permissionService.approveBlacklistedAccountRecovery(fromNode, orgId, account).blockingFirst();
        assertThat(!execStatus.hasError());
        waitForAccountStatus(fromNode, account, "Active");
    }

    @Step("From <fromNode> update account <accountKey>'s role to org <orgId> role <roleId>")
    public void changeAccountRole(QuorumNetworkProperty.Node fromNode, String accountKey, String orgId, String roleId) {
        orgId = getNetworkAdminOrg(orgId);
        String account = (String) DataStoreFactory.getScenarioDataStore().get(accountKey);
        ExecStatusInfo execStatus = permissionService.changeAccountRole(fromNode, account, orgId, roleId).blockingFirst();
        assertThat(!execStatus.hasError());
        waitForAccountStatusWithRole(fromNode, account, "Active", roleId);
    }

    @Step("From <fromNode> remove role <roleId> from org <org>")
    public void removeRole(QuorumNetworkProperty.Node fromNode, String roleId, String org) {
        org = getNetworkAdminOrg(org);
        ExecStatusInfo execStatus = permissionService.removeRole(fromNode, org, roleId).blockingFirst();
        assertThat(!execStatus.hasError());
        waitForRoleStatus(fromNode, org, roleId, false);
    }

    @Step("From <fromNode> assign <targetNode>'s default account to <org> org and <roleId> role")
    public void assignAccountRole(QuorumNetworkProperty.Node fromNode, QuorumNetworkProperty.Node targetNode, String org, String roleId) {
        org = getNetworkAdminOrg(org);
        String account = accountService.getDefaultAccountAddress(targetNode).blockingFirst();
        ExecStatusInfo execStatus = permissionService.assignAdminRole(fromNode, account, org, roleId).blockingFirst();
        assertThat(!execStatus.hasError());
        waitForAccountStatus(fromNode, account, "Pending");
    }

    @Step("From <fromNode> approve <targetNode>'s default account to <org> org and <roleId> role")
    public void approveAccountRole(QuorumNetworkProperty.Node fromNode, QuorumNetworkProperty.Node targetNode, String org, String roleId) {
        org = getNetworkAdminOrg(org);
        String account = accountService.getDefaultAccountAddress(targetNode).blockingFirst();
        ExecStatusInfo execStatus = permissionService.approveAdminRoleAssignment(fromNode, account, org).blockingFirst();
        assertThat(!execStatus.hasError());
        waitForAccountStatus(fromNode, account, "Active");
    }

    @Step("From <fromNode> create new account as <accountKey>")
    public void newAccount(QuorumNetworkProperty.Node fromNode, String accountKey) {
        String account = permissionService.newAccount(fromNode).blockingFirst().getResult();
        assertThat(account != null && !account.trim().equals(""));
        DataStoreFactory.getScenarioDataStore().put(accountKey, account);
        boolean unlockStatus = permissionService.unlockAccount(fromNode, account).blockingFirst().getResult();
        assertThat(unlockStatus).isTrue();
    }

    @Step("From <fromNode> assign admin role to account <accountKey> of org <orgId> role <roleId>")
    public void assignAdminRole(QuorumNetworkProperty.Node fromNode, String accountKey, String orgId, String roleId) {
        String targetAcct = (String) DataStoreFactory.getScenarioDataStore().get(accountKey);
        ExecStatusInfo execStatus = permissionService.assignAdminRole(fromNode, targetAcct, orgId, roleId).blockingFirst();
        assertThat(!execStatus.hasError());
        waitForAccountStatus(fromNode, targetAcct, "Pending");
    }

    @Step("From <fromNode> approve admin role to account <accountKey> of org <orgId>")
    public void approveAdminRole(QuorumNetworkProperty.Node fromNode, String accountKey, String orgId) {
        String targetAcct = (String) DataStoreFactory.getScenarioDataStore().get(accountKey);
        ExecStatusInfo execStatus = permissionService.approveAdminRoleAssignment(fromNode, targetAcct, orgId).blockingFirst();
        assertThat(!execStatus.hasError());
        waitForAccountStatus(fromNode, targetAcct, "Active");
    }


    @Step("From <fromNode> deactivate node <url> of org <orgId>")
    public void deactivateNode(QuorumNetworkProperty.Node fromNode, String url, String orgId) {
        updateNodeAction("deactivate", fromNode, url, orgId);
    }

    @Step("From <fromNode> recover blacklisted node <url> of org <orgId>")
    public void recoverBlacklistNode(QuorumNetworkProperty.Node fromNode, String url, String orgId) {
        updateNodeAction("recoverBlacklist", fromNode, url, orgId);
    }

    @Step("From <fromNode> approve recovery of blacklisted node <url> of org <orgId>")
    public void approveRecoverBlacklist(QuorumNetworkProperty.Node fromNode, String url, String orgId) {
        updateNodeAction("approveRecoverBlacklist", fromNode, url, orgId);
    }

}
