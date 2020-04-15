package com.quorum.gauge.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quorum.gauge.common.QuorumNetworkProperty;
import com.quorum.gauge.common.QuorumNode;
import io.reactivex.Observable;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.InputStream;
import java.util.Map;

@Service
public class GraphQLService extends AbstractService {
    private static final Logger logger = LoggerFactory.getLogger(GraphQLService.class);

    @Autowired
    OkHttpClient httpClient;

    public Observable<Integer> getBlockNumber(QuorumNode node) {
        String query = "{ \"query\": \"{ block { number } }\" }";
        Map<String, Object> jsonObject = executeGraphQL(node, query);
        if (jsonObject != null) {
            int blockNumber = Integer.decode(((Map<String, Object>)((Map<String, Object>)jsonObject.get("data")).get("block")).get("number").toString());
            logger.debug("GraphQL block number response: " + blockNumber);
            return Observable.just(blockNumber);
        }
        logger.debug("Invalid GraphQL block number response");
        return Observable.just(-1);
    }

    public Observable<Boolean> getIsPrivate(QuorumNode node, String hash) {
        String query = "{ \"query\": \"{ transaction(hash: \\\"" + hash + "\\\") { isPrivate } }\" }";
        Map<String, Object> jsonObject = executeGraphQL(node, query);
        return Observable.just(Boolean.parseBoolean(((Map<String, Object>)((Map<String, Object>)jsonObject.get("data")).get("transaction")).get("isPrivate").toString()));
    }

    public Observable<String> getPrivatePayload(QuorumNode node, String hash) {
        String query = "{ \"query\": \"{ transaction(hash: \\\"" + hash + "\\\") { privateInputData } }\" }";
        Map<String, Object> jsonObject = executeGraphQL(node, query);
        return Observable.just(((Map<String, Object>)((Map<String, Object>)jsonObject.get("data")).get("transaction")).get("privateInputData").toString());
    }

    private String graphqlUrl(QuorumNode node) {
        QuorumNetworkProperty.Node nodeConfig = networkProperty().getNodes().get(node);
        if (nodeConfig == null) {
            throw new IllegalArgumentException("Node " + node + " not found in config");
        }
        return nodeConfig.getGraphqlUrl();
    }

    private Map<String, Object> executeGraphQL(QuorumNode node, String query) {
        RequestBody body = RequestBody.create(
            MediaType.parse("application/json"), query);
        Request request = new Request.Builder()
            .url(graphqlUrl(node))
            .post(body)
            .build();
        Call call = httpClient.newCall(request);
        try {
            Response response = call.execute();
            InputStream responseBody = response.body().byteStream();
            return new ObjectMapper().readValue(responseBody, Map.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
