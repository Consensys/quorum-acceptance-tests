package com.quorum.gauge.services;

import org.web3j.tx.gas.ContractGasProvider;

import java.math.BigInteger;

public class PermissionContractDeployGasProvider implements ContractGasProvider {
    @Override
    public BigInteger getGasPrice(String action) {
        return BigInteger.ZERO;
    }

    @Override
    public BigInteger getGasPrice() {
        return null;
    }

    @Override
    public BigInteger getGasLimit(String action) {
        return new BigInteger("989680", 16);
    }

    @Override
    public BigInteger getGasLimit() {
        return null;
    }
}
