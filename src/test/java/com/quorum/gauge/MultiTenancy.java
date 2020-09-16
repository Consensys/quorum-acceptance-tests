package com.quorum.gauge;

import com.google.common.primitives.Ints;
import com.quorum.gauge.common.Context;
import com.quorum.gauge.common.QuorumNetworkProperty;
import com.quorum.gauge.common.QuorumNode;
import com.quorum.gauge.common.config.WalletData;
import com.quorum.gauge.core.AbstractSpecImplementation;
import com.thoughtworks.gauge.Step;
import com.thoughtworks.gauge.Table;
import com.thoughtworks.gauge.datastore.DataStoreFactory;
import io.reactivex.Observable;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.response.EthFilter;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.tx.Contract;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@Service
public class MultiTenancy extends AbstractSpecImplementation {

    private static final Logger logger = LoggerFactory.getLogger(MultiTenancy.class);
    private Map<String, List<String>> assignedNamedKeys = new HashMap<>();

    @Step("Setup `<tenantName>` access controls restricted to `<nodes>`, assigned TM keys `<namedKeys>` and with the following scopes <table>")
    public void configureAuthorizationServer(String tenantName, String nodes, String namedKeys, Table scopes) {
        assignedNamedKeys.put(tenantName, Arrays.stream(namedKeys.split(","))
            .map(String::trim)
            .collect(Collectors.toList()));
        List<String> nodeList = Arrays.stream(nodes.split(",")).map(String::trim).map(QuorumNode::valueOf).map(QuorumNode::name).collect(Collectors.toList());
        List<String> scopeList = scopes.getTableRows().stream()
            .map(r -> r.getCell("scope"))
            .map(StringUtils::trim)
            .map(s -> StringUtils.removeStart(s, "`"))
            .map(s -> StringUtils.removeEnd(s, "`"))
            .map(StringUtils::trim)
            .map(s -> {
                String newString = s;
                for (QuorumNode node : networkProperty.getNodes().keySet()) {
                    for (String namedKey : networkProperty.getNodes().get(node).getPrivacyAddressAliases().keySet()) {
                        newString = StringUtils.replace(newString, "=" + namedKey, "=" + UriUtils.encode(privacyService.id(node, namedKey), StandardCharsets.UTF_8));
                    }
                }
                logger.debug("{} -> {}", s, newString);
                return newString;
            }).collect(Collectors.toList());
        assertThat(oAuth2Service.updateOrInsert(tenantName, scopeList, nodeList).blockingFirst()).isEqualTo(true);
    }

    @Step("`<tenantName>` can deploy a <contractId> private contract to `<node>`, private from `<privateFrom>` and private for `<privateFor>`")
    public void deployPrivateContractsUsingSelfManagedAccount(String tenantName, String contractId, QuorumNode node, String privateFrom, String privateFor) {
        Observable<Boolean> deployPrivateContract = deployPrivateContract(tenantName, node, privateFrom, privateFor, contractId, networkProperty.getWallets().get("Wallet1"))
            .map(c -> c.isPresent() && c.get().getTransactionReceipt().isPresent());
        assertThat(deployPrivateContract.blockingFirst()).isTrue();
    }

    @Step("`<tenantName>` can __NOT__ deploy a <contractId> private contract to `<node>`, private from `<privateFrom>` and private for `<privateFor>`")
    public void denyDeployPrivateContractsUsingSelfManagedAccount(String tenantName, String contractId, QuorumNode node, String privateFrom, String privateFor) {
        Observable<Boolean> deployPrivateContract = deployPrivateContract(tenantName, node, privateFrom, privateFor, contractId, networkProperty.getWallets().get("Wallet1"))
            .map(c -> c.isPresent() && c.get().getTransactionReceipt().isPresent());
        assertThat(deployPrivateContract.blockingFirst()).isFalse();
    }

