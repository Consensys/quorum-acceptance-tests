package com.quorum.gauge.common;

import com.quorum.gauge.common.config.WalletData;
import com.thoughtworks.gauge.datastore.DataStoreFactory;
import com.thoughtworks.gauge.execution.parameters.parsers.base.CustomParameterParser;
import gauge.messages.Spec;

public class WalletDataParameterParser extends CustomParameterParser<WalletData> {

    @Override
    protected WalletData customParse(final Class<?> aClass, final Spec.Parameter parameter) {
        final String walletName = parameter.getValue();

        final QuorumNetworkProperty props
            = (QuorumNetworkProperty) DataStoreFactory.getSuiteDataStore().get("networkProperties");

        final WalletData convertedWallet = props.getWallets().get(walletName);
        if(convertedWallet == null) {
            throw new IllegalArgumentException("Wallet " + walletName + " not found in network properties");
        }
        return convertedWallet;
    }

    @Override
    public boolean canParse(final Class<?> aClass, final Spec.Parameter parameter) {
        return aClass.equals(WalletData.class);
    }
}
