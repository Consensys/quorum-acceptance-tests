package com.quorum.gauge.ext;

import com.thoughtworks.gauge.datastore.DataStoreFactory;
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

    /**
     * Returns true if the to address is within the range of allowed precompiled contract addresses
     */
    static Boolean isPrecompiledContract(String to) {
        String maxPrecompileAddress = Numeric.toHexStringWithPrefixZeroPadded(new BigInteger("127"), 40);
        String addr = Numeric.prependHexPrefix(to);
        return addr.compareToIgnoreCase(maxPrecompileAddress) <= 0;
    }

    /**
     * Returns the privacy precompile contract address - either fetching from the Gauge Suite DataStore or making an API
     * call
     */
    private static String getPrivacyPrecompileAddress(Web3jService web3jService) {
        String stored = (String) DataStoreFactory.getSuiteDataStore().get("privacyPrecompileAddress");

        if (Objects.nonNull(stored)) {
            return stored;
        }

        String addr = new Request<>(
            "eth_getPrivacyPrecompileAddress",
            Collections.emptyList(),
            web3jService,
            StringResponse.class
        ).flowable().toObservable().blockingFirst().getResult();

        DataStoreFactory.getSuiteDataStore().put("privacyPrecompileAddress", addr);

        return addr;
    }

    /**
     * Fetches the corresponding private transaction receipt if the provided receipt is for a privacy marker transaction,
     * or an empty Optional otherwise.
     */
    public static Optional<EthGetTransactionReceipt> maybeGetPrivateTransactionReceipt(Web3jService web3jService, TransactionReceipt receipt) {
        if (!isPrecompiledContract(receipt.getTo())) {
            return Optional.empty();
        }

        final String privacyPrecompile = getPrivacyPrecompileAddress(web3jService);
        if(Strings.isEmpty(privacyPrecompile) || !privacyPrecompile.equalsIgnoreCase(receipt.getTo())) {
            return Optional.empty();
        }

        EthGetTransactionReceipt privateReceiptResponse = new Request<Object, EthGetTransactionReceipt>(
            "eth_getPrivateTransactionReceipt",
            List.of(receipt.getTransactionHash()),
            web3jService,
            EthGetTransactionReceipt.class
        ).flowable().toObservable().blockingFirst();

        if (privateReceiptResponse.getTransactionReceipt().isPresent()) {
            return Optional.of(privateReceiptResponse);
        } else {
            return Optional.empty();
        }
    }

}
