package com.quorum.gauge;

import com.quorum.gauge.common.QuorumNode;
import com.quorum.gauge.common.RetryWithDelay;
import com.quorum.gauge.core.AbstractSpecImplementation;
import com.quorum.gauge.sol.SimpleStorage;
import com.thoughtworks.gauge.Step;
import com.thoughtworks.gauge.datastore.DataStoreFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Service
public class PrivateRawSmartContractEthApi extends AbstractSpecImplementation {
    private static final Logger logger = LoggerFactory.getLogger(PrivateRawSmartContractEthApi.class);

    @Step("Deploy a simple smart contract with initial value <initialValue> signed with <apiMethod> using <source>'s default account and it's private for <target>, name this contract as <contractName>")
    public void setupContractUsingEthApi(int initialValue, String apiMethod, QuorumNode source, QuorumNode target, String contractName) {
        saveCurrentBlockNumber();
        logger.debug("Setting up contract from {} to {}", source, target);
        EthSendTransaction sendTransactionResponse = rawContractService.createRawSimplePrivateContractUsingEthApi(apiMethod, initialValue, source, target).toBlocking().first();

        Optional<String> responseError = Optional.ofNullable(sendTransactionResponse.getError()).map(Response.Error::getMessage);
        assertThat(responseError).as("EthSendTransaction error").isEmpty();

        DataStoreFactory.getSpecDataStore().put(contractName + "_transactionHash", sendTransactionResponse.getTransactionHash());
    }

    @Step("Execute <contractName>'s `set()` function with new value <newValue> signed with <apiMethod> using <source>'s default account and it's private for <target>")
    public void updateNewValueUsingEthApi(String contractName, int newValue, String apiMethod, QuorumNode source, QuorumNode target) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractName, Contract.class);
        Optional<TransactionReceipt> receipt = rawContractService.updateRawSimplePrivateContractUsingEthApi(apiMethod, newValue, c.getContractAddress(), source, target).toBlocking().first().getTransactionReceipt();

        assertThat(receipt.isPresent()).isTrue();
        assertThat(receipt.get().getTransactionHash()).isNotBlank();
        assertThat(receipt.get().getBlockNumber()).isNotEqualTo(currentBlockNumber());
    }

    @Step("Transaction Hash is returned for API signed <contractName>")
    public void verifyTransactionHash(String contractName) {
        String transactionHash = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractName + "_transactionHash", String.class);

        assertThat(transactionHash).isNotBlank();
    }

    @Step("Transaction Receipt is present in <node> for eth_signTransaction signed <contractName> from <node>'s default account")
    public void verifyTransactionReceipt(QuorumNode node, String contractName, QuorumNode source) {
        String transactionHash = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractName + "_transactionHash", String.class);
        Optional<TransactionReceipt> receipt = transactionService.getTransactionReceipt(node, transactionHash)
            .map(ethGetTransactionReceipt -> {
                if (ethGetTransactionReceipt.getTransactionReceipt().isPresent()) {
                    return ethGetTransactionReceipt;
                } else {
                    throw new RuntimeException("retry");
                }
            }).retryWhen(new RetryWithDelay(20, 3000))
            .toBlocking().first().getTransactionReceipt();

        assertThat(receipt.isPresent()).isTrue();
        assertThat(receipt.get().getBlockNumber()).isNotEqualTo(currentBlockNumber());

        String senderAddr = accountService.getDefaultAccountAddress(source).toBlocking().first();
        assertThat(receipt.get().getFrom()).isEqualTo(senderAddr);

        if (DataStoreFactory.getSpecDataStore().get(contractName) == null) {
            // create a dummy contract which is used to hold the necessary values used by the other test scenarios
            Contract c = makeDummyContract(receipt.get().getContractAddress(), receipt.get());
            DataStoreFactory.getSpecDataStore().put(contractName, c);
        }
    }

    private SimpleStorage makeDummyContract(String contractAddress, TransactionReceipt transactionReceipt) {
        SimpleStorage c = SimpleStorage.load(
            contractAddress,
            null,
            (TransactionManager) null,
            null,
            null
        );
        c.setTransactionReceipt(transactionReceipt);

        return c;
    }

}
