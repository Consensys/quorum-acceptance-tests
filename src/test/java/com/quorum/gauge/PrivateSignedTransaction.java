package com.quorum.gauge;

import com.quorum.gauge.common.QuorumNode;
import com.quorum.gauge.services.TransactionService;
import com.thoughtworks.gauge.Step;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Service
public class PrivateSignedTransaction {

    @Autowired
    TransactionService transactionService;

    @Step("From <from> to <to>.")
    public void sendSignedTransaction(QuorumNode from, QuorumNode to) {
        String txHash = transactionService.sendSignedTransaction(from, to);

        assertThat(txHash).isNotEmpty();
    }
}
