package com.quorum.gauge.ext.privacyprecompile;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectReader;
import org.web3j.protocol.ObjectMapperFactory;
import org.web3j.protocol.core.Response;

import java.io.IOException;
import java.util.Optional;

public class PMTEnabledTransactionReceiptResponse extends Response<PMTEnabledTransactionReceipt> {

    public Optional<PMTEnabledTransactionReceipt> getTransactionReceipt() {
        return Optional.ofNullable(getResult());
    }
}
