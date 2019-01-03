package org.web3j.tx;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jService;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.quorum.methods.request.PrivateTransaction;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

public class QuorumRawTransactionManager extends RawTransactionManager {

    private Web3jService web3jService;

    private List<String> privateFor;

    private OkHttpClient httpClient;

    private String thirdPartyUrl;

    public QuorumRawTransactionManager(OkHttpClient httpClient, String thirdPartyUrl, Web3j web3j, Web3jService web3jService,
                                       Credentials credentials, List<String> privateFor, int attempts,
                                       int sleepDuration) {
        super(web3j, credentials, attempts, sleepDuration);
        this.web3jService = web3jService;
        this.httpClient = httpClient;
        this.privateFor = privateFor;
        this.thirdPartyUrl = thirdPartyUrl;
    }


    @Override
    public EthSendTransaction signAndSend(RawTransaction rawTransaction) throws IOException {

        if (Objects.isNull(privateFor) || privateFor.isEmpty()) {
            return super.signAndSend(rawTransaction);
        }

        if (null != privateFor && privateFor.size() > 0) {
            rawTransaction = storeRaw(rawTransaction);
        }

        byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);

        String hexValue = Numeric.toHexString(signedMessage);

        PrivateTransaction privateTransaction = new PrivateTransaction(null,
                null, null, null, null, null, null, privateFor);

        return new Request<>(
                "eth_sendRawPrivateTransaction",
                Arrays.asList(hexValue, privateTransaction),
                web3jService,
                org.web3j.protocol.core.methods.response.EthSendTransaction.class).send();
    }

    private RawTransaction storeRaw(RawTransaction rawTransaction) throws IOException {
        // store raw to the corresponding tessera
        byte[] data = Numeric.hexStringToByteArray(rawTransaction.getData());
        String dataB64 = Base64.getEncoder().encodeToString(data);
        String req = String.format("{\"payload\":\"%s\"}", dataB64);

        RequestBody requestBody = RequestBody.create(okhttp3.MediaType.parse("application/json"), req);
        okhttp3.Request request = new okhttp3.Request.Builder().url(thirdPartyUrl + "/storeraw").post(requestBody).build();
        okhttp3.Response response = httpClient.newCall(request).execute();

        String result = response.body().string();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode root = objectMapper.readTree(result);

        String keyB64 = root.get("key").asText();
        byte[] key = Base64.getDecoder().decode(keyB64);
        String hexKey = Numeric.toHexString(key);

        return RawTransaction.createTransaction(rawTransaction.getNonce(),
                rawTransaction.getGasPrice(),
                rawTransaction.getGasLimit(),
                rawTransaction.getTo(),
                rawTransaction.getValue(),
                hexKey);
    }

}
