package com.quorum.gauge.ext.accountplugin;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class HashicorpNewAccountJson {
    @JsonAlias("SecretName")
    private String secretName;

    @JsonAlias("SecretVersion")
    private Integer secretVersion;

    private OverwriteProtection overwriteProtection;

    public String getSecretName() {
        return secretName;
    }

    public void setSecretName(String secretName) {
        this.secretName = secretName;
    }

    public Integer getSecretVersion() {
        return secretVersion;
    }

    public void setSecretVersion(Integer secretVersion) {
        this.secretVersion = secretVersion;
    }

    public OverwriteProtection getOverwriteProtection() {
        return overwriteProtection;
    }

    public void setOverwriteProtection(OverwriteProtection overwriteProtection) {
        this.overwriteProtection = overwriteProtection;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OverwriteProtection {
        private Boolean insecureDisable;

        private Integer currentVersion;

        public Boolean getInsecureDisable() {
            return insecureDisable;
        }

        public void setInsecureDisable(Boolean insecureDisable) {
            this.insecureDisable = insecureDisable;
        }

        public Integer getCurrentVersion() {
            return currentVersion;
        }

        public void setCurrentVersion(Integer currentVersion) {
            this.currentVersion = currentVersion;
        }
    }
}





