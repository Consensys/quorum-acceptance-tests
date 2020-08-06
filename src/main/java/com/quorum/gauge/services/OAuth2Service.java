package com.quorum.gauge.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quorum.gauge.common.Context;
import io.reactivex.Observable;
import okhttp3.*;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.AccessControlException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Connect to Hydra and setup oauth clients.
 *
 * Maintain the mapping inmemory for later
 */
@Service
public class OAuth2Service extends AbstractService {
    private static final Logger logger = LoggerFactory.getLogger(OAuth2Service.class);

    @Autowired
    private OkHttpClient httpClient;

    private Map<String, ClientCredentials> clientCredentials = new HashMap<>(); // id -> credentials

    public Observable<Boolean> updateOrInsert(String clientId, List<String> scopes, List<String> audience) {
        Context.removeAccessToken();
        // check if there's an existing client
        ClientCredentials cred = clientCredentials.get(clientId);
        if (cred == null) {
            cred = new ClientCredentials();
            cred.clientId = clientId.replaceAll("\\s", "_");
            cred.clientSecret = UUID.randomUUID().toString();
            clientCredentials.put(clientId, cred);
        }
        logger.debug("Client ID={}, Client Secret={}", cred.clientId, cred.clientSecret);
        String clientAPI = oAuth2ServerProperty().getAdminEndpoint() + "/clients/" + cred.clientId;

        Request checkExistRequest = new Request.Builder()
                .url(clientAPI)
                .get()
                .build();

        StringBuilder buf = new StringBuilder();
        buf.append("{")
                .append("\"grant_types\":[\"client_credentials\"],")
                .append("\"token_endpoint_auth_method\":\"client_secret_post\",")
                .append("\"audience\":[").append(audience.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(","))).append("],")
                .append("\"client_secret\":\"").append(cred.clientSecret).append("\",")
                .append("\"scope\":\"").append(StringUtils.join(scopes, " ")).append("\"")
                .append("}");
        Request updateRequest = new Request.Builder()
                .url(clientAPI)
                .put(RequestBody.create(MediaType.parse("application/json"), buf.toString()))
                .build();

        buf = new StringBuilder();
        buf.append("{")
                .append("\"grant_types\":[\"client_credentials\"],")
                .append("\"token_endpoint_auth_method\":\"client_secret_post\",")
                .append("\"audience\":[").append(audience.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(","))).append("],")
                .append("\"client_id\":\"").append(cred.clientId).append("\",")
                .append("\"client_secret\":\"").append(cred.clientSecret).append("\",")
                .append("\"scope\":\"").append(StringUtils.join(scopes, " ")).append("\"")
                .append("}");
        Request createRequest = new Request.Builder()
                .url(oAuth2ServerProperty().getAdminEndpoint() + "/clients")
                .post(RequestBody.create(MediaType.parse("application/json"), buf.toString()))
                .build();

        ClientCredentials finalCred = cred;
        return Observable.fromCallable(() -> httpClient.newCall(checkExistRequest).execute())
                .flatMap(r -> {
                    try {
                        if (r.isSuccessful()) {
                            logger.debug("Update existing client {}", finalCred.clientId);
                            return Observable.fromCallable(() -> httpClient.newCall(updateRequest).execute());
                        } else {
                            logger.debug("Create new client {}", finalCred.clientId);
                            return Observable.fromCallable(() -> httpClient.newCall(createRequest).execute());
                        }
                    } finally {
                        r.close();
                    }
                })
                .doOnNext(Response::close)
                .map(Response::isSuccessful);
    }

    public Observable<String> requestAccessToken(String clientId, List<String> targetAud, List<String> requestScopes) {
        Context.removeAccessToken();
        ClientCredentials cred = clientCredentials.get(clientId);
        if (cred == null) {
            throw new AccessControlException(clientId + " OAuth2 Client has not been setup");
        }
        RequestBody body = new FormBody.Builder()
                .add("grant_type", "client_credentials")
                .add("client_id", cred.clientId)
                .add("client_secret", cred.clientSecret)
                .add("scope", String.join(" ", requestScopes))
                .add("audience", String.join(" ", targetAud))
                .build();
        Request req = new Request.Builder()
                .url(oAuth2ServerProperty().getClientEndpoint() + "/oauth2/token")
                .post(body)
                .build();
        return Observable.fromCallable(() -> httpClient.newCall(req).execute())
                .doOnError(e -> logger.error("Failed to retrieve access token", e))
                .map(r -> {
                    try {
                        assert r.body() != null;
                        if (!r.isSuccessful()) {
                            throw new RuntimeException("unsuccessful request: " + r.message() + ": " + r.body().string());
                        }
                        TypeReference<HashMap<String, String>> typeRef
                                = new TypeReference<HashMap<String, String>>() {};
                        Map<String, String> map = new ObjectMapper().readValue(r.body().byteStream(), typeRef);
                        return map.get("token_type") + " " + map.get("access_token");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } finally {
                        r.close();
                    }
                })
                .onExceptionResumeNext(Observable.just("no_access_token_retrieved"))
                .doOnNext(Context::storeAccessToken);
    }

    private static class ClientCredentials {
        private String clientId;
        private String clientSecret;
    }
}
