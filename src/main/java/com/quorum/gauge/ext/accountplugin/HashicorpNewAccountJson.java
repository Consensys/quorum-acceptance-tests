package com.quorum.gauge.ext.accountplugin;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class HashicorpNewAccountJson {
    @JsonAlias("SecretEnginePath")
    private String secretEnginePath;

    @JsonAlias("SecretPath")
    private String secretPath;

    @JsonAlias("SecretVersion")
    private Integer secretVersion;

    private Boolean insecureSkipCAS;

    private Integer casValue;

    public String getSecretEnginePath() {
        return secretEnginePath;
    }

    public void setSecretEnginePath(String secretEnginePath) {
        this.secretEnginePath = secretEnginePath;
    }

    public String getSecretPath() {
        return secretPath;
    }

    public void setSecretPath(String secretPath) {
        this.secretPath = secretPath;
    }

    public Integer getSecretVersion() {
        return secretVersion;
    }

    public void setSecretVersion(Integer secretVersion) {
        this.secretVersion = secretVersion;
    }

    public Boolean isInsecureSkipCAS() {
        return insecureSkipCAS;
    }

    public void setInsecureSkipCAS(Boolean insecureSkipCAS) {
        this.insecureSkipCAS = insecureSkipCAS;
    }

    public Integer getCasValue() {
        return casValue;
    }

    public void setCasValue(Integer casValue) {
        this.casValue = casValue;
    }
}

