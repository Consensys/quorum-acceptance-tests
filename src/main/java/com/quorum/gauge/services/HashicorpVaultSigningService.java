package com.quorum.gauge.services;

import com.quorum.gauge.common.QuorumNetworkProperty;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;

import java.io.IOException;
import java.math.BigInteger;

/**
 * Client to Hashicorp Vault server and keeps track of new account details across test steps
 */
@Service
public class HashicorpVaultSigningService extends HashicorpVaultAbstractService {

    public Transaction toSign(BigInteger nonce, String from) {
        return new Transaction(from, nonce, BigInteger.ZERO, DEFAULT_GAS_LIMIT, null, null, "0x000000");
    }

}