    @Step("`<tenantName>` can request for RPC modules available in `<node>`")
    public void canCallRPCModule(String tenantName, QuorumNode node) {
        assertThat(oAuth2Service.requestAccessToken(tenantName,
            Collections.singletonList(node.name()),
            Stream.of("rpc://rpc_modules").collect(Collectors.toList()))
            .flatMap((r) -> utilService.getRpcModules(node).onExceptionResumeNext(Observable.just(Boolean.FALSE)))
            .doOnTerminate(Context::removeAccessToken).blockingFirst()
        ).isEqualTo(true);
    }

    @Step("`<tenantName>` can __NOT__ request for RPC modules available in `<node>`")
    public void canNotCallRPCModule(String tenantName, QuorumNode node) {
        assertThat(oAuth2Service.requestAccessToken(tenantName,
            Collections.singletonList(node.name()),
            Stream.of("rpc://rpc_modules").collect(Collectors.toList()))
            .flatMap((r) -> utilService.getRpcModules(node).onExceptionResumeNext(Observable.just(Boolean.FALSE)))
            .doOnTerminate(Context::removeAccessToken).blockingFirst()
        ).isEqualTo(false);
    }

    @Step("`<tenantName>` deploys a <contractId> private contract, named <contractName>, by sending a transaction to `<node>` with its TM key `<privateFrom>` and private for `<privateFor>`")
    public void deployPrivateContractsPrivateFor(String tenantName, String contractId, String contractName, QuorumNode node, String privateFrom, String privateFor) {
        Contract contract = deployPrivateContract(tenantName, node, privateFrom, privateFor, contractId, networkProperty.getWallets().get("Wallet1"))
            .doOnNext(c -> {
                if (c.isEmpty() || c.get().getTransactionReceipt().isEmpty()) {
                    throw new RuntimeException("contract not deployed");
                }
            }).blockingFirst().get();
        logger.debug("Saving contract address {} with name {}", contract.getContractAddress(), contractName);
        DataStoreFactory.getScenarioDataStore().put(contractName + "_id", contractId);
        DataStoreFactory.getScenarioDataStore().put(contractName, contract);
        DataStoreFactory.getScenarioDataStore().put(tenantName + contractId + contractName, new Object[]{node, privateFrom, privateFor});
    }

    @Step("`<tenantName>` writes a new arbitrary value to <contractName> successfully by sending a transaction to `<node>` with its TM key `<privateFrom>` private for `<privateFor>`")
    public void setSimpleContractValue(String tenantName, String contractName, QuorumNode node, String privateFrom, String privateFor) {
        Contract c = mustHaveValue(DataStoreFactory.getScenarioDataStore(), contractName, Contract.class);
        List<String> privateForList = Arrays.stream(privateFor.split(",")).map(String::trim).collect(Collectors.toList());
        UriComponents contractScope = UriComponentsBuilder.fromUriString("private://0x0/_/contracts")
            .queryParam("owned.eoa", "0x0")
            .queryParam("party.tm", UriUtils.encode(privacyService.id(node, privateFrom), StandardCharsets.UTF_8))
            .build();
        List<String> requestScopes = Stream.of("rpc://eth_*", contractScope.toUriString()).collect(Collectors.toList());
        assertThat(oAuth2Service.requestAccessToken(tenantName, Collections.singletonList(node.name()), requestScopes)
            .flatMap(t -> rawContractService.updateRawSimplePrivateContract(100, c.getContractAddress(), networkProperty.getWallets().get("Wallet1"), node, privateFrom, privateForList))
            .map(Optional::of)
            .onErrorResumeNext(o -> {
                return Observable.just(Optional.empty());
            })
            .doOnTerminate(Context::removeAccessToken)
            .map(r -> r.isPresent() && r.get().isStatusOK()).blockingFirst()
        ).isTrue();
    }

