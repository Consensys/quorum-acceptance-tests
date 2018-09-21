package com.quorum.gauge;

import com.quorum.gauge.common.QuorumNode;
import com.quorum.gauge.services.TransactionService;
import com.thoughtworks.gauge.Step;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Service
public class ValueBasedTransaction {

    @Autowired
    TransactionService transactionService;

    @Step("Sending a public transaction from <source> to <target>.")
    public void sendSignedTransaction(QuorumNode from, QuorumNode to) {
        String txHash = transactionService.sendSignedTransaction(from, to);

        assertThat(txHash).isNotEmpty();
    }
}
