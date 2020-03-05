package com.quorum.gauge.common;

import com.thoughtworks.gauge.datastore.DataStoreFactory;
import com.thoughtworks.gauge.execution.parameters.parsers.base.CustomParameterParser;
import gauge.messages.Spec;

import static com.quorum.gauge.common.QuorumNetworkProperty.Node;

public class QuorumNodeParameterParser extends CustomParameterParser<Node> {

    @Override
    protected Node customParse(final Class<?> aClass, final Spec.Parameter parameter) {
        final String nodeName = parameter.getValue();

        final QuorumNetworkProperty props
            = (QuorumNetworkProperty) DataStoreFactory.getSuiteDataStore().get("networkProperties");

        final Node node = props.getNodesAsString().get(nodeName);
        if(node == null) {
            throw new IllegalArgumentException("Node " + nodeName + " not found in network properties");
        }
        return node;
    }

    @Override
    public boolean canParse(final Class<?> aClass, final Spec.Parameter parameter) {
        return aClass.equals(Node.class);
    }
}
