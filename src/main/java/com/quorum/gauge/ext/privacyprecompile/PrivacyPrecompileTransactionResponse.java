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

public class PrivacyPrecompileTransactionResponse extends Response<PrivacyPrecompileTransaction> {
    public Optional<PrivacyPrecompileTransaction> getTransaction() {
        return Optional.ofNullable(getResult());
    }

    public static class ResponseDeserialiser extends JsonDeserializer<PrivacyPrecompileTransaction> {

        private ObjectReader objectReader = ObjectMapperFactory.getObjectReader();

        @Override
        public PrivacyPrecompileTransaction deserialize(
            JsonParser jsonParser, DeserializationContext deserializationContext)
            throws IOException {
            if (jsonParser.getCurrentToken() != JsonToken.VALUE_NULL) {
                return objectReader.readValue(jsonParser, PrivacyPrecompileTransaction.class);
            } else {
                return null; // null is wrapped by Optional in above getter
            }
        }
    }
}
