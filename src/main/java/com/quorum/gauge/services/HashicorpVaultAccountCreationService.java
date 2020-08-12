package com.quorum.gauge.services;

import com.quorum.gauge.ext.accountplugin.HashicorpNewAccountJson;
import org.springframework.stereotype.Service;

/**
 * Client to Hashicorp Vault server and keeps track of new account details across test steps
 */
@Service
public class HashicorpVaultAccountCreationService extends HashicorpVaultAbstractService {
    private HashicorpNewAccountJson newAccountJson;
    private String newAccountAddress;
    private String newAccountUrl;

    public HashicorpNewAccountJson getNewAccountJson() {
        return newAccountJson;
    }

    public void setNewAccountJson(HashicorpNewAccountJson newAccountJson) {
        this.newAccountJson = newAccountJson;
    }

    public String getNewAccountAddress() {
        return newAccountAddress;
    }

    public void setNewAccountAddress(String newAccountAddress) {
        this.newAccountAddress = newAccountAddress;
    }

    public String getNewAccountUrl() {
        return newAccountUrl;
    }

    public void setNewAccountUrl(String newAccountUrl) {
        this.newAccountUrl = newAccountUrl;
    }

    public boolean isNewAccountContextInitialised() {
        if (newAccountJson == null || newAccountAddress == null || newAccountUrl == null) {
            return false;
        }

        return !newAccountAddress.isEmpty() && !newAccountUrl.isEmpty();
    }
}
