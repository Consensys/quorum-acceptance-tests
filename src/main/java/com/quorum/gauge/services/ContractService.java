package com.quorum.gauge.services;

import com.quorum.gauge.common.QuorumNode;
import com.quorum.gauge.ext.EthStorageRoot;
import com.quorum.gauge.sol.SimpleStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.quorum.Quorum;
import org.web3j.quorum.tx.ClientTransactionManager;
import org.web3j.tx.Contract;
import org.web3j.tx.ReadonlyTransactionManager;
import org.web3j.tx.exceptions.ContractCallException;
import rx.Observable;

import java.math.BigInteger;
import java.util.Arrays;

@Service
public class ContractService extends AbstractService {
    private static final Logger logger = LoggerFactory.getLogger(ContractService.class);

    @Autowired
    PrivacyService privacyService;

    @Autowired
    AccountService accountService;

    public Observable<? extends Contract> createSimpleContract(int initialValue, QuorumNode source, QuorumNode target) {
        Quorum client = connectionFactory.getConnection(source);
        return accountService.getDefaultAccountAddress(source).flatMap(address -> {
            ClientTransactionManager clientTransactionManager = new ClientTransactionManager(
                    client,
                    address,
                    Arrays.asList(privacyService.id(target)));
            return SimpleStorage.deploy(client,
                    clientTransactionManager,
                    BigInteger.valueOf(0),
                    DEFAULT_GAS_LIMIT,
                    BigInteger.valueOf(initialValue)).observable();
        });
    }

    // Read-only contract
    public int readSimpleContractValue(QuorumNode node, String contractAddress) {
        Quorum client = connectionFactory.getConnection(node);
        String address;
        try {
            address = client.ethCoinbase().send().getAddress();
            ReadonlyTransactionManager txManager = new ReadonlyTransactionManager(client, address);
            return SimpleStorage.load(contractAddress, client, txManager,
                    BigInteger.valueOf(0),
                    DEFAULT_GAS_LIMIT).get().send().intValue();
        } catch (ContractCallException cce) {
            if (cce.getMessage().contains("Empty value (0x)")) {
                return 0;
            }
            logger.error("readSimpleContractValue()", cce);
            throw new RuntimeException(cce);
        } catch (Exception e) {
            logger.error("readSimpleContractValue()", e);
            throw new RuntimeException(e);
        }
    }

    public Observable<TransactionReceipt> updateSimpleContract(QuorumNode source, QuorumNode target, String contractAddress, int newValue) {
        Quorum client = connectionFactory.getConnection(source);
        return accountService.getDefaultAccountAddress(source).flatMap(address -> {
            ClientTransactionManager txManager = new ClientTransactionManager(
                    client,
                    address,
                    Arrays.asList(privacyService.id(target)));
            return SimpleStorage.load(contractAddress, client, txManager,
                    BigInteger.valueOf(0),
                    new BigInteger("47b760", 16)).set(BigInteger.valueOf(newValue)).observable();
        });
    }

    public Observable<EthStorageRoot> getStorageRoot(QuorumNode node, String contractAddress) {
        Request<String, EthStorageRoot> request = new Request<>(
                "eth_storageRoot",
                Arrays.asList(contractAddress),
                connectionFactory.getWeb3jService(node),
                EthStorageRoot.class);
        return request.observable();
    }
}
