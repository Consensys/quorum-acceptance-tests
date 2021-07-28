package com.quorum.gauge.ext;

import org.web3j.protocol.Web3jService;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Numeric;
import org.web3j.utils.Strings;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

// A service that can be used by directly by TransactionManagers, with methods that block instead of returning Observables
public class QuorumTransactionManagerService {

    private static String ADDRESS;

    /**
     * Returns true if the to address is within the range of allowed precompiled contract addresses
     */
    private static Boolean isPrecompiledContract(String to) {
        String maxPrecompileAddress = Numeric.toHexStringWithPrefixZeroPadded(new BigInteger("127"), 40);
        String addr = Numeric.prependHexPrefix(to);
        return addr.compareToIgnoreCase(maxPrecompileAddress) <= 0;
    }

    private static String getPrivacyContractAddress(Web3jService web3jService) {
        if (Objects.nonNull(ADDRESS)) {
            return ADDRESS;
        }

        ADDRESS = new Request<>(
            "eth_getPrivacyPrecompileAddress",
            Collections.emptyList(),
            web3jService,
            StringResponse.class
        ).flowable().toObservable().blockingFirst().getResult();

        return ADDRESS;
    }

    /**
     * Fetches the corresponding private transaction receipt if the provided receipt is for a privacy marker transaction
     */
    public static Optional<TransactionReceipt> maybeGetPrivateTransactionReceipt(Web3jService web3jService, TransactionReceipt receipt) {
        if (!isPrecompiledContract(receipt.getTo())) {
            return Optional.empty();
        }

        final String privacyPrecompile = getPrivacyContractAddress(web3jService);
        if(Strings.isEmpty(privacyPrecompile) || !privacyPrecompile.equalsIgnoreCase(receipt.getTo())) {
            return Optional.empty();
        }

        return new Request<Object, EthGetTransactionReceipt>(
            "eth_getPrivateTransactionReceipt",
            List.of(receipt.getTransactionHash()),
            web3jService,
            EthGetTransactionReceipt.class
        ).flowable().toObservable().blockingFirst().getTransactionReceipt();
    }

}
