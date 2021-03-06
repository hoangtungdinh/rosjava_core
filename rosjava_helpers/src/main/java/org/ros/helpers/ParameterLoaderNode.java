/*
 * Copyright 2017 Ekumen, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ros.helpers;

import org.apache.commons.logging.Log;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.parameter.ParameterTree;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Loads parameters from Yaml files into the ROS parameter server.
 * A different namespace can be specified for each of the files to load.
 * To use this node, create a {@link Resource} list with the resources to load,
 * and start it in the standard RosJava way.
 *
 * @author lucas@ekumenlabs.com (Lucas Chiesa).
 * Modified by jubeira@ekumenlabs.com (Juan I. Ubeira)
 */
public class ParameterLoaderNode extends AbstractNodeMain {

    public static final String NODE_NAME = "parameter_loader";
    private final List<LoadedResource> params = new ArrayList<>();
    private Log log;

    /**
     * Default constructor
     * @param resources Array of resources with their respective namespace to load.
     */
    public ParameterLoaderNode(ArrayList<Resource> resources) {
        for (Resource r : resources) {
            addSingleYmlInput(r.inputStream, r.namespace == null ? "" : r.namespace);
        }
    }

    private void addSingleYmlInput(InputStream ymlInputStream, String namespace) {
        this.params.add(new LoadedResource((new Yaml()).load(ymlInputStream), namespace));
    }

    private void addParams(ParameterTree parameterTree, String namespace, Map<String, Object> params) {
        for (Map.Entry<String, Object> e : params.entrySet()) {
            String fullKeyName = namespace + "/" + e.getKey();
            if (log != null) {
                log.info("Loading parameter " + fullKeyName + " \nValue = " + e.getValue());
            }

            if (e.getValue() instanceof String) {
                parameterTree.set(fullKeyName, (String)e.getValue());
            } else if (e.getValue() instanceof Integer) {
                parameterTree.set(fullKeyName, (Integer)e.getValue());
            } else if (e.getValue() instanceof Double) {
                parameterTree.set(fullKeyName, (Double)e.getValue());
            } else if (e.getValue() instanceof Map) {
                parameterTree.set(fullKeyName, (Map)e.getValue());
            } else if (e.getValue() instanceof Boolean) {
                parameterTree.set(fullKeyName, (Boolean)e.getValue());
            } else if (e.getValue() instanceof List) {
                parameterTree.set(fullKeyName, (List)e.getValue());
            } else if (log != null) {
                log.debug("I don't know what type parameter " + fullKeyName + " is. Value = " + e.getValue());
                log.debug("Class name is: " + e.getValue().getClass().getName());
            }
        }
    }

    /**
     * @return the name of the Node that will be used if a name was not
     * specified in the Node's associated NodeConfiguration.
     */
    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of(NODE_NAME);
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        if (params != null) {
            ParameterTree parameterTree = connectedNode.getParameterTree();
            log = connectedNode.getLog();

            // TODO: For some reason, setting the / param when using a rosjava master doesn't work
            // It does work fine with an external master, and also setting other params of any type
            for (LoadedResource r : params) {
                addParams(parameterTree, r.namespace, r.resource);
            }

            connectedNode.shutdown();
        }
    }

    /**
     * Resource to load to Parameter Server, consisting of an InputStream and its corresponding namespace.
     */
    public static class Resource {
        public InputStream inputStream;
        public String namespace;

        public Resource(InputStream inputStream, String namespace) {
            this.inputStream = inputStream;
            this.namespace = namespace;
        }
    }

    /**
     * Thin wrapper for the object returned by Yaml.load().
     * The object returned by Yaml.load() is a LinkedHashMap<String, Object>; this class is to
     * keep the code simple.
     */
    private class LoadedResource {
        public Map<String, Object> resource;
        public String namespace;

        LoadedResource(Object resource, String namespace) {
            this.resource = (Map<String, Object>) resource;
            this.namespace = namespace;
        }
    }
}
