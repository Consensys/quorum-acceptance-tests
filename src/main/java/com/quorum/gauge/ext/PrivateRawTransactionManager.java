package com.quorum.gauge.ext;

import com.quorum.gauge.common.PrivacyFlag;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.quorum.Quorum;
import org.web3j.rlp.RlpDecoder;
import org.web3j.rlp.RlpEncoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpString;
import org.web3j.tx.ChainId;
import org.web3j.tx.RawTransactionManager;
import org.web3j.utils.Numeric;

import java.util.List;

public class PrivateRawTransactionManager extends RawTransactionManager {
    private Credentials credentials;
    private String privateFrom;
    private List<String> privateFor;
    private List<PrivacyFlag> contractFlag;

    public PrivateRawTransactionManager(Quorum quorum,
                                        Credentials credentials,
                                        String privateFrom,
                                        List<String> privateFor,
                                        List<PrivacyFlag> contractFlag
    ) {
        super(quorum, credentials);
        this.credentials = credentials;
        this.privateFrom = privateFrom;
        this.privateFor = privateFor;
        this.contractFlag = contractFlag;
    }

    @Override
    public String sign(final RawTransaction rawTransaction) {

        byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, this.credentials);

        if (privateFor.size() > 0) {
            signedMessage = setPrivate(signedMessage);
        }

        return Numeric.toHexString(signedMessage);
    }

    private byte[] setPrivate(final byte[] signedMessage) {
        // If the byte array RLP decodes to a list of size >= 1 containing a list of size >= 3
        // then find the 3rd element from the last. If the element is a RlpString of size 1 then
        // it should be the V component from the SignatureData structure -> mark the transaction as private.
        // If any of of the above checks fails then return the original byte array.
        var rlpWrappingList = RlpDecoder.decode(signedMessage);
        var result = signedMessage;
        if (!rlpWrappingList.getValues().isEmpty()) {
            var rlpList = rlpWrappingList.getValues().get(0);
            if (rlpList instanceof RlpList) {
                var rlpListSize = ((RlpList) rlpList).getValues().size();
                if (rlpListSize > 3) {
                    var vField = ((RlpList) rlpList).getValues().get(rlpListSize - 3);
                    if (vField instanceof RlpString) {
                        if (1 == ((RlpString) vField).getBytes().length) {
                            var first = ((RlpString) vField).getBytes()[0];

                            if (first == 28) {
                                ((RlpString) vField).getBytes()[0] = 38;
                            } else {
                                ((RlpString) vField).getBytes()[0] = 37;
                            }
                            result = RlpEncoder.encode(rlpList);
                        }
                    }
                }
            }
        }
        return result;
    }
}

