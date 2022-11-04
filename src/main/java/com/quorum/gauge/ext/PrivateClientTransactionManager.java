package com.quorum.gauge.ext;

import org.web3j.protocol.Web3j;
import org.web3j.quorum.PrivacyFlag;
import org.web3j.quorum.Quorum;
import org.web3j.quorum.tx.ClientTransactionManager;

import java.util.List;

public class PrivateClientTransactionManager extends ClientTransactionManager {

    public static int DEFAULT_SLEEP_DURATION_IN_MILLIS = 2000;
    public static int DEFAULT_MAX_RETRY = 60;

    public PrivateClientTransactionManager(Quorum quorum, String fromAddress, String privateFrom, List<String> privateFor) {
        super(quorum, fromAddress, privateFrom, privateFor, DEFAULT_MAX_RETRY, DEFAULT_SLEEP_DURATION_IN_MILLIS);
    }

    public PrivateClientTransactionManager(final Quorum client, final String eoa, final String id, final List<String> collect, final PrivacyFlag standardPrivate) {
        super(client, eoa, id, collect, standardPrivate, DEFAULT_MAX_RETRY, DEFAULT_SLEEP_DURATION_IN_MILLIS);
    }

    public PrivateClientTransactionManager(Quorum quorum, String fromAddress, String privateFrom, List<String> privateFor, PrivacyFlag privacyFlag, List<String> mandatoryFor) {
        super(quorum, fromAddress, privateFrom, privateFor, privacyFlag, mandatoryFor, DEFAULT_MAX_RETRY, DEFAULT_SLEEP_DURATION_IN_MILLIS);
    }
}
