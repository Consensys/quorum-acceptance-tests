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

package com.quorum.gauge.common;

import java.util.List;

public class PermissionsConfig {
    private String upgradableAddress;
    private String interfaceAddress;
    private String implAddress;
    private String orgMgrAddress;
    private String nodeMgrAddress;
    private String accountMgrAddress;
    private String roleMgrAddress;
    private String voterMgrAddress;
    private List<String> accounts;
    private String nwAdminOrg;
    private String nwAdminRole;
    private String orgAdminRole;
    private int subOrgBreadth;
    private int subOrgDepth;

    public String getPermissionModel() {
        return permissionModel;
    }

    public void setPermissionModel(String permissionModel) {
        this.permissionModel = permissionModel;
    }

    private String permissionModel;

    public String getUpgradableAddress() {
        return upgradableAddress;
    }

    public void setUpgradableAddress(String upgradableAddress) {
        this.upgradableAddress = upgradableAddress;
    }

    public String getInterfaceAddress() {
        return interfaceAddress;
    }

    public void setInterfaceAddress(String interfaceAddress) {
        this.interfaceAddress = interfaceAddress;
    }

    public String getImplAddress() {
        return implAddress;
    }

    public void setImplAddress(String implAddress) {
        this.implAddress = implAddress;
    }

    public String getNodeMgrAddress() {
        return nodeMgrAddress;
    }

    public void setNodeMgrAddress(String nodeMgrAddress) {
        this.nodeMgrAddress = nodeMgrAddress;
    }

    public String getAccountMgrAddress() {
        return accountMgrAddress;
    }

    public void setAccountMgrAddress(String accountMgrAddress) {
        this.accountMgrAddress = accountMgrAddress;
    }

    public String getRoleMgrAddress() {
        return roleMgrAddress;
    }

    public void setRoleMgrAddress(String roleMgrAddress) {
        this.roleMgrAddress = roleMgrAddress;
    }

    public String getVoterMgrAddress() {
        return voterMgrAddress;
    }

    public void setVoterMgrAddress(String voterMgrAddress) {
        this.voterMgrAddress = voterMgrAddress;
    }

    public String getNwAdminOrg() {
        return nwAdminOrg;
    }

    public void setNwAdminOrg(String nwAdminOrg) {
        this.nwAdminOrg = nwAdminOrg;
    }

    public String getNwAdminRole() {
        return nwAdminRole;
    }

    public void setNwAdminRole(String nwAdminRole) {
        this.nwAdminRole = nwAdminRole;
    }

    public String getOrgAdminRole() {
        return orgAdminRole;
    }

    public void setOrgAdminRole(String orgAdminRole) {
        this.orgAdminRole = orgAdminRole;
    }

    public int getSubOrgBreadth() {
        return subOrgBreadth;
    }

    public void setSubOrgBreadth(int subOrgBreadth) {
        this.subOrgBreadth = subOrgBreadth;
    }

    public int getSubOrgDepth() {
        return subOrgDepth;
    }

    public void setSubOrgDepth(int subOrgDepth) {
        this.subOrgDepth = subOrgDepth;
    }

    public String getOrgMgrAddress() {
        return orgMgrAddress;
    }

    public void setOrgMgrAddress(String orgMgrAddress) {
        this.orgMgrAddress = orgMgrAddress;
    }

    public void setAccounts(List<String> accounts) {
        this.accounts = accounts;
    }

    @Override
    public String toString() {
        return "PermissionsConfig{" +
            "permissionModel='" + permissionModel + '\'' +
            "upgradableAddress='" + upgradableAddress + '\'' +
            ", interfaceAddress='" + interfaceAddress + '\'' +
            ", implAddress='" + implAddress + '\'' +
            ", orgMgrAddress='" + orgMgrAddress + '\'' +
            ", nodeMgrAddress='" + nodeMgrAddress + '\'' +
            ", accountMgrAddress='" + accountMgrAddress + '\'' +
            ", roleMgrAddress='" + roleMgrAddress + '\'' +
            ", voterMgrAddress='" + voterMgrAddress + '\'' +
            ", accounts=" + accounts +
            ", nwAdminOrg='" + nwAdminOrg + '\'' +
            ", nwAdminRole='" + nwAdminRole + '\'' +
            ", orgAdminRole='" + orgAdminRole + '\'' +
            ", subOrgBreadth=" + subOrgBreadth +
            ", subOrgDepth=" + subOrgDepth +
            '}';
    }

    public List<String> getAccounts() {
        return accounts;
    }
}
