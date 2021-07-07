package com.quorum.gauge.common;

import org.web3j.protocol.core.methods.response.TransactionReceipt;

public class RawDeployedContractTarget {
    public int value;
    public QuorumNode target;
    public TransactionReceipt receipt;

    public RawDeployedContractTarget(int value, QuorumNode target, TransactionReceipt receipt) {
        this.value = value;
        this.target = target;
        this.receipt = receipt;
    }
}
