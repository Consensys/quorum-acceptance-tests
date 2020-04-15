package com.quorum.gauge.services;

import com.quorum.gauge.common.QuorumNetworkProperty;
import com.quorum.gauge.common.QuorumNode;
import io.reactivex.Observable;
import okhttp3.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.InputStream;
import java.io.InputStreamReader;

@Service
public class GraphQLService extends AbstractService {
    private static final Logger logger = LoggerFactory.getLogger(GraphQLService.class);

    @Autowired
    OkHttpClient httpClient;

    public Observable<Integer> getBlockNumber(QuorumNode node) {
        String query = "{ \"query\": \"{ block { number } }\" }";
        JSONObject jsonObject = executeGraphQL(node, query);
        if (jsonObject != null) {
            int blockNumber = Integer.decode(((JSONObject)((JSONObject)jsonObject.get("data")).get("block")).get("number").toString());
            logger.debug("GraphQL block number response: " + blockNumber);
            return Observable.just(blockNumber);
        }
        logger.debug("Invalid GraphQL block number response");
        return Observable.just(-1);
    }

    public Observable<Boolean> getIsPrivate(QuorumNode node, String hash) {
        String query = "{ \"query\": \"{ transaction(hash: \\\"" + hash + "\\\") { isPrivate } }\" }";
        JSONObject jsonObject = executeGraphQL(node, query);
        return Observable.just(Boolean.parseBoolean(((JSONObject)((JSONObject)jsonObject.get("data")).get("transaction")).get("isPrivate").toString()));
    }

    public Observable<String> getPrivatePayload(QuorumNode node, String hash) {
        String query = "{ \"query\": \"{ transaction(hash: \\\"" + hash + "\\\") { privateInputData } }\" }";
        JSONObject jsonObject = executeGraphQL(node, query);
        return Observable.just(((JSONObject)((JSONObject)jsonObject.get("data")).get("transaction")).get("privateInputData").toString());
    }

    private String graphqlUrl(QuorumNode node) {
        QuorumNetworkProperty.Node nodeConfig = networkProperty().getNodes().get(node);
        if (nodeConfig == null) {
            throw new IllegalArgumentException("Node " + node + " not found in config");
        }
        return nodeConfig.getGraphqlUrl();
    }

    private JSONObject executeGraphQL(QuorumNode node, String query) {
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
            JSONParser jsonParser = new JSONParser();
            JSONObject jsonObject = (JSONObject)jsonParser.parse(
                new InputStreamReader(responseBody, "UTF-8"));
            return jsonObject;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
