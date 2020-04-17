package com.quorum.gauge.ext.FillTx;

import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.quorum.methods.request.PrivateTransaction;

import java.util.List;

public class FillTransaction {
    String raw;
    Transaction tx;

    public FillTransaction() {
    }

    public FillTransaction(String raw, Transaction tx) {
        this.raw = raw;
        this.tx = tx;
    }

    public String getRaw() {
        return raw;
    }

    public void setRaw(String raw) {
        this.raw = raw;
    }

    public Transaction getTx() {
        return tx;
    }

    public void setTx(Transaction tx) {
        this.tx = tx;
    }

    @Override
    public String toString() {
        return "FillTransaction{" +
            "raw='" + raw + '\'' +
            ", tx=" + tx +
            '}';
    }

    public PrivateFillTransaction getPrivateTransaction(String from, List<String> privateFor) {
        PrivateFillTransaction txObject =
            new PrivateFillTransaction(
                from,
                tx.getNonce(),
                tx.getGas(),
                null,
                tx.getValue(),
                tx.getInput(),
                null,
                privateFor);

        return txObject;
    }
}
