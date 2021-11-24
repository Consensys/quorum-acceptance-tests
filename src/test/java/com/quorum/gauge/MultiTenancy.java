package com.quorum.gauge;

import com.google.common.primitives.Ints;
import com.quorum.gauge.common.Context;
import com.quorum.gauge.common.PrivacyFlag;
import com.quorum.gauge.common.QuorumNetworkProperty;
import com.quorum.gauge.common.QuorumNetworkProperty.Node;
import com.quorum.gauge.common.QuorumNode;
import com.quorum.gauge.common.config.WalletData;
import com.quorum.gauge.core.AbstractSpecImplementation;
import com.quorum.gauge.ext.EthChainId;
import com.quorum.gauge.services.ExtensionService;
import com.thoughtworks.gauge.Step;
import com.thoughtworks.gauge.Table;
import com.thoughtworks.gauge.datastore.DataStore;
import com.thoughtworks.gauge.datastore.DataStoreFactory;
import io.reactivex.Observable;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.protocol.exceptions.TransactionException;
import org.web3j.tx.Contract;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Service
public class MultiTenancy extends AbstractSpecImplementation {

    @Autowired
    ExtensionService extensionService;

    private static final Logger logger = LoggerFactory.getLogger(MultiTenancy.class);
    // clientName -> nodes
    private final Map<String, List<String>> assignedNodes = new HashMap<>();
    // clientName -> scopes
    private final Map<String, List<String>> assignedScopes = new HashMap<>();

