package com.quorum.gauge.common.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WalletData {

    @JsonProperty("path")
    private String walletPath;

    @JsonProperty("pass")
    private String walletPass;

    public String getWalletPath() {
        return walletPath;
    }

    public void setWalletPath(final String walletPath) {
        this.walletPath = walletPath;
    }

    public String getWalletPass() {
        return walletPass;
    }

    public void setWalletPass(final String walletPass) {
        this.walletPass = walletPass;
    }
}
