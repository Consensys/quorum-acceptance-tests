package com.quorum.gauge;

import com.quorum.gauge.common.QuorumNode;
import com.quorum.gauge.core.AbstractSpecImplementation;
import com.quorum.gauge.ext.EthGetQuorumPayload;
import com.thoughtworks.gauge.Step;
import com.thoughtworks.gauge.datastore.DataStoreFactory;
import org.springframework.stereotype.Service;
import org.web3j.tx.Contract;

import java.math.BigInteger;
import static org.assertj.core.api.Assertions.assertThat;

@Service
public class GraphQL extends AbstractSpecImplementation {
    @Step("Get block number from <node> graphql and it should equal to <snapshotName>")
    public void getCurrentBlockNumber(QuorumNode node, String snapshotName) {
        int currentBlockHeight = ((BigInteger) DataStoreFactory.getScenarioDataStore().get(snapshotName)).intValue();
        assertThat(graphQLService.getBlockNumber(node)).isEqualTo(currentBlockHeight);
    }

    @Step("Get isPrivate field for <contractName>'s contract deployment transaction using GraphQL query from <node> and it should equal to <isPrivate>")
    public void GetIsPrivate(String contractName, QuorumNode node, Boolean isPrivate) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractName, Contract.class);
        String transactionHash = c.getTransactionReceipt().orElseThrow(() -> new RuntimeException("no transaction receipt for contract")).getTransactionHash();
        assertThat(graphQLService.getIsPrivate(node, transactionHash)).isEqualTo(isPrivate);
    }

    @Step("Get privateInputData field for <contractName>'s contract deployment transaction using GraphQL query from <node> and it should be the same as eth_getQuorumPayload")
    public void GetPrivateInputData(String contractName, QuorumNode node) {
        Contract c = mustHaveValue(DataStoreFactory.getSpecDataStore(), contractName, Contract.class);
        String transactionHash = c.getTransactionReceipt().orElseThrow(() -> new RuntimeException("no transaction receipt for contract")).getTransactionHash();
        EthGetQuorumPayload payload = transactionService.getPrivateTransactionPayload(node, transactionHash).blockingFirst();
        assertThat(graphQLService.getPrivatePayload(node, transactionHash)).isEqualTo(payload.getResult());
    }
}
