package com.quorum.gauge.services;

import com.quorum.gauge.common.QuorumNetworkProperty;
import com.quorum.gauge.common.QuorumNode;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.InputStream;
import java.io.InputStreamReader;

@Service
public class GraphQLService extends AbstractService {
    private static final Logger logger = LoggerFactory.getLogger(GraphQLService.class);

    public int getBlockNumber(QuorumNode node) {
        String query = "{ \"query\": \"{ block { number } }\" }";
        JSONObject jsonObject = executeGraphQL(node, query);
        if (jsonObject != null) {
            int blockNumber = Integer.decode(((JSONObject)((JSONObject)jsonObject.get("data")).get("block")).get("number").toString());
            logger.debug("GraphQL block number response: " + blockNumber);
            return blockNumber;
        }
        logger.debug("Invalid GraphQL block number response");
        return -1;
    }

    public Boolean getIsPrivate(QuorumNode node, String hash) {
        String query = "{ \"query\": \"{ transaction(hash: \\\"" + hash + "\\\") { isPrivate } }\" }";
        JSONObject jsonObject = executeGraphQL(node, query);
        return Boolean.parseBoolean(((JSONObject)((JSONObject)jsonObject.get("data")).get("transaction")).get("isPrivate").toString());
    }

    public String getPrivatePayload(QuorumNode node, String hash) {
        String query = "{ \"query\": \"{ transaction(hash: \\\"" + hash + "\\\") { privateInputData } }\" }";
        JSONObject jsonObject = executeGraphQL(node, query);
        return ((JSONObject)((JSONObject)jsonObject.get("data")).get("transaction")).get("privateInputData").toString();
    }

    private String graphqlUrl(QuorumNode node) {
        QuorumNetworkProperty.Node nodeConfig = networkProperty().getNodes().get(node);
        if (nodeConfig == null) {
            throw new IllegalArgumentException("Node " + node + " not found in config");
        }
        return nodeConfig.getGraphqlUrl();
    }

    private JSONObject executeGraphQL(QuorumNode node, String query) {
        StringEntity entity = new StringEntity(query,
            ContentType.APPLICATION_JSON);
        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost request = new HttpPost(graphqlUrl(node));
        request.setEntity(entity);

        try {
            HttpResponse response = httpClient.execute(request);
            InputStream responseBody = response.getEntity().getContent();
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
