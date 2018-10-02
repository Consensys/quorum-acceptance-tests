package com.quorum.gauge.core;

import com.quorum.gauge.services.AccountService;
import com.quorum.gauge.services.ContractService;
import com.quorum.gauge.services.TransactionService;
import com.thoughtworks.gauge.datastore.DataStoreFactory;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigInteger;

public abstract class AbstractSpecImplementation {

    @Autowired
    protected ContractService contractService;

    @Autowired
    protected TransactionService transactionService;

    @Autowired
    protected AccountService accountService;

    @Autowired
    protected OkHttpClient okHttpClient;

    protected BigInteger currentBlockNumber() {
        return (BigInteger) DataStoreFactory.getScenarioDataStore().get("blocknumber");
    }
}