    @Step("Configure `<clientName>` in authorization server to access `<node>` with: <table>")
    public void setupClients(String clientName, Node node, Table scopes) {
        Set<String> nodeList = Stream.of(node).map(Node::getName).collect(Collectors.toSet());
        List<String> scopeList = scopes.getTableRows().stream()
            .map(r -> r.getCell("scope"))
            .map(s -> s.replace('`', ' '))
            .map(StringUtils::trim)
            .map(rawScope -> {
                String expandedScope = rawScope;
                for (QuorumNetworkProperty.Node n : networkProperty.getNodes().values()) {
                    for (String alias : n.getAccountAliases().keySet()) {
                        if (rawScope.contains("=" + alias)) {
                            expandedScope = StringUtils.replace(expandedScope, alias, n.getAccountAliases().get(alias).toLowerCase(Locale.ROOT));
                        }
                    }
                }
                for (String alias : networkProperty.getWallets().keySet()) {
                    if (rawScope.contains(alias)) {
                        WalletData walletData = networkProperty.getWallets().get(alias);
                        try {
                            Credentials cred = WalletUtils.loadCredentials(walletData.getWalletPass(), walletData.getWalletPath());
                            expandedScope = StringUtils.replace(expandedScope, alias, cred.getAddress().toLowerCase(Locale.ROOT));
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                logger.debug("{} -> {}", rawScope, expandedScope);
                return expandedScope;
            }).collect(Collectors.toList());
        assignedScopes.put(clientName, scopeList);
        assignedNodes.put(clientName, List.copyOf(nodeList));
        assertThat(oAuth2Service.updateOrInsert(clientName, scopeList, List.copyOf(nodeList)).blockingFirst()).isEqualTo(true);
    }

    @Step("`<clientName>` can deploy a <contractId> private contract to `<node>`, private from `<privateFrom>`, signed by `<wallet>` and private for `<privateFor>`")
    public void deployPrivateContractsUsingSelfManagedAccount(String clientName, String contractId, QuorumNode node, String privateFrom, String wallet, String privateFor) {
        Observable<Boolean> deployPrivateContract = deployPrivateContract(clientName, node, privateFrom, privateFor, contractId, networkProperty.getWallets().get(wallet), null)
            .map(c -> c.isPresent() && c.get().getTransactionReceipt().isPresent());
        assertThat(deployPrivateContract.blockingFirst()).isTrue();
    }

    @Step("`<clientName>` can __NOT__ deploy a <contractId> private contract to `<node>`, private from `<privateFrom>`, signed by `<wallet>` and private for `<privateFor>`")
    public void denyDeployPrivateContractsUsingSelfManagedAccount(String clientName, String contractId, QuorumNode node, String privateFrom, String wallet, String privateFor) {
        Observable<Boolean> deployPrivateContract = deployPrivateContract(clientName, node, privateFrom, privateFor, contractId, networkProperty.getWallets().get(wallet), null)
            .map(c -> c.isPresent() && c.get().getTransactionReceipt().isPresent());
        assertThat(deployPrivateContract.blockingFirst()).isFalse();
    }

    @Step("`<clientName>` can request for RPC modules available in `<node>`")
    public void canCallRPCModule(String clientName, QuorumNode node) {
        assertThat(oAuth2Service.requestAccessToken(clientName,
                Collections.singletonList(node.name()),
                Stream.of("rpc://rpc_modules").collect(Collectors.toList()))
            .flatMap((r) -> utilService.getRpcModules(node).onExceptionResumeNext(Observable.just(Boolean.FALSE)))
            .doOnTerminate(Context::removeAccessToken).blockingFirst()
        ).isEqualTo(true);
    }

    @Step("`<clientName>` can __NOT__ request for RPC modules available in `<node>`")
    public void canNotCallRPCModule(String clientName, QuorumNode node) {
        assertThat(oAuth2Service.requestAccessToken(clientName,
                Collections.singletonList(node.name()),
                Stream.of("rpc://rpc_modules").collect(Collectors.toList()))
            .flatMap((r) -> utilService.getRpcModules(node).onExceptionResumeNext(Observable.just(Boolean.FALSE)))
            .doOnTerminate(Context::removeAccessToken).blockingFirst()
        ).isEqualTo(false);
    }

    @Step("`<clientName>` deploys a <privacyFlag> `SimpleStorage` contract on `<node>` private from `<privateFrom>` using `<ethAccount>`, named <contractName>, private for <anotherClient>'s `<privateFor>`")
    public void deploySimpleStorageContractWithFlag(String clientName, PrivacyFlag privacyFlag, Node node, String privateFrom, String ethAccount, String contractName, String anotherClient, String privateFor) {
        List<String> privateForList = Arrays.stream(privateFor.split(",")).map(String::trim).collect(Collectors.toList());
        obtainAccessToken(clientName);
        Optional<String> accessToken = haveValue(DataStoreFactory.getScenarioDataStore(), "access_token", String.class);
        accessToken.ifPresent(Context::storeAccessToken);

        Contract contract = contractService.createSimpleContract(42, node, ethAccount, privateFrom, privateForList, List.of(privacyFlag))
            .doOnTerminate(Context::removeAccessToken)
            .blockingFirst();

        DataStoreFactory.getScenarioDataStore().put(contractName, contract);
        DataStoreFactory.getScenarioDataStore().put(contractName + "_privateFrom", privacyService.id(privateFrom));
        DataStoreFactory.getScenarioDataStore().put(contractName + "_privacyFlag", privacyFlag);
    }

    @Step("`<clientName>` deploys a <contractId> private contract, named <contractName>, by sending a transaction to `<node>` with its TM key `<privateFrom>`, signed by `<wallet>` and private for `<privateFor>`")
    public void deployPrivateContractsPrivateFor(String clientName, String contractId, String contractName, QuorumNode node, String privateFrom, String wallet, String privateFor) {
        Contract contract = deployPrivateContract(clientName, node, privateFrom, privateFor, contractId, networkProperty.getWallets().get(wallet), null)
            .doOnNext(c -> {
                if (c.isEmpty() || c.get().getTransactionReceipt().isEmpty()) {
                    throw new RuntimeException("contract not deployed");
                }
            }).blockingFirst().get();
        logger.debug("Saving contract address {} with name {}", contract.getContractAddress(), contractName);
        DataStoreFactory.getScenarioDataStore().put(contractName + "_id", contractId);
        DataStoreFactory.getScenarioDataStore().put(contractName, contract);
        DataStoreFactory.getScenarioDataStore().put(clientName + contractId + contractName, new Object[]{node, privateFrom, privateFor});
        DataStoreFactory.getScenarioDataStore().put(contractName + "_privateFrom", privacyService.id(privateFrom));
    }

    @Step("`<clientName>` can read <contractName> from <node>")
    public void clientCanReadContract(String clientName, String contractName, Node node) {
        String contractId = mustHaveValue(contractName + "_id", String.class);
        Contract c = mustHaveValue(DataStoreFactory.getScenarioDataStore(), contractName, Contract.class);
        if (!assignedScopes.containsKey(clientName)) {
            throw new IllegalArgumentException(clientName + " is not assigned any scopes");
        }
        oAuth2Service.requestAccessToken(clientName, Collections.singletonList(node.getName()), assignedScopes.get(clientName))
            .doOnNext(s -> {
                switch (contractId) {
                    case "SimpleStorage":
                        assertThat(contractService.readSimpleContractValue(node, c.getContractAddress()).blockingFirst()).isNotZero();
                        break;
                    default:
                        throw new RuntimeException("unknown contract " + contractId + " with name " + contractName);
                }
            })
            .doOnTerminate(Context::removeAccessToken)
            .blockingSubscribe();
    }

    @Step("`<clientName>` fails to read <contractName> from <node>")
    public void clientCanNotReadContract(String clientName, String contractName, Node node) {
        String contractId = mustHaveValue(contractName + "_id", String.class);
        Contract c = mustHaveValue(DataStoreFactory.getScenarioDataStore(), contractName, Contract.class);
        assertThatThrownBy(() -> requestAccessToken(clientName)
            .doOnNext(s -> {
                if ("SimpleStorage".equals(contractId)) {
                    contractService.readSimpleContractValue(node, c.getContractAddress()).blockingSubscribe();
                } else {
                    throw new RuntimeException("unknown contract " + contractId + " with name " + contractName);
                }
            })
            .doOnTerminate(Context::removeAccessToken)
            .blockingSubscribe()
        ).hasMessageContaining("not authorized");
    }

    @Step("`<clientName>` writes a new arbitrary value to <contractName> successfully by sending a transaction to `<node>` with its TM key `<privateFrom>`, signed by `<wallet>` and private for `<privateFor>`")
    public void setRawSimpleContractValue(String clientName, String contractName, QuorumNode node, String privateFrom, String wallet, String privateFor) {
        Contract c = mustHaveValue(DataStoreFactory.getScenarioDataStore(), contractName, Contract.class);
        List<String> privateForList = Arrays.stream(privateFor.split(",")).map(String::trim).collect(Collectors.toList());
        assertThat(requestAccessToken(clientName)
            .flatMap(t -> rawContractService.updateRawSimplePrivateContract(100, c.getContractAddress(), networkProperty.getWallets().get(wallet), node, privateFrom, privateForList))
            .map(Optional::of)
            .onErrorResumeNext(o -> {
                return Observable.just(Optional.empty());
            })
            .doOnTerminate(Context::removeAccessToken)
            .map(r -> r.isPresent() && r.get().isStatusOK()).blockingFirst()
        ).isTrue();
    }

    @Step("`<clientName>` writes a new arbitrary value to <contractName> successfully by sending a transaction to `<node>` with its TM key `<privateFrom>` using `<ethAccount>` and private for `<privateFor>`")
    public void setSimpleContractValue(String clientName, String contractName, Node node, String privateFrom, String ethAccount, String privateFor) {
        Contract c = mustHaveValue(DataStoreFactory.getScenarioDataStore(), contractName, Contract.class);
        final String contractId = mustHaveValue(DataStoreFactory.getScenarioDataStore(), contractName + "_id", String.class);
        List<String> privateForList = Arrays.stream(privateFor.split(",")).map(String::trim).collect(Collectors.toList());
        assertThat(requestAccessToken(clientName)
            .flatMap(t -> {
                if (contractId.startsWith("SimpleStorageDelegate")) {
                    return contractService.updateSimpleStorageDelegateContract(100, c.getContractAddress(), node, ethAccount, privateFrom, privateForList);
                } else {
                    return contractService.updateSimpleStorageContract(100, c.getContractAddress(), node, ethAccount, privateFrom, privateForList);
                }
            })
            .map(Optional::of)
            .onErrorResumeNext(o -> {
                return Observable.just(Optional.empty());
            })
            .doOnTerminate(Context::removeAccessToken)
            .map(r -> {
                if (!r.isPresent()) {
                    return false;
                }
                DataStoreFactory.getScenarioDataStore().put("LastSetArbitraryValueTXHash", r.get().getTransactionHash());
                return r.get().isStatusOK();
            }).blockingFirst()
        ).isTrue();
    }


    @Step("`<clientName>` fails to write a new arbitrary value to <contractName> by sending a transaction to `<node>` with its TM key `<privateFrom>`, signed by `<wallet>` and private for `<privateForList>`")
    public void failToSetSimpleContractValue(String clientName, String contractName, QuorumNode node, String privateFrom, String wallet, String privateFor) {
        Contract c = mustHaveValue(DataStoreFactory.getScenarioDataStore(), contractName, Contract.class);
        String contractId = mustHaveValue(DataStoreFactory.getScenarioDataStore(), contractName + "_id", String.class);
        List<String> privateForList = Arrays.stream(privateFor.split(",")).map(String::trim).collect(Collectors.toList());
        AtomicReference<Throwable> caughtException = new AtomicReference<>();
        assertThat(requestAccessToken(clientName)
            .flatMap(t -> {
                if (contractId.startsWith("SimpleStorageDelegate")) {
                    return rawContractService.updateRawSimpleDelegatePrivateContract(100, c.getContractAddress(), networkProperty.getWallets().get(wallet), node, privateFrom, privateForList);
                } else {
                    return rawContractService.updateRawSimplePrivateContract(100, c.getContractAddress(), networkProperty.getWallets().get(wallet), node, privateFrom, privateForList);
                }
            })
            .map(Optional::of)
            .doOnError(e -> {
                logger.debug("On exception: {}", e.getMessage());
                caughtException.set(e);
            })
            .onErrorResumeNext(o -> {
                return Observable.just(Optional.empty());
            })
            .doOnTerminate(Context::removeAccessToken)
            .map(r -> r.isPresent() && r.get().isStatusOK()).blockingFirst()
        ).isFalse();
        assertThat(caughtException.get()).hasMessageContaining("not authorized");
    }

    @Step("`<clientName>` invokes getFromDelgate with nonce shift <nonceShift> in <contractName> by sending a transaction to `<node>` with its TM key `<privateFrom>`, signed by `<wallet>` and private for `<privateForList>` name this transaction <transactionName>")
    public void invokeGetFromDelegate(String clientName, int nonceShift, String contractName, QuorumNode node, String privateFrom, String wallet, String privateFor, String transactionName) {
        Contract c = mustHaveValue(DataStoreFactory.getScenarioDataStore(), contractName, Contract.class);
        String contractId = mustHaveValue(DataStoreFactory.getScenarioDataStore(), contractName + "_id", String.class);
        List<String> privateForList = Arrays.stream(privateFor.split(",")).map(String::trim).collect(Collectors.toList());
        AtomicReference<Throwable> caughtException = new AtomicReference<>();
        assertThat(requestAccessToken(clientName)
            .flatMap(t -> {
                return rawContractService.invokeGetFromDelegateInSneakyWrapper(nonceShift, c.getContractAddress(), networkProperty.getWallets().get(wallet), node, privateFrom, privateForList);
            })
            .map(Optional::of)
            .doOnError(e -> {
                logger.debug("On exception: {}", e.getMessage());
                caughtException.set(e);
            })
            .onErrorResumeNext(o -> {
                return Observable.just(Optional.empty());
            })
            .doOnTerminate(Context::removeAccessToken)
            // do not check the receipt - as we won't get any (as this transaction will wait for the next transaction)
            .map(r -> true).blockingFirst()
        ).isTrue();
        assertThat(caughtException.get()).hasMessageContaining("Transaction receipt was not generated after");
        TransactionException txe = (TransactionException) caughtException.get();
        assertThat(txe.getTransactionHash()).isNotEmpty();
        DataStoreFactory.getScenarioDataStore().put(transactionName, txe.getTransactionHash().get());
    }

    @Step("`<clientName>` invokes setDelegate to <value> in <contractName> by sending a transaction to `<node>` with its TM key `<privateFrom>`, signed by `<wallet>` and private for `<privateFor>`")
    public void invokeSetDelegate(String clientName, boolean value, String contractName, QuorumNode node, String privateFrom, String wallet, String privateFor) {
        Contract c = mustHaveValue(DataStoreFactory.getScenarioDataStore(), contractName, Contract.class);
        List<String> privateForList = Arrays.stream(privateFor.split(",")).map(String::trim).collect(Collectors.toList());
        assertThat(requestAccessToken(clientName)
            .flatMap(t -> rawContractService.updateDelegateInSneakyWrapperContract(value, c.getContractAddress(), networkProperty.getWallets().get(wallet), node, privateFrom, privateForList))
            .map(Optional::of)
            .onErrorResumeNext(o -> {
                return Observable.just(Optional.empty());
            })
            .doOnTerminate(Context::removeAccessToken)
            .map(r -> r.isPresent() && r.get().isStatusOK()).blockingFirst()
        ).isTrue();
    }

    private Observable<String> requestAccessToken(String clientName) {
        if (!assignedNodes.containsKey(clientName) || !assignedScopes.containsKey(clientName)) {
            logger.warn("Client {} is not setup yet. Maybe it's intentional.", clientName);
            return Observable.just("");
        } else {
            return oAuth2Service.requestAccessToken(clientName, assignedNodes.get(clientName), assignedScopes.get(clientName));
        }
    }

    @Step("`<clientName>` invokes get in <contractName> on `<node>` and gets value <expectedValue>")
    public void invokeGetMethodOnSimpleContract(String clientName, String contractName, Node node, int expectedValue) {
        Contract c = mustHaveValue(DataStoreFactory.getScenarioDataStore(), contractName, Contract.class);

        BigInteger actualValue = requestAccessToken(clientName)
            .flatMap(t -> contractService.readSimpleContractValue(node, c.getContractAddress()))
            .doOnTerminate(Context::removeAccessToken)
            .onErrorReturn(err -> {
                if (StringUtils.equals("Empty value (0x) returned from contract", err.getMessage())) {
                    return BigInteger.ZERO;
                }
                throw new RuntimeException(err);
            })
            .blockingFirst();

        assertThat(actualValue).isEqualTo(expectedValue);
    }

    @Step("`<clientName>` checks the transaction <transactionName> on `<node>` has failed")
    public void checkTransactionStatus(String clientName, String transactionName, QuorumNode node) {
        String transactionHash = mustHaveValue(DataStoreFactory.getScenarioDataStore(), transactionName, String.class);
        requestAccessToken(clientName)
            .doOnNext(s -> {
                logger.debug("token {}", s);
                Optional<TransactionReceipt> transactionReceipt = transactionService.getTransactionReceipt(node, transactionHash).blockingFirst().getTransactionReceipt();
                assertThat(transactionReceipt).isNotEmpty();
                assertThat(transactionReceipt.get().isStatusOK()).isFalse();
            })
            .doOnTerminate(Context::removeAccessToken)
            .blockingSubscribe();
    }

    @Step("`<clientName>` checks the last arbitrary write transaction on `<node>` has `<count>` events")
    public void checkTransactionStatus(String clientName, QuorumNode node, int count) {
        String transactionHash = mustHaveValue(DataStoreFactory.getScenarioDataStore(), "LastSetArbitraryValueTXHash", String.class);
        requestAccessToken(clientName)
            .doOnNext(s -> {
                logger.debug("token {}", s);
                Optional<TransactionReceipt> transactionReceipt = transactionService.getTransactionReceipt(node, transactionHash).blockingFirst().getTransactionReceipt();
                assertThat(transactionReceipt).isNotEmpty();
                assertThat(transactionReceipt.get().isStatusOK()).isTrue();
                assertThat(transactionReceipt.get().getLogs()).hasSize(count);
            })
            .doOnTerminate(Context::removeAccessToken)
            .blockingSubscribe();
    }

    @Step("`<clientName>` deploys a <contractId> public contract, named <contractName>, by sending a transaction to `<node>`")
    public void deployPublicContract(String clientName, String contractId, String contractName, QuorumNetworkProperty.Node node) {

        Contract contract = requestAccessToken(clientName)
            .flatMap(accessToken -> rpcService.call(node, "eth_chainId", Collections.emptyList(), EthChainId.class))
            .flatMap(ethChainId -> rawContractService.createRawSimplePublicContract(42, networkProperty.getWallets().get("Wallet1"), node, ethChainId.getChainId()))
            .doOnTerminate(Context::removeAccessToken)
            .doOnNext(c -> {
                if (c == null || !c.getTransactionReceipt().isPresent()) {
                    throw new RuntimeException("contract not deployed");
                }
            })
            .blockingFirst();
        logger.debug("Saving contract address {} with name {}", contract.getContractAddress(), contractName);
        DataStoreFactory.getScenarioDataStore().put(contractName, contract);
    }

    @Step("`<clientName>` deploys a <contractId> public contract, named <contractName>, by sending a transaction to `<node>` using `<ethAccount>`")
    public void deployNodeManagedPublicContract(String clientName, String contractId, String contractName, Node node, String ethAccount) {
        Contract contract = requestAccessToken(clientName)
            .flatMap(t -> contractService.createPublicSimpleContract(42, node, ethAccount))
            .doOnTerminate(Context::removeAccessToken)
            .doOnNext(c -> {
                if (c == null || !c.getTransactionReceipt().isPresent()) {
                    throw new RuntimeException("contract not deployed");
                }
            })
            .blockingFirst();
        logger.debug("Saving contract address {} with name {}", contract.getContractAddress(), contractName);
        DataStoreFactory.getScenarioDataStore().put(contractName, contract);
    }

    @Step("`<clientName>` writes a new value <newValue> to <contractName> successfully by sending a transaction to `<node>`")
    public void setSimpleContractValue(String clientName, int newValue, String contractName, QuorumNetworkProperty.Node node) {
        long chainId = rpcService.call(node, "eth_chainId", Collections.emptyList(), EthChainId.class).blockingFirst().getChainId();

        Contract contract = mustHaveValue(DataStoreFactory.getScenarioDataStore(), contractName, Contract.class);
        assertThat(requestAccessToken(clientName)
            .flatMap(t -> rawContractService.updateRawSimplePublicContract(node, networkProperty.getWallets().get("Wallet1"), contract.getContractAddress(), newValue, chainId))
            .map(Optional::of)
            .onErrorResumeNext(o -> {
                return Observable.just(Optional.empty());
            })
            .doOnTerminate(Context::removeAccessToken)
            .map(r -> r.isPresent() && r.get().isStatusOK()).blockingFirst()
        ).isTrue();
    }

    @Step("`<clientName>` writes a new value <newValue> to <contractName> successfully by sending a transaction to `<node>` using `<ethAccount>`")
    public void setNodeManagedPublicSimpleContractValue(String clientName, int newValue, String contractName, Node node, String ethAccount) {
        Contract contract = mustHaveValue(DataStoreFactory.getScenarioDataStore(), contractName, Contract.class);
        assertThat(requestAccessToken(clientName)
            .flatMap(t -> contractService.updatePublicSimpleStorageContract(newValue, contract.getContractAddress(), node, ethAccount))
            .map(Optional::of)
            .onErrorResumeNext(o -> {
                return Observable.just(Optional.empty());
            })
            .doOnTerminate(Context::removeAccessToken)
            .map(r -> r.isPresent() && r.get().isStatusOK()).blockingFirst()
        ).isTrue();
    }

    @Step("`<clientName>` reads the value from <contractName> successfully by sending a request to `<node>` and the value returns <expectedValue>")
    public void readSimpleContractValue(String clientName, String contractName, QuorumNode node, int expectedValue) {
        Contract contract = mustHaveValue(DataStoreFactory.getScenarioDataStore(), contractName, Contract.class);
        assertThat(requestAccessToken(clientName)
            .flatMap(t -> Observable.fromCallable(() -> contractService.readSimpleContractValue(node, contract.getContractAddress())))
            .doOnTerminate(Context::removeAccessToken)
            .blockingFirst()
        ).isEqualTo(expectedValue);
    }

    @Step("`<clientName>`, initially allocated with key(s) `<namedKey>`, subscribes to log events from <contractName> in `<node>`, named this subscription as <subscriptionName>")
    public void subscribeLogs(String clientName, String namedKey, String contractName, QuorumNode node, String subscriptionName) {
        Contract c = mustHaveValue(DataStoreFactory.getScenarioDataStore(), contractName, Contract.class);
        EthFilter filter = requestAccessToken(clientName)
            .flatMap(t -> contractService.newLogFilter(node, c.getContractAddress()))
            .doOnTerminate(() -> Context.removeAccessToken())
            .blockingFirst();

        assertThat(filter.hasError()).as(Optional.ofNullable(filter.getError()).orElse(new Response.Error()).getMessage()).isFalse();
        DataStoreFactory.getScenarioDataStore().put(subscriptionName + "filter", filter.getFilterId());
        DataStoreFactory.getScenarioDataStore().put(subscriptionName + "node", node);
        DataStoreFactory.getScenarioDataStore().put(subscriptionName + "namedKey", namedKey);
        DataStoreFactory.getScenarioDataStore().put(subscriptionName + "clientName", clientName);
    }

    @Step("`<clientName>` executes <contractName>'s `deposit()` function <times> times with arbitrary id and value between original parties")
    public void depositClientReceipt(String clientName, String contractName, int times) {
        Contract c = mustHaveValue(DataStoreFactory.getScenarioDataStore(), contractName, Contract.class);
        Object[] data = mustHaveValue(DataStoreFactory.getScenarioDataStore(), clientName + "ClientReceipt" + contractName, Object[].class);
        QuorumNode node = (QuorumNode) data[0];
        String privateFrom = (String) data[1];
        String privateFor = (String) data[2];
        List<String> privateForList = Arrays.stream(privateFor.split(",")).map(String::trim).collect(Collectors.toList());
        requestAccessToken(clientName).blockingSubscribe();
        try {
            List<String> txHashes = new ArrayList<>();
            for (int i = 0; i < times; i++) {
                assertThat(rawContractService.updateRawClientReceiptPrivateContract(c.getContractAddress(), networkProperty.getWallets().get("Wallet1"), node, privateFrom, privateForList)
                    .map(Optional::of)
                    .onErrorResumeNext(o -> {
                        return Observable.just(Optional.empty());
                    })
                    .doOnNext(r -> {
                        if (r.isPresent()) {
                            txHashes.add(r.get().getTransactionHash());
                        }
                    })
                    .map(r -> r.isPresent() && r.get().isStatusOK()).blockingFirst()
                ).isTrue();
            }
            DataStoreFactory.getScenarioDataStore().put("hashes", txHashes.toArray(new String[0]));
        } finally {
            Context.removeAccessToken();
        }
    }

    @Step({"<subscriptionA> receives <expected> events", "<subscriptionA> receives <expected> error"})
    public void subscriptionCheckForEventCount(String subscriptionName, String expected) {
        BigInteger filterId = mustHaveValue(DataStoreFactory.getScenarioDataStore(), subscriptionName + "filter", BigInteger.class);
        QuorumNode node = mustHaveValue(DataStoreFactory.getScenarioDataStore(), subscriptionName + "node", QuorumNode.class);
        String namedKey = mustHaveValue(DataStoreFactory.getScenarioDataStore(), subscriptionName + "namedKey", String.class);
        String clientName = mustHaveValue(DataStoreFactory.getScenarioDataStore(), subscriptionName + "clientName", String.class);

        EthLog ethLog = requestAccessToken(clientName)
            .flatMap(t -> contractService.getFilterLogs(node, filterId))
            .doOnTerminate(Context::removeAccessToken)
            .blockingFirst();

        Integer exepctedCount = Ints.tryParse(expected);
        if (exepctedCount == null) {
            assertThat(ethLog.hasError()).isTrue();
            assertThat(ethLog.getError().getMessage()).contains(expected);
        } else {
            assertThat(ethLog.hasError()).isFalse();
            assertThat(ethLog.getLogs().size()).isEqualTo(exepctedCount);
        }
    }

    @Step("`<clientName>`, initially allocated with key(s) `<namedKey>`, sees <expectedCount> events in total from transaction receipts in `<node>`")
    public void eventsFromTransactionReceipts(String clientName, String namedKey, int expectedCount, QuorumNode node) {
        String[] txHashes = mustHaveValue(DataStoreFactory.getScenarioDataStore(), "hashes", String[].class);
        requestAccessToken(clientName)
            .doOnNext(t -> {
                int logCount = 0;
                for (String h : txHashes) {
                    EthGetTransactionReceipt txReceipt = transactionService.getTransactionReceipt(node, h).blockingFirst();
                    assertThat(txReceipt.getTransactionReceipt().isPresent()).isTrue();
                    logCount += txReceipt.getTransactionReceipt().get().getLogs().size();
                }
                assertThat(logCount).isEqualTo(expectedCount);
            })
            .doOnTerminate(Context::removeAccessToken)
            .blockingSubscribe();
    }

    @Step("`<clientName>`, initially allocated with key(s) `<namedKey>`, sees <expectedCount> events when filtering by <contractName> address in `<node>`")
    public void eventsFromGetLogs(String clientName, String namedKey, int expectedCount, String contractName, QuorumNode node) {
        Contract c = mustHaveValue(DataStoreFactory.getScenarioDataStore(), contractName, Contract.class);
        requestAccessToken(clientName)
            .doOnNext(t -> {
                EthLog ethLog = transactionService.getLogsUsingFilter(node, c.getContractAddress()).blockingFirst();

                assertThat(ethLog.getLogs().size()).isEqualTo(expectedCount);
            })
            .doOnTerminate(Context::removeAccessToken)
            .blockingSubscribe();
    }

    private Observable<Optional<Contract>> deployPrivateContract(String clientName, QuorumNode node, String privateFrom, String privateFor, String contractId, WalletData wallet, String ethAccount) {
        List<String> privateForList = Arrays.stream(privateFor.split(",")).map(String::trim).collect(Collectors.toList());
        return requestAccessToken(clientName)
            .flatMap(t -> {
                String realContractId = contractId;
                String delegateContractAddress = "";
                if (realContractId.startsWith("SimpleStorageDelegate")) {
                    realContractId = "SimpleStorageDelegate";
                    String delegateContractName = contractId.replaceAll("^.+\\((.+)\\)$", "$1");
                    Contract delegateContractInstance = mustHaveValue(DataStoreFactory.getScenarioDataStore(), delegateContractName, Contract.class);
                    delegateContractAddress = delegateContractInstance.getContractAddress();
                }

                if (realContractId.startsWith("SneakyWrapper")) {
                    realContractId = "SneakyWrapper";
                    String delegateContractName = contractId.replaceAll("^.+\\((.+)\\)$", "$1");
                    Contract delegateContractInstance = mustHaveValue(DataStoreFactory.getScenarioDataStore(), delegateContractName, Contract.class);
                    delegateContractAddress = delegateContractInstance.getContractAddress();
                }

                Node source = networkProperty.getNode(node.name());
                switch (realContractId) {
                    case "SimpleStorage":
                        if (wallet == null) {
                            return contractService.createSimpleContract(42, source, ethAccount, privateFrom, privateForList, null);
                        } else {
                            return rawContractService.createRawSimplePrivateContract(42, wallet, node, privateFrom, privateForList);
                        }
                    case "ClientReceipt":
                        if (wallet == null) {
                            return contractService.createClientReceiptPrivateSmartContract(source, ethAccount, privateFrom, privateForList);
                        } else {
                            return rawContractService.createRawClientReceiptPrivateContract(wallet, node, privateFrom, privateForList);
                        }
                    case "SimpleStorageDelegate":
                        if (wallet == null) {
                            return contractService.createSimpleDelegatePrivateContract(delegateContractAddress, source, ethAccount, privateFrom, privateForList);
                        } else {
                            return rawContractService.createRawSimpleDelegatePrivateContract(delegateContractAddress, wallet, node, privateFrom, privateForList);
                        }
                    case "SneakyWrapper":
                        if (wallet != null) {
                            return rawContractService.createRawSneakyWrapperPrivateContract(delegateContractAddress, wallet, node, privateFrom, privateForList);
                        }
                    case "ContractCodeReader":
                        if (wallet == null) {
                            return contractCodeReaderService.createPrivateContract(source, ethAccount, privateFrom, privateForList);
                        }
                    default:
                        return Observable.error(new UnsupportedOperationException(contractId + " is not supported"));
                }
            })
            .map(Optional::of)
            .doOnError(e -> logger.debug("Deploying private contract: {}", e.getMessage(), e))
            .onErrorResumeNext(o -> {
                return Observable.just(Optional.empty());
            })
            .doOnTerminate(Context::removeAccessToken);
    }

    @Step("`<clientName>` can deploy a <contractId> private contract to `<node>` using `<ethAccount>`, private from `<privateFrom>` and private for `<privateFor>`")
    public void deployPrivateContractsUsingNodeManagedAccount(String clientName, String contractId, QuorumNode node, String ethAccount, String privateFrom, String privateFor) {
        Observable<Boolean> deployPrivateContract = deployPrivateContract(clientName, node, privateFrom, privateFor, contractId, null, ethAccount)
            .map(c -> c.isPresent() && c.get().getTransactionReceipt().isPresent());
        assertThat(deployPrivateContract.blockingFirst()).isTrue();
    }

    @Step("`<clientName>` can __NOT__ deploy a <contractId> private contract to `<node>` using `<ethAccount>`, private from `<privateFrom>` and private for `<privateFor>`")
    public void denyDeployPrivateContractsUsingNodeManagedAccount(String clientName, String contractId, QuorumNode node, String ethAccount, String privateFrom, String privateFor) {
        Observable<Boolean> deployPrivateContract = deployPrivateContract(clientName, node, privateFrom, privateFor, contractId, null, ethAccount)
            .map(c -> c.isPresent() && c.get().getTransactionReceipt().isPresent());
        assertThat(deployPrivateContract.blockingFirst()).isFalse();
    }

    @Step("`<clientName>` deploys a <contractId> private contract, named <contractName>, by sending a transaction to `<node>` with its TM key `<privateFrom>` using `<ethAccount>` and private for `<privateFor>`")
    public void deployPrivateContractsPrivateForUsingNodeManagedAccount(String clientName, String contractId, String contractName, QuorumNode node, String privateFrom, String ethAccount, String privateFor) {
        Contract contract = deployPrivateContract(clientName, node, privateFrom, privateFor, contractId, null, ethAccount)
            .doOnNext(c -> {
                if (c.isEmpty() || c.get().getTransactionReceipt().isEmpty()) {
                    throw new RuntimeException("contract not deployed");
                }
            }).blockingFirst().get();
        logger.debug("Saving contract address {} with name {}", contract.getContractAddress(), contractName);
        DataStoreFactory.getScenarioDataStore().put(contractName + "_id", contractId);
        DataStoreFactory.getScenarioDataStore().put(contractName, contract);
        DataStoreFactory.getScenarioDataStore().put(clientName + contractId + contractName, new Object[]{node, privateFrom, privateFor});
    }

    @Step("`<clientName>` fails to write a new arbitrary value to <contractName> by sending a transaction to `<node>` with its TM key `<privateFrom>` using `<ethAccount>` and private for `<privateForList>`")
    public void failToSetSimpleContractValueUsingNodeDefaultAccount(String clientName, String contractName, QuorumNode node, String privateFrom, String ethAccount, String privateFor) {
        Contract c = mustHaveValue(DataStoreFactory.getScenarioDataStore(), contractName, Contract.class);
        String contractId = mustHaveValue(DataStoreFactory.getScenarioDataStore(), contractName + "_id", String.class);
        List<String> privateForList = Arrays.stream(privateFor.split(",")).map(String::trim).collect(Collectors.toList());
        AtomicReference<Throwable> caughtException = new AtomicReference<>();
        assertThat(requestAccessToken(clientName)
            .flatMap(t -> {
                Node source = networkProperty.getNode(node.name());
                if (contractId.startsWith("SimpleStorageDelegate")) {
                    return contractService.updateSimpleStorageDelegateContract(100, c.getContractAddress(), source, ethAccount, privateFrom, privateForList);
                } else {
                    return contractService.updateSimpleStorageContract(100, c.getContractAddress(), source, ethAccount, privateFrom, privateForList);
                }
            })
            .map(Optional::of)
            .doOnError(e -> {
                logger.debug("On exception: {}", e.getMessage());
                caughtException.set(e);
            })
            .onErrorResumeNext(o -> {
                return Observable.just(Optional.empty());
            })
            .doOnTerminate(Context::removeAccessToken)
            .map(r -> r.isPresent() && r.get().isStatusOK()).blockingFirst()
        ).isFalse();
        assertThat(caughtException.get()).hasMessageContaining("not authorized");
    }

    @Step("`<clientName>` requests access token from authorization server")
    public void obtainAccessToken(String clientName) {
        requestAccessToken(clientName)
            .doOnNext(token -> DataStoreFactory.getScenarioDataStore().put("access_token", token))
            .doOnTerminate(Context::removeAccessToken)
            .blockingSubscribe();
    }

    @Step("Initiate `<contractName>` extension to `<targetParty>` received by `<targetEthAccount>` in `<targetNode>` from `<sourceNode>`, private from `<sourceParty>` using `<sourceEthAccount>`")
    public void inititateContractExtension(String contractName, String targetParty, String targetEthAccount, Node targetNode, Node sourceNode, String sourceParty, String sourceEthAccount) {
        Contract existingContract = mustHaveValue(DataStoreFactory.getScenarioDataStore(), contractName, Contract.class);
        PrivacyFlag privacyFlag = mustHaveValue(DataStoreFactory.getScenarioDataStore(), contractName + "_privacyFlag", PrivacyFlag.class);
        Optional<String> accessToken = haveValue(DataStoreFactory.getScenarioDataStore(), "access_token", String.class);
        accessToken.ifPresent(Context::storeAccessToken);
        String extensionAddress = extensionService
            .initiateContractExtension(sourceNode, sourceEthAccount, sourceParty, existingContract.getContractAddress(), targetNode, targetEthAccount, targetParty, privacyFlag)
            .map(res -> {
                assertThat(res.getError()).as("failed to initiate contract extension").isNull();
                return res.getResult();
            })
            .map(txHash -> {
                Optional<TransactionReceipt> transactionReceipt = transactionService
                    .pollTransactionReceipt(sourceNode, txHash);
                assertThat(transactionReceipt.isPresent()).isTrue();
                assertThat(transactionReceipt.get().getStatus()).isEqualTo("0x1");
                return transactionReceipt.get().getContractAddress();
            })
            .doOnTerminate(Context::removeAccessToken)
            .blockingFirst();

        DataStoreFactory.getScenarioDataStore().put(contractName + "extensionAddress", extensionAddress);
        DataStoreFactory.getScenarioDataStore().put(contractName + "extendedFrom", extensionAddress);
    }

    @Step("`<targetClientName>` accepts `<contractName>` extension from `<targetNode>`, private from `<targetParty>` using `<targetEthAccount>` and private for `<soureParty>`")
    public void acceptContractExtension(String targetClientNamme, String contractName, Node targetNode, String targetParty, String targetEthAccount, String sourceParty) {
        DataStore store = DataStoreFactory.getScenarioDataStore();
        PrivacyFlag privacyFlag = mustHaveValue(store, contractName + "_privacyFlag", PrivacyFlag.class);
        String contractAddress = mustHaveValue(store, contractName + "extensionAddress", String.class);

        obtainAccessToken(targetClientNamme);
        Optional<String> accessToken = haveValue(DataStoreFactory.getScenarioDataStore(), "access_token", String.class);
        accessToken.ifPresent(Context::storeAccessToken);

        Optional<TransactionReceipt> transactionReceipt = extensionService
            .acceptExtension(targetNode, true, targetEthAccount, privacyService.id(targetParty), contractAddress, List.of(privacyService.id(sourceParty)), privacyFlag)
            .map(res -> {
                assertThat(res.getError()).isNull();
                return res.getResult();
            })
            .map(txHash -> transactionService.pollTransactionReceipt(targetNode, txHash))
            .doOnTerminate(Context::removeAccessToken)
            .blockingLast();

        assertThat(transactionReceipt.isPresent()).isTrue();
        assertThat(transactionReceipt.get().getStatus()).isEqualTo("0x1");
    }

    @Step("`<clientNames>` can read <contractName> on `<node>`")
    public void canReadContract(String clientNames, String contractName, Node node) {
        Contract existingContract = mustHaveValue(DataStoreFactory.getScenarioDataStore(), contractName, Contract.class);

        Observable.fromIterable(Arrays.stream(clientNames.split(",")).map(String::trim).collect(Collectors.toList()))
            .forEach(clientName -> {
                obtainAccessToken(clientName);
                Optional<String> accessToken = haveValue(DataStoreFactory.getScenarioDataStore(), "access_token", String.class);
                accessToken.ifPresent(Context::storeAccessToken);
                BigInteger actualValue = contractService.readSimpleContractValue(node, existingContract.getContractAddress())
                    .doOnTerminate(Context::removeAccessToken)
                    .blockingFirst();

                assertThat(actualValue).as(clientName).isNotZero();
            });
    }

    @Step("`<clientNames>` can __NOT__ read <contractName> on `<node>`")
    public void canNotReadContract(String clientNames, String contractName, Node node) {
        Contract existingContract = mustHaveValue(DataStoreFactory.getScenarioDataStore(), contractName, Contract.class);

        Observable.fromIterable(Arrays.stream(clientNames.split(",")).map(String::trim).collect(Collectors.toList()))
            .forEach(clientName -> {
                obtainAccessToken(clientName);
                Optional<String> accessToken = haveValue(DataStoreFactory.getScenarioDataStore(), "access_token", String.class);
                accessToken.ifPresent(Context::storeAccessToken);

                assertThatThrownBy(() -> contractService.readSimpleContractValue(node, existingContract.getContractAddress())
                    .doOnTerminate(Context::removeAccessToken)
                    .blockingSubscribe()
                ).as(clientName).hasMessageContaining("Empty value (0x) returned from contract");
            });
    }

    @Step("`<sourceClientName>` <expectedErr> to extend <contractName> on `<sourceNode>` private from `<sourceParty>` using `<sourceEthAccount>` to `<targetClientName>`'s `<targetParty>` on `<targetNode>` with acceptance by `<targetEthAccount>`")
    public void notAuthorizedToExtendContract(String sourceClientName, String expectedErr, String contractName, Node sourceNode, String sourceParty, String sourceEthAccount, String targetClientName, String targetParty, Node targetNode, String targetEthAccount) {
        Contract existingContract = mustHaveValue(DataStoreFactory.getScenarioDataStore(), contractName, Contract.class);
        PrivacyFlag privacyFlag = mustHaveValue(DataStoreFactory.getScenarioDataStore(), contractName + "_privacyFlag", PrivacyFlag.class);
        Optional<String> accessToken = haveValue(DataStoreFactory.getScenarioDataStore(), "access_token", String.class);
        accessToken.ifPresent(Context::storeAccessToken);
        extensionService
            .initiateContractExtension(sourceNode, sourceEthAccount, sourceParty, existingContract.getContractAddress(), targetNode, targetEthAccount, targetParty, privacyFlag)
            .doOnNext(res -> {
                assertThat(res.getError()).as("expected error to return").isNotNull();
                assertThat(res.getError().getMessage()).contains(expectedErr);
            })
            .doOnTerminate(Context::removeAccessToken)
            .blockingSubscribe();
    }

    @Step("From `<targetNode>` `<targetClientName>` <expectedErr> to accept `<contractName>` extension, private from `<targetParty>` using `<targetEthAccount>` and private for `<sourceParty>`")
    public void notAuthorizedToAcceptContractExtension(Node targetNode, String targetClientName, String expectedErr, String contractName, String targetParty, String targetEthAccount, String sourceParty) {
        DataStore store = DataStoreFactory.getScenarioDataStore();
        PrivacyFlag privacyFlag = mustHaveValue(store, contractName + "_privacyFlag", PrivacyFlag.class);
        String contractAddress = mustHaveValue(store, contractName + "extensionAddress", String.class);

        obtainAccessToken(targetClientName);
        Optional<String> accessToken = haveValue(DataStoreFactory.getScenarioDataStore(), "access_token", String.class);
        accessToken.ifPresent(Context::storeAccessToken);

        extensionService
            .acceptExtension(targetNode, true, targetEthAccount, privacyService.id(targetParty), contractAddress, List.of(privacyService.id(sourceParty)), privacyFlag)
            .doOnNext(res -> {
                assertThat(res.getError()).as("expected error to return").isNotNull();
                assertThat(res.getError().getMessage()).contains(expectedErr);
            })
            .doOnTerminate(Context::removeAccessToken)
            .blockingSubscribe();
    }

    @Step("`<clientName>` invokes ContractCodeReader(<contractName>)'s getCodeSize(<targetContractName>) on `<source>` and gets non-empty value")
    public void getCodeSize(String clientName, String contractName, String targetContractName, Node source) {
        getCodeSizeWithAssertion(clientName, contractName, targetContractName, source, actual -> assertThat(actual).isNotZero());
    }

    @Step("`<clientName>` invokes ContractCodeReader(<contractName>)'s getCodeSize(<targetContractName>) on `<source>` and gets empty value")
    public void getCodeSizeEmpty(String clientName, String contractName, String targetContractName, Node source) {
        getCodeSizeWithAssertion(clientName, contractName, targetContractName, source, actual -> assertThat(actual).isZero());
    }

    @Step("`<clientName>` invokes ContractCodeReader(<contractName>)'s getCodeSize(<targetContractName>) on `<source>` and gets <expectedMsg>")
    public void getCodeSizeError(String clientName, String contractName, String targetContractName, Node source, String expectedMsg) {
        assertThatThrownBy(() ->
            getCodeSizeWithAssertion(clientName, contractName, targetContractName, source, actual -> {
            })
        ).hasMessageContaining(expectedMsg);
    }

    private void getCodeSizeWithAssertion(String clientName, String contractName, String targetContractName, Node source, Consumer<BigInteger> assertFunc) {
        Contract reader = mustHaveValue(DataStoreFactory.getScenarioDataStore(), contractName, Contract.class);
        Contract target = mustHaveValue(DataStoreFactory.getScenarioDataStore(), targetContractName, Contract.class);

        obtainAccessToken(clientName);
        Optional<String> accessToken = haveValue(DataStoreFactory.getScenarioDataStore(), "access_token", String.class);
        accessToken.ifPresent(Context::storeAccessToken);

        BigInteger actual = contractCodeReaderService.getCodeSize(source, reader.getContractAddress(), target.getContractAddress())
            .doOnTerminate(Context::removeAccessToken)
            .blockingFirst();

        assertFunc.accept(actual);
    }

    @Step("`<clientName>` invokes ContractCodeReader(<contractName>)'s getCode(<targetContractName>) on `<source>` and gets non-empty value")
    public void getCode(String clientName, String contractName, String targetContractName, Node source) {
        getCodeWithAssertion(clientName, contractName, targetContractName, source, actual -> assertThat(actual).isNotEmpty());
    }

    @Step("`<clientName>` invokes ContractCodeReader(<contractName>)'s getCode(<targetContractName>) on `<source>` and gets empty value")
    public void getCodeEmpty(String clientName, String contractName, String targetContractName, Node source) {
        getCodeWithAssertion(clientName, contractName, targetContractName, source, actual -> assertThat(actual).isEmpty());
    }

    @Step("`<clientName>` invokes ContractCodeReader(<contractName>)'s getCode(<targetContractName>) on `<source>` and gets <expectedMsg>")
    public void getCodeError(String clientName, String contractName, String targetContractName, Node source, String expectedMsg) {
        assertThatThrownBy(() -> getCodeWithAssertion(clientName, contractName, targetContractName, source, actual -> {
        }))
            .hasMessageContaining(expectedMsg);
    }

    private void getCodeWithAssertion(String clientName, String contractName, String targetContractName, Node source, Consumer<byte[]> assertFunc) {
        Contract reader = mustHaveValue(DataStoreFactory.getScenarioDataStore(), contractName, Contract.class);
        Contract target = mustHaveValue(DataStoreFactory.getScenarioDataStore(), targetContractName, Contract.class);

        obtainAccessToken(clientName);
        Optional<String> accessToken = haveValue(DataStoreFactory.getScenarioDataStore(), "access_token", String.class);
        accessToken.ifPresent(Context::storeAccessToken);

        byte[] actual = contractCodeReaderService.getCode(source, reader.getContractAddress(), target.getContractAddress())
            .doOnTerminate(Context::removeAccessToken)
            .blockingFirst();

        assertFunc.accept(actual);
    }

    @Step("`<clientName>` invokes ContractCodeReader(<contractName>)'s getCodeHash(<targetContractName>) on `<source>` and gets non-empty value")
    public void getCodeHash(String clientName, String contractName, String targetContractName, Node source) {
        getCodeHashWithAssertion(clientName, contractName, targetContractName, source, actual -> assertThat(actual).isNotEmpty());
    }

    @Step("`<clientName>` invokes ContractCodeReader(<contractName>)'s getCodeHash(<targetContractName>) on `<source>` and gets empty value")
    public void getCodeHashEmpty(String clientName, String contractName, String targetContractName, Node source) {
        getCodeHashWithAssertion(clientName, contractName, targetContractName, source, actual -> assertThat(actual).isEmpty());
    }

    @Step("`<clientName>` invokes ContractCodeReader(<contractName>)'s getCodeHash(<targetContractName>) on `<source>` and gets <expectedMsg>")
    public void getCodeHashError(String clientName, String contractName, String targetContractName, Node source, String expectedMsg) {
        assertThatThrownBy(() ->
            getCodeHashWithAssertion(clientName, contractName, targetContractName, source, actual -> {
            })
        ).hasMessageContaining(expectedMsg);
    }

    private void getCodeHashWithAssertion(String clientName, String contractName, String targetContractName, Node source, Consumer<byte[]> assertFunc) {
        Contract reader = mustHaveValue(DataStoreFactory.getScenarioDataStore(), contractName, Contract.class);
        Contract target = mustHaveValue(DataStoreFactory.getScenarioDataStore(), targetContractName, Contract.class);

        obtainAccessToken(clientName);
        Optional<String> accessToken = haveValue(DataStoreFactory.getScenarioDataStore(), "access_token", String.class);
        accessToken.ifPresent(Context::storeAccessToken);

        byte[] actual = contractCodeReaderService.getCodeHash(source, reader.getContractAddress(), target.getContractAddress())
            .doOnTerminate(Context::removeAccessToken)
            .blockingFirst();

        assertFunc.accept(actual);
    }

    @Step("`<GS Investment>` invokes ContractCodeReader(<reader>)'s getLastCodeSize() on `<Node1>` and gets non-empty value")
    public void getLastCodeSize(String clientName, String contractName, Node source) {
        getLastCodeSizeWithAssertion(clientName, contractName, source, actual -> assertThat(actual).isNotZero());
    }

    @Step("`<GS Investment>` invokes ContractCodeReader(<reader>)'s getLastCodeSize() on `<Node1>` and gets empty value")
    public void getLastCodeSizeEmpty(String clientName, String contractName, Node source) {
        getLastCodeSizeWithAssertion(clientName, contractName, source, actual -> assertThat(actual).isZero());
    }

    private void getLastCodeSizeWithAssertion(String clientName, String contractName, Node source, Consumer<BigInteger> assertFunc) {
        Contract reader = mustHaveValue(DataStoreFactory.getScenarioDataStore(), contractName, Contract.class);

        obtainAccessToken(clientName);
        Optional<String> accessToken = haveValue(DataStoreFactory.getScenarioDataStore(), "access_token", String.class);
        accessToken.ifPresent(Context::storeAccessToken);

        BigInteger actual = contractCodeReaderService.getLastCodeSize(source, reader.getContractAddress())
            .doOnTerminate(Context::removeAccessToken)
            .blockingFirst();

        assertFunc.accept(actual);
    }

    @Step("`<clientName>` invokes ContractCodeReader(<reader>)'s setLastCodeSize(<target>) by sending a transaction to `<Node1>` with its TM key `<GS_K1>` using `<GS_ACC1>` and private for `<JPM_K1>`")
    public void setLastCodeSize(String clientName, String contractName, String targetContractName, Node source, String privateFromAlias, String ethAccountAlias, String privateForAlias) {
        Contract reader = mustHaveValue(DataStoreFactory.getScenarioDataStore(), contractName, Contract.class);
        Contract target = mustHaveValue(DataStoreFactory.getScenarioDataStore(), targetContractName, Contract.class);

        obtainAccessToken(clientName);
        Optional<String> accessToken = haveValue(DataStoreFactory.getScenarioDataStore(), "access_token", String.class);
        accessToken.ifPresent(Context::storeAccessToken);

        contractCodeReaderService.setLastCodeSize(source, reader.getContractAddress(), target.getContractAddress(), ethAccountAlias, privateFromAlias, List.of(privateForAlias))
            .doOnTerminate(Context::removeAccessToken)
            .blockingSubscribe();
    }

    @Step("`<clientName>` fails to invoke ContractCodeReader(<reader>)'s setLastCodeSize(<target>) by sending a transaction to `<Node1>` with its TM key `<GS_K1>` using `<GS_ACC1>` and private for `<JPM_K1>`")
    public void setLastCodeSizeFails(String clientName, String contractName, String targetContractName, Node source, String privateFromAlias, String ethAccountAlias, String privateForAlias) {
        Contract reader = mustHaveValue(DataStoreFactory.getScenarioDataStore(), contractName, Contract.class);
        Contract target = mustHaveValue(DataStoreFactory.getScenarioDataStore(), targetContractName, Contract.class);

        obtainAccessToken(clientName);
        Optional<String> accessToken = haveValue(DataStoreFactory.getScenarioDataStore(), "access_token", String.class);
        accessToken.ifPresent(Context::storeAccessToken);

        assertThatThrownBy(() -> contractCodeReaderService.setLastCodeSize(source, reader.getContractAddress(), target.getContractAddress(), ethAccountAlias, privateFromAlias, List.of(privateForAlias))
            .doOnTerminate(Context::removeAccessToken)
            .blockingSubscribe()
        ).hasMessageContaining("not authorized");
    }

    @Step("`<JPM Investment>` can __NOT__ deploy a <SimpleStorage> private contract to `<Node1>` targeting `<GS>` tenancy")
    public void clientTargetUnauthorizedPSI(String clientName, String contractId, QuorumNode node, String psi) {
        try {
            Observable<Boolean> deployPrivateContract = Observable.just(psi)
                .doOnNext(Context::storePSI)
                .flatMap(v -> deployPrivateContract(clientName, node, "JPM_K1", "JPM_K1", contractId, networkProperty.getWallets().get("Wallet1"), null))
                .map(c -> c.isPresent() && c.get().getTransactionReceipt().isPresent());

            assertThat(deployPrivateContract.blockingFirst()).isFalse();
        } finally {
            Context.removePSI();
        }
    }

    @Step("`<GS Investment>` gets empty contract code for <contract1> on `<Node1>`")
    public void emptyContractCode(String clientName, String contractName, Node node) {
        Contract contract = mustHaveValue(DataStoreFactory.getScenarioDataStore(), contractName, Contract.class);

        EthGetCode result = requestAccessToken(clientName)
            .flatMap(t -> contractService.getCode(node, contract.getContractAddress()))
            .doOnTerminate(Context::removeAccessToken)
            .blockingFirst();

        assertThat(result.getCode()).isEqualTo("0x");
    }

    @Step("`<JPM Settlement>` sees <5> events in total from transaction receipts in `<Node1>`")
    public void eventsFromTxReceipt(String clientName, int expectedCount, Node node) {
        String[] txHashes = mustHaveValue("hashes", String[].class);
        requestAccessToken(clientName).blockingSubscribe();
        try {
            int totalCount = 0;
            for (String txHash : txHashes) {
                Optional<TransactionReceipt> receipt = transactionService.getTransactionReceipt(node, txHash).blockingFirst().getTransactionReceipt();
                assertThat(receipt).isNotEmpty();
                assertThat(receipt.get().isStatusOK()).isTrue();
                totalCount += receipt.get().getLogs().size();
            }
            assertThat(totalCount).isEqualTo(expectedCount);
        } finally {
            Context.removeAccessToken();
        }
    }

    @Step("`<JPM Settlement>` sees <5> events when filtering by <contract1> address in `<Node2>`")
    public void eventsFromFilterLogs(String clientName, int expectedCount, String contractName, QuorumNode node) {
        Contract c = mustHaveValue(DataStoreFactory.getScenarioDataStore(), contractName, Contract.class);
        EthLog ethLog = requestAccessToken(clientName)
            .flatMap(t -> transactionService.getLogsUsingFilter(node, c.getContractAddress()))
            .doOnTerminate(Context::removeAccessToken)
            .blockingFirst();

        assertThat(ethLog.getLogs().size()).isEqualTo(expectedCount);
    }
}
