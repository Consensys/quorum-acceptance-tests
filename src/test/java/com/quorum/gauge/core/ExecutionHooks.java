package com.quorum.gauge.core;

import com.quorum.gauge.services.UtilService;
import com.thoughtworks.gauge.BeforeScenario;
import com.thoughtworks.gauge.datastore.DataStoreFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigInteger;

@Service
public class ExecutionHooks {
    @Autowired
    UtilService utilService;

    @BeforeScenario
    public void saveCurrentBlockNumber() {
        BigInteger currentBlockNumber = utilService.getCurrentBlockNumber().toBlocking().first().getBlockNumber();
        DataStoreFactory.getScenarioDataStore().put("blocknumber", currentBlockNumber);
    }
}
