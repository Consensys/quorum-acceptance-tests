package com.quorum.gauge.services;

import com.quorum.gauge.common.QuorumNetworkProperty;
import com.quorum.gauge.ext.StringResponse;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.Request;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.Collections;
import java.util.Objects;

@Service
public class PrecompiledContractService extends AbstractService {

    private static String ADDRESS;

    public String getPrivacyContractAddress(QuorumNetworkProperty.Node node) {
        if (Objects.nonNull(ADDRESS)) {
            return ADDRESS;
        }

        ADDRESS = new Request<>(
            "eth_getPrivacyPrecompileAddress",
            Collections.emptyList(),
            connectionFactory().getWeb3jService(node),
            StringResponse.class
        ).flowable().toObservable().blockingFirst().getResult();

        return ADDRESS;
    }

    /**
     * Returns true if the to address is within the range of allowed precompiled contract addresses
      */
    public Boolean isPrecompiledContract(String to) {
        String maxPrecompileAddress = Numeric.toHexStringWithPrefixZeroPadded(new BigInteger("127"), 40);
        String addr = Numeric.prependHexPrefix(to);
        return addr.compareToIgnoreCase(maxPrecompileAddress) <= 0;
    }

}
