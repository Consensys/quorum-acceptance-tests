package com.quorum.gauge.ext.accountplugin;

import com.fasterxml.jackson.annotation.JsonAlias;

public class HashicorpAccountConfigFileJson {
    @JsonAlias("Address")
    private String address;

    @JsonAlias("VaultAccount")
    private HashicorpNewAccountJson vaultAccount;

    @JsonAlias("Version")
    private int version;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public HashicorpNewAccountJson getVaultAccount() {
        return vaultAccount;
    }

    public void setVaultAccount(HashicorpNewAccountJson vaultAccount) {
        this.vaultAccount = vaultAccount;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }
}
