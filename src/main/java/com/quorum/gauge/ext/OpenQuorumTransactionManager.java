package com.quorum.gauge.ext;

import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jService;
import org.web3j.protocol.core.JsonRpc2_0Web3j;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.exceptions.TransactionException;
import org.web3j.quorum.Quorum;
import org.web3j.quorum.enclave.Enclave;
import org.web3j.quorum.enclave.SendResponse;
import org.web3j.quorum.tx.util.Base64Kt;
import org.web3j.rlp.RlpDecoder;
import org.web3j.rlp.RlpEncoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpString;
import org.web3j.rlp.RlpType;
import org.web3j.tx.RawTransactionManager;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.Collection;
import java.util.List;

// the QuorumTransactionManager is implemented in Kotlin - so it is by default a final class
// this is a copy of the "decompiled" version
public class OpenQuorumTransactionManager extends RawTransactionManager {

    private final Quorum web3j;
    private final Credentials credentials;
    private final String publicKey;

    private List privateFor;

    private final Enclave enclave;

    private Web3jService web3jService;


    public EthSendTransaction sendTransaction(BigInteger gasPrice, BigInteger gasLimit, String to, String data, BigInteger value) throws IOException {
        BigInteger nonce = this.getNonce();
        RawTransaction rawTransaction = RawTransaction.createTransaction(nonce, gasPrice, gasLimit, to, value, data);
        return this.signAndSend(rawTransaction);
    }

    public String getFromAddress() {
        String var10000 = this.credentials.getAddress();
        return var10000;
    }

    public final SendResponse storeRawRequest(String payload, String from, List to) {
        byte[] var10000 = Numeric.hexStringToByteArray(payload);
        String payloadBase64 = Base64Kt.encode(var10000);
        return this.enclave.storeRawRequest(payloadBase64, from, to);
    }

    public String sign(RawTransaction rawTransaction) {
        byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, this.credentials);
        Collection var3 = (Collection)this.privateFor;
        boolean var4 = false;
        if (!var3.isEmpty()) {
            signedMessage = this.setPrivate(signedMessage);
        }

        String var10000 = Numeric.toHexString(signedMessage);
        return var10000;
    }

    public EthSendTransaction signAndSend(RawTransaction rawTransaction) {
        Collection var3 = (Collection)this.privateFor;
        boolean var4 = false;
        byte[] signedMessage;
        String hexValue;
        byte[] var10000;
        if (!var3.isEmpty()) {
            var10000 = Numeric.hexStringToByteArray(rawTransaction.getData());
            hexValue = Base64Kt.encode(var10000);
            SendResponse response = this.enclave.storeRawRequest(hexValue, this.publicKey, this.privateFor);
            String responseDecoded = Numeric.toHexString(Base64Kt.decode(response.getKey()));
            RawTransaction privateTransaction = RawTransaction.createTransaction(rawTransaction.getNonce(), rawTransaction.getGasPrice(), rawTransaction.getGasLimit(), rawTransaction.getTo(), rawTransaction.getValue(), responseDecoded);
            byte[] privateMessage = TransactionEncoder.signMessage(privateTransaction, this.credentials);
            signedMessage = this.setPrivate(privateMessage);
        } else {
            var10000 = TransactionEncoder.signMessage(rawTransaction, this.credentials);
            signedMessage = var10000;
        }

        hexValue = Numeric.toHexString(signedMessage);
        Enclave var11 = this.enclave;
        return var11.sendRawRequest(hexValue, this.privateFor);
    }

    public final EthSendTransaction sendRaw(String signedTx, List to) {
        return this.enclave.sendRawRequest(signedTx, to);
    }

    private final byte[] setPrivate(byte[] message) {
        byte[] result = message;
        RlpList rlpWrappingList = RlpDecoder.decode(message);
        if (rlpWrappingList instanceof RlpList && !rlpWrappingList.getValues().isEmpty()) {
            RlpType rlpList = (RlpType)rlpWrappingList.getValues().get(0);
            if (rlpList instanceof RlpList) {
                int rlpListSize = ((RlpList)rlpList).getValues().size();
                if (rlpListSize > 3) {
                    RlpType vField = (RlpType)((RlpList)rlpList).getValues().get(rlpListSize - 3);
                    if (vField instanceof RlpString && 1 == ((RlpString)vField).getBytes().length) {
                        switch(((RlpString)vField).getBytes()[0]) {
                            case 28:
                                ((RlpString)vField).getBytes()[0] = 38;
                                break;
                            default:
                                ((RlpString)vField).getBytes()[0] = 37;
                        }

                        byte[] var10000 = RlpEncoder.encode(rlpList);
                        result = var10000;
                    }
                }
            }
        }

        return result;
    }

    @Override
    protected TransactionReceipt executeTransaction(BigInteger gasPrice, BigInteger gasLimit, String to, String data, BigInteger value) throws IOException, TransactionException {
        final TransactionReceipt receipt = super.executeTransaction(gasPrice, gasLimit, to, data, value);

        return QuorumTransactionManagerService.maybeGetPrivateTransactionReceipt(web3jService, receipt)
            .flatMap(EthGetTransactionReceipt::getTransactionReceipt)
            .orElse(receipt);
    }

    @Override
    protected TransactionReceipt executeTransaction(BigInteger gasPrice, BigInteger gasLimit, String to, String data, BigInteger value, boolean constructor) throws IOException, TransactionException {
        final TransactionReceipt receipt = super.executeTransaction(gasPrice, gasLimit, to, data, value, constructor);

        return QuorumTransactionManagerService.maybeGetPrivateTransactionReceipt(web3jService, receipt)
            .flatMap(EthGetTransactionReceipt::getTransactionReceipt)
            .orElse(receipt);
    }

    public final Quorum getWeb3j() {
        return this.web3j;
    }

    public final List getPrivateFor() {
        return this.privateFor;
    }

    public final void setPrivateFor(List var1) {
        this.privateFor = var1;
    }

    public final Enclave getEnclave() {
        return this.enclave;
    }

    public OpenQuorumTransactionManager(Quorum web3j, Credentials credentials, String publicKey, List privateFor, Enclave enclave, int attempts, long sleepDuration) {
        super((Web3j)web3j, credentials, attempts, (int)sleepDuration);
        this.web3j = web3j;
        this.credentials = credentials;
        this.publicKey = publicKey;
        this.privateFor = privateFor;
        this.enclave = enclave;

        try {
            JsonRpc2_0Web3j q = (JsonRpc2_0Web3j) web3j;
            Field f = JsonRpc2_0Web3j.class.getDeclaredField("web3jService"); //NoSuchFieldException
            f.setAccessible(true);
            this.web3jService = (Web3jService) f.get(q); //IllegalAccessException
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

   public OpenQuorumTransactionManager(Quorum web3j, Credentials credentials, String publicKey, List privateFor, Enclave enclave) {
        this(web3j, credentials, publicKey, privateFor, enclave, 40, 15000L);
    }

}
