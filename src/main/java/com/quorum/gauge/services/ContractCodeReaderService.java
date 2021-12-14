/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.quorum.gauge.services;

import com.quorum.gauge.common.PrivacyFlag;
import com.quorum.gauge.common.QuorumNetworkProperty;
import com.quorum.gauge.ext.PrivateClientTransactionManager;
import com.quorum.gauge.sol.ContractCodeReader;
import io.reactivex.Observable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint32;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.quorum.Quorum;
import org.web3j.tx.Contract;
import org.web3j.tx.exceptions.ContractCallException;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.quorum.gauge.sol.ContractCodeReader.FUNC_GETCODE;
import static com.quorum.gauge.sol.ContractCodeReader.FUNC_GETCODEHASH;
import static com.quorum.gauge.sol.ContractCodeReader.FUNC_GETCODESIZE;
import static com.quorum.gauge.sol.ContractCodeReader.FUNC_GETLASTCODESIZE;

@Service
public class ContractCodeReaderService extends AbstractService {

    @Autowired
    PrivacyService privacyService;

    @Autowired
    AccountService accountService;

    public Observable<? extends Contract> createPrivateContract(QuorumNetworkProperty.Node source, String ethAccountAlias, String privateFromAlias, List<String> privateForAliases) {
        Quorum client = connectionFactory().getConnection(source);

        return accountService.getAccountAddress(source, ethAccountAlias).flatMap(eoa -> {
            PrivateClientTransactionManager clientTransactionManager = new PrivateClientTransactionManager(
                client,
                eoa,
                privacyService.id(privateFromAlias),
                privateForAliases.stream().map(privacyService::id).collect(Collectors.toList()),
                List.of(PrivacyFlag.StandardPrivate)
            );
            return ContractCodeReader.deploy(client, clientTransactionManager, getPermContractDepGasProvider()).flowable().toObservable();
        });
    }

    public Observable<TransactionReceipt> setLastCodeSize(QuorumNetworkProperty.Node source, String contractAddress, String targetContractAddress, String ethAccountAlias, String privateFromAlias, List<String> privateForAliases) {
        Quorum client = connectionFactory().getConnection(source);

        return accountService.getAccountAddress(source, ethAccountAlias)
            .map(eoa -> new PrivateClientTransactionManager(
                client,
                eoa,
                privacyService.id(privateFromAlias),
                privateForAliases.stream().map(privacyService::id).collect(Collectors.toList()),
                List.of(PrivacyFlag.StandardPrivate)
            ))
            .flatMap(txManager -> ContractCodeReader.load(
                contractAddress, client, txManager, BigInteger.ZERO, DEFAULT_GAS_LIMIT).setLastCodeSize(targetContractAddress)
                .flowable().toObservable()
            );

    }

    private <T> Observable<T> execute(QuorumNetworkProperty.Node node, Function function, String contractAddress, Class<T> outputType) {
        Quorum client = connectionFactory().getConnection(node);
        return client.ethCoinbase().flowable().toObservable()
            .map(Response::getResult)
            .flatMap(address -> {
                Request<?, EthCall> req = client.ethCall(Transaction.createEthCallTransaction(address, contractAddress, FunctionEncoder.encode(function)), DefaultBlockParameterName.LATEST);
                return req.flowable().toObservable();
            })
            .map(ec -> {
                if (ec.hasError()) {
                    throw new ContractCallException(ec.getError().getMessage());
                }
                List<Type> values = FunctionReturnDecoder.decode(ec.getValue(), function.getOutputParameters());
                Type result;
                if (!values.isEmpty()) {
                    result = values.get(0);
                } else {
                    throw new ContractCallException("Empty value (0x) returned from contract");
                }
                Object value = result.getValue();
                if (outputType.isAssignableFrom(value.getClass())) {
                    return (T) value;
                } else {
                    throw new ContractCallException(
                        "Unable to convert response: " + value + " of type " + result
                            + " to expected type: " + outputType.getSimpleName());
                }
            });
    }

    public Observable<BigInteger> getCodeSize(QuorumNetworkProperty.Node node, String contractAddress, String targetContractAddress) {
        Function function = new Function(FUNC_GETCODESIZE,
            Arrays.asList(new org.web3j.abi.datatypes.Address(targetContractAddress)),
            Arrays.asList(new TypeReference<Uint32>() {}));
        return execute(node, function, contractAddress, BigInteger.class);
    }

    public Observable<byte[]> getCode(QuorumNetworkProperty.Node node, String contractAddress, String targetContractAddress) {
        Function function = new Function(FUNC_GETCODE,
            Arrays.asList(new org.web3j.abi.datatypes.Address(targetContractAddress)),
            Arrays.asList(new TypeReference<DynamicBytes>() {}));
        return execute(node, function, contractAddress, byte[].class);
    }

    public Observable<byte[]> getCodeHash(QuorumNetworkProperty.Node node, String contractAddress, String targetContractAddress) {
        Function function = new Function(FUNC_GETCODEHASH,
            Arrays.asList(new org.web3j.abi.datatypes.Address(targetContractAddress)),
            Arrays.asList(new TypeReference<Bytes32>() {}));
        return execute(node, function, contractAddress, byte[].class);
    }

    public Observable<BigInteger> getLastCodeSize(QuorumNetworkProperty.Node node, String contractAddress) {
        Function function = new Function(FUNC_GETLASTCODESIZE,
            Arrays.asList(),
            Arrays.asList(new TypeReference<Uint32>() {}));
        return execute(node, function, contractAddress, BigInteger.class);
    }
}
