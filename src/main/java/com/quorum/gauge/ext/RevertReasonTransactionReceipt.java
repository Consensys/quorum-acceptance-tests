package com.quorum.gauge.ext;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectReader;
import org.web3j.protocol.ObjectMapperFactory;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.io.IOException;
import java.util.Optional;

public class RevertReasonTransactionReceipt extends TransactionReceipt {

    private String revertReason;

    public String getRevertReason() {
        return revertReason;
    }

    public void setRevertReason(String revertReason) {
        this.revertReason = revertReason;
    }


    public static class EthGetRevertReasonTransactionReceipt extends Response<RevertReasonTransactionReceipt> {
        public EthGetRevertReasonTransactionReceipt() {
        }

        public Optional<RevertReasonTransactionReceipt> getTransactionReceipt() {
            return Optional.ofNullable(this.getResult());
        }

        public static class ResponseDeserialiser extends JsonDeserializer<RevertReasonTransactionReceipt> {
            private ObjectReader objectReader = ObjectMapperFactory.getObjectReader();

            public ResponseDeserialiser() {
            }

            public RevertReasonTransactionReceipt deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
                return jsonParser.getCurrentToken() != JsonToken.VALUE_NULL ? this.objectReader.readValue(jsonParser, RevertReasonTransactionReceipt.class) : null;
            }
        }
    }

}
