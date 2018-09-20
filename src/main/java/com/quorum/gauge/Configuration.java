package com.quorum.gauge;


import com.quorum.gauge.common.QuorumNetworkProperty;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

import java.net.InetSocketAddress;
import java.net.Proxy;

@org.springframework.context.annotation.Configuration
public class Configuration {
    private static final Logger logger = LoggerFactory.getLogger(Configuration.class);

    @Autowired
    QuorumNetworkProperty networkProperty;

    @Bean
    public OkHttpClient okHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        if (logger.isDebugEnabled()) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor(logger::debug);
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            builder.addInterceptor(logging);
        }
        QuorumNetworkProperty.SocksProxy socksProxyConfig = networkProperty.getSocksProxy();
        if (socksProxyConfig != null) {
            logger.debug("Configured SOCKS Proxy ({}:{}) for HTTPClient", socksProxyConfig.getHost(), socksProxyConfig.getPort());
            builder.proxy(new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(socksProxyConfig.getHost(), socksProxyConfig.getPort())));
        }
        return builder.build();
    }
}