    @Step("`<tenantName>` fails to write a new arbitrary value to <contractName> by sending a transaction to `<node>` with its TM key `<privateFrom>` and private for `<privateForList>`")
    public void failToSetSimpleContractValue(String tenantName, String contractName, QuorumNode node, String privateFrom, String privateFor) {
        Contract c = mustHaveValue(DataStoreFactory.getScenarioDataStore(), contractName, Contract.class);
        String contractId = mustHaveValue(DataStoreFactory.getScenarioDataStore(), contractName + "_id", String.class);
        List<String> privateForList = Arrays.stream(privateFor.split(",")).map(String::trim).collect(Collectors.toList());
        UriComponents contractScope = UriComponentsBuilder.fromUriString("private://0x0/_/contracts")
            .queryParam("owned.eoa", "0x0")
            .queryParam("party.tm", UriUtils.encode(privacyService.id(node, privateFrom), StandardCharsets.UTF_8))
            .build();
        List<String> requestScopes = Stream.of("rpc://eth_*", contractScope.toUriString()).collect(Collectors.toList());
        AtomicReference<Throwable> caughtException = new AtomicReference<>();
        assertThat(oAuth2Service.requestAccessToken(tenantName, Collections.singletonList(node.name()), requestScopes)
            .flatMap(t -> {
                if (contractId.startsWith("SimpleStorageDelegate")) {
                    return rawContractService.updateRawSimpleDelegatePrivateContract(100, c.getContractAddress(), networkProperty.getWallets().get("Wallet1"), node, privateFrom, privateForList);
                } else {
                    return rawContractService.updateRawSimplePrivateContract(100, c.getContractAddress(), networkProperty.getWallets().get("Wallet1"), node, privateFrom, privateForList);
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

    @Step("`<tenantName>` deploys a <contractId> public contract, named <contractName>, by sending a transaction to `<node>`")
    public void deployPublicContract(String tenantName, String contractId, String contractName, QuorumNode node) {
        // only need to request access to eth_* apis
        List<String> requestScopes = Stream.of("rpc://eth_*").collect(Collectors.toList());
        Contract contract = oAuth2Service.requestAccessToken(tenantName, Collections.singletonList(node.name()), requestScopes)
            .flatMap(t -> rawContractService.createRawSimplePublicContract(42, networkProperty.getWallets().get("Wallet1"), node))
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

    @Step("`<tenantName>` writes a new value <newValue> to <contractName> successfully by sending a transaction to `<node>`")
    public void setSimpleContractValue(String tenantName, int newValue, String contractName, QuorumNode node) {
        Contract contract = mustHaveValue(DataStoreFactory.getScenarioDataStore(), contractName, Contract.class);
        // only need to request access to eth_* apis
        List<String> requestScopes = Stream.of("rpc://eth_*").collect(Collectors.toList());
        assertThat(oAuth2Service.requestAccessToken(tenantName, Collections.singletonList(node.name()), requestScopes)
            .flatMap(t -> rawContractService.updateRawSimplePublicContract(node, networkProperty.getWallets().get("Wallet1"), contract.getContractAddress(), newValue))
            .map(Optional::of)
            .onErrorResumeNext(o -> {
                return Observable.just(Optional.empty());
            })
            .doOnTerminate(Context::removeAccessToken)
            .map(r -> r.isPresent() && r.get().isStatusOK()).blockingFirst()
        ).isTrue();
    }

    @Step("`<tenantName>` reads the value from <contractName> successfully by sending a request to `<node>` and the value returns <expectedValue>")
    public void readSimpleContractValue(String tenantName, String contractName, QuorumNode node, int expectedValue) {
        Contract contract = mustHaveValue(DataStoreFactory.getScenarioDataStore(), contractName, Contract.class);
        // only need to request access to eth_* apis
        List<String> requestScopes = Stream.of("rpc://eth_*").collect(Collectors.toList());
        assertThat(oAuth2Service.requestAccessToken(tenantName, Collections.singletonList(node.name()), requestScopes)
            .flatMap(t -> Observable.fromCallable(() -> contractService.readSimpleContractValue(node, contract.getContractAddress())))
            .doOnTerminate(Context::removeAccessToken)
            .blockingFirst()
        ).isEqualTo(expectedValue);
    }

    @Step("`<tenantName>`, initially allocated with key(s) `<namedKey>`, subscribes to log events from <contractName> in `<node>`, named this subscription as <subscriptionName>")
    public void subscribeLogs(String tenantName, String namedKey, String contractName, QuorumNode node, String subscriptionName) {
        Contract c = mustHaveValue(DataStoreFactory.getScenarioDataStore(), contractName, Contract.class);

        List<String> requestScopes = Stream.concat(Stream.of("rpc://eth_*"),
            Arrays.stream(namedKey.split(",")).map(String::trim).distinct()
                .map(k -> UriComponentsBuilder.fromUriString("private://0x0/_/contracts")
                    .queryParam("owned.eoa", "0x0")
                    .queryParam("party.tm", UriUtils.encode(privacyService.id(node, k), StandardCharsets.UTF_8))
                    .build())
                .map(UriComponents::toUriString))
            .collect(Collectors.toList());
        EthFilter filter = oAuth2Service
            .requestAccessToken(tenantName, Collections.singletonList(node.name()), requestScopes)
            .flatMap(t -> contractService.newLogFilter(node, c.getContractAddress()))
            .doOnTerminate(() -> Context.removeAccessToken())
            .blockingFirst();

        assertThat(filter.hasError()).as(Optional.ofNullable(filter.getError()).orElse(new Response.Error()).getMessage()).isFalse();
        DataStoreFactory.getScenarioDataStore().put(subscriptionName + "filter", filter.getFilterId());
        DataStoreFactory.getScenarioDataStore().put(subscriptionName + "node", node);
        DataStoreFactory.getScenarioDataStore().put(subscriptionName + "namedKey", namedKey);
        DataStoreFactory.getScenarioDataStore().put(subscriptionName + "tenantName", tenantName);
    }

    @Step("`<tenantName>` executes <contractName>'s `deposit()` function <times> times with arbitrary id and value between original parties")
    public void depositClientReceipt(String tenantName, String contractName, int times) {
        Contract c = mustHaveValue(DataStoreFactory.getScenarioDataStore(), contractName, Contract.class);
        Object[] data = mustHaveValue(DataStoreFactory.getScenarioDataStore(), tenantName + "ClientReceipt" + contractName, Object[].class);
        QuorumNode node = (QuorumNode) data[0];
        String privateFrom = (String) data[1];
        String privateFor = (String) data[2];
        List<String> privateForList = Arrays.stream(privateFor.split(",")).map(String::trim).collect(Collectors.toList());
        UriComponents writeContractScope = UriComponentsBuilder.fromUriString("private://0x0/_/contracts")
            .queryParam("owned.eoa", "0x0")
            .queryParam("party.tm", UriUtils.encode(privacyService.id(node, privateFrom), StandardCharsets.UTF_8))
            .build();
        List<String> requestScopes = Stream.of("rpc://eth_*", writeContractScope.toUriString()).collect(Collectors.toList());
        oAuth2Service.requestAccessToken(tenantName, Collections.singletonList(node.name()), requestScopes).blockingFirst();
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
        String tenantName = mustHaveValue(DataStoreFactory.getScenarioDataStore(), subscriptionName + "tenantName", String.class);

        List<String> requestScopes = Stream.concat(Stream.of("rpc://eth_*"),
            Arrays.stream(namedKey.split(",")).map(String::trim).distinct()
                .map(k -> UriComponentsBuilder.fromUriString("private://0x0/_/contracts")
                    .queryParam("owned.eoa", "0x0")
                    .queryParam("party.tm", UriUtils.encode(privacyService.id(node, k), StandardCharsets.UTF_8))
                    .build())
                .map(UriComponents::toUriString))
            .collect(Collectors.toList());
        EthLog ethLog = oAuth2Service
            .requestAccessToken(tenantName, Collections.singletonList(node.name()), requestScopes)
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

    @Step("`<tenantName>`, initially allocated with key(s) `<namedKey>`, sees <expectedCount> events in total from transaction receipts in `<node>`")
    public void eventsFromTransactionReceipts(String tenantName, String namedKey, int expectedCount, QuorumNode node) {
        String[] txHashes = mustHaveValue(DataStoreFactory.getScenarioDataStore(), "hashes", String[].class);
        UriComponents contractScope = UriComponentsBuilder.fromUriString("private://0x0/_/contracts")
            .queryParam("owned.eoa", "0x0")
            .queryParam("party.tm", UriUtils.encode(privacyService.id(node, namedKey), StandardCharsets.UTF_8))
            .build();
        List<String> requestScopes = Stream.of("rpc://eth_*", contractScope.toUriString()).collect(Collectors.toList());
        oAuth2Service
            .requestAccessToken(tenantName, Collections.singletonList(node.name()), requestScopes)
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
            .blockingFirst();
    }

    @Step("`<tenantName>`, initially allocated with key(s) `<namedKey>`, sees <expectedCount> events when filtering by <contractName> address in `<node>`")
    public void eventsFromGetLogs(String tenantName, String namedKey, int expectedCount, String contractName, QuorumNode node) {
        Contract c = mustHaveValue(DataStoreFactory.getScenarioDataStore(), contractName, Contract.class);
        UriComponents contractScope = UriComponentsBuilder.fromUriString("private://0x0/_/contracts")
            .queryParam("owned.eoa", "0x0")
            .queryParam("party.tm", UriUtils.encode(privacyService.id(node, namedKey), StandardCharsets.UTF_8))
            .build();
        List<String> requestScopes = Stream.of("rpc://eth_*", contractScope.toUriString()).collect(Collectors.toList());
        oAuth2Service
            .requestAccessToken(tenantName, Collections.singletonList(node.name()), requestScopes)
            .doOnNext(t -> {
                EthLog ethLog = transactionService.getLogsUsingFilter(node, c.getContractAddress()).blockingFirst();

                assertThat(ethLog.getLogs().size()).isEqualTo(expectedCount);
            })
            .doOnTerminate(Context::removeAccessToken)
            .blockingFirst();
    }

    @Step("`<tenantName>`, initially allocated with key(s) `<namedKey>`, sees <expectedErrorMsg> when retrieving logs from transaction receipts in `<node>`")
    public void exceptionMessageFromTransactionReceipts(String tenantName, String namedKey, String expectedErrorMsg, QuorumNode node) {
        String[] txHashes = mustHaveValue(DataStoreFactory.getScenarioDataStore(), "hashes", String[].class);
        UriComponents contractScope = UriComponentsBuilder.fromUriString("private://0x0/_/contracts")
            .queryParam("owned.eoa", "0x0")
            .queryParam("party.tm", UriUtils.encode(privacyService.id(node, namedKey), StandardCharsets.UTF_8))
            .build();
        List<String> requestScopes = Stream.of("rpc://eth_*", contractScope.toUriString()).collect(Collectors.toList());
        EthGetTransactionReceipt ethTxReceipt = oAuth2Service
            .requestAccessToken(tenantName, Collections.singletonList(node.name()), requestScopes)
            .flatMap(t -> transactionService.getTransactionReceipt(node, txHashes[0]))
            .doOnTerminate(Context::removeAccessToken)
            .blockingFirst();

        assertThat(ethTxReceipt.hasError()).isTrue();
        assertThat(ethTxReceipt.getError().getMessage()).contains(expectedErrorMsg);
    }

    @Step("`<tenantName>`, initially allocated with key(s) `<namedKey>`, sees <expectedErrorMsg> when filtering logs by <contractName> address in `<node>`")
    public void exceptionMessageFromGetLogs(String tenantName, String namedKey, String expectedErrorMsg, String contractName, QuorumNode node) {
        Contract c = mustHaveValue(DataStoreFactory.getScenarioDataStore(), contractName, Contract.class);
        UriComponents contractScope = UriComponentsBuilder.fromUriString("private://0x0/_/contracts")
            .queryParam("owned.eoa", "0x0")
            .queryParam("party.tm", UriUtils.encode(privacyService.id(node, namedKey), StandardCharsets.UTF_8))
            .build();
        List<String> requestScopes = Stream.of("rpc://eth_*", contractScope.toUriString()).collect(Collectors.toList());
        EthLog ethLog = oAuth2Service
            .requestAccessToken(tenantName, Collections.singletonList(node.name()), requestScopes)
            .flatMap(t -> transactionService.getLogsUsingFilter(node, c.getContractAddress()))
            .doOnTerminate(Context::removeAccessToken)
            .blockingFirst();

        assertThat(ethLog.hasError()).isTrue();
        assertThat(ethLog.getError().getMessage()).contains(expectedErrorMsg);
    }

    private Observable<Optional<Contract>> deployPrivateContract(String tenantName, QuorumNode node, String privateFrom, String privateFor, String contractId, WalletData wallet) {
        List<String> privateForList = Arrays.stream(privateFor.split(",")).map(String::trim).collect(Collectors.toList());
        if (!assignedNamedKeys.containsKey(tenantName)) {
            throw new IllegalArgumentException(tenantName + " has no assigned TM keys");
        }
        List<String> requestScopes = Stream.concat(
            Stream.of("rpc://eth_*"),
            assignedNamedKeys.get(tenantName).stream().map(k -> UriComponentsBuilder.fromUriString("private://0x0/_/contracts")
                .queryParam("owned.eoa", "0x0")
                .queryParam("party.tm", UriUtils.encode(privacyService.id(node, k), StandardCharsets.UTF_8))
                .build()).map(UriComponents::toUriString)
        ).collect(Collectors.toList());
        return oAuth2Service.requestAccessToken(tenantName, Collections.singletonList(node.name()), requestScopes)
            .flatMap(t -> {
                String realContractId = contractId;
                String delegateContractAddress = "";
                if (realContractId.startsWith("SimpleStorageDelegate")) {
                    realContractId = "SimpleStorageDelegate";
                    String delegateContractName = contractId.replaceAll("^.+\\((.+)\\)$", "$1");
                    Contract delegateContractInstance = mustHaveValue(DataStoreFactory.getScenarioDataStore(), delegateContractName, Contract.class);
                    delegateContractAddress = delegateContractInstance.getContractAddress();
                }
                QuorumNetworkProperty.Node source = networkProperty.getNode(node.name());
                switch (realContractId) {
                    case "SimpleStorage":
                        if (wallet == null) {
                            return contractService.createSimpleContract(42, source, privateFrom, privateForList, null);
                        } else {
                            return rawContractService.createRawSimplePrivateContract(42, wallet, node, privateFrom, privateForList);
                        }
                    case "ClientReceipt":
                        if (wallet == null) {
                            return contractService.createClientReceiptPrivateSmartContract(source, privateFrom, privateForList);
                        } else {
                            return rawContractService.createRawClientReceiptPrivateContract(wallet, node, privateFrom, privateForList);
                        }
                    case "SimpleStorageDelegate":
                        if (wallet == null) {
                            return contractService.createSimpleDelegatePrivateContract(delegateContractAddress, source, privateFrom, privateForList);
                        } else {
                            return rawContractService.createRawSimpleDelegatePrivateContract(delegateContractAddress, wallet, node, privateFrom, privateForList);
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

    @Step("`<tenantName>` can deploy a <contractId> private contract to `<node>` using node's default account, private from `<privateFrom>` and private for `<privateFor>`")
    public void deployPrivateContractsUsingNodeManagedAccount(String tenantName, String contractId, QuorumNode node, String privateFrom, String privateFor) {
        Observable<Boolean> deployPrivateContract = deployPrivateContract(tenantName, node, privateFrom, privateFor, contractId, null)
            .map( c -> c.isPresent() && c.get().getTransactionReceipt().isPresent());
        assertThat(deployPrivateContract.blockingFirst()).isTrue();
    }

    @Step("`<tenantName>` can __NOT__ deploy a <contractId> private contract to `<node>` using node's default account, private from `<privateFrom>` and private for `<privateFor>`")
    public void denyDeployPrivateContractsUsingNodeManagedAccount(String tenantName, String contractId, QuorumNode node, String privateFrom, String privateFor) {
        Observable<Boolean> deployPrivateContract = deployPrivateContract(tenantName, node, privateFrom, privateFor, contractId, null)
            .map(c -> c.isPresent() && c.get().getTransactionReceipt().isPresent());
        assertThat(deployPrivateContract.blockingFirst()).isFalse();
    }

    @Step("`<tenantName>` deploys a <contractId> private contract, named <contractName>, by sending a transaction to `<node>` with its TM key `<privateFrom>` using node's default account and private for `<privateFor>`")
    public void deployPrivateContractsPrivateForUsingNodeManagedAccount(String tenantName, String contractId, String contractName, QuorumNode node, String privateFrom, String privateFor) {
        Contract contract = deployPrivateContract(tenantName, node, privateFrom, privateFor, contractId, null)
            .doOnNext(c -> {
                if (c.isEmpty() || c.get().getTransactionReceipt().isEmpty()) {
                    throw new RuntimeException("contract not deployed");
                }
            }).blockingFirst().get();
        logger.debug("Saving contract address {} with name {}", contract.getContractAddress(), contractName);
        DataStoreFactory.getScenarioDataStore().put(contractName + "_id", contractId);
        DataStoreFactory.getScenarioDataStore().put(contractName, contract);
        DataStoreFactory.getScenarioDataStore().put(tenantName + contractId + contractName, new Object[]{node, privateFrom, privateFor});
    }

    @Step("`<tenantName>` fails to write a new arbitrary value to <contractName> by sending a transaction to `<node>` with its TM key `<privateFrom>` using node's default account and private for `<privateForList>`")
    public void failToSetSimpleContractValueUsingNodeDefaultAccount(String tenantName, String contractName, QuorumNode node, String privateFrom, String privateFor) {
        Contract c = mustHaveValue(DataStoreFactory.getScenarioDataStore(), contractName, Contract.class);
        String contractId = mustHaveValue(DataStoreFactory.getScenarioDataStore(), contractName + "_id", String.class);
        List<String> privateForList = Arrays.stream(privateFor.split(",")).map(String::trim).collect(Collectors.toList());
        UriComponents contractScope = UriComponentsBuilder.fromUriString("private://0x0/_/contracts")
            .queryParam("owned.eoa", "0x0")
            .queryParam("party.tm", UriUtils.encode(privacyService.id(node, privateFrom), StandardCharsets.UTF_8))
            .build();
        List<String> requestScopes = Stream.of("rpc://eth_*", contractScope.toUriString()).collect(Collectors.toList());
        AtomicReference<Throwable> caughtException = new AtomicReference<>();
        assertThat(oAuth2Service.requestAccessToken(tenantName, Collections.singletonList(node.name()), requestScopes)
            .flatMap(t -> {
                QuorumNetworkProperty.Node source = networkProperty.getNode(node.name());
                if (contractId.startsWith("SimpleStorageDelegate")) {
                    return contractService.updateSimpleStorageDelegateContract(100, c.getContractAddress(), source, privateFrom, privateForList);
                } else {
                    return contractService.updateSimpleStorageContract(100, c.getContractAddress(), source, privateFrom, privateForList);
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
}
