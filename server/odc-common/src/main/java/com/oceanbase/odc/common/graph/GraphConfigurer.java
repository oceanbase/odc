/*
 * Copyright (c) 2023 OceanBase.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.oceanbase.odc.common.graph;

import java.util.LinkedList;

import lombok.NonNull;

/**
 * Config object for {@link Graph}
 *
 * @author yh263208
 * @date 2022-02-18 21:12
 * @since ODC_release_3.3.0
 */
public class GraphConfigurer<T extends Graph, V extends GraphVertex> {

    private final T target;
    private final LinkedList<V> graphVertex = new LinkedList<>();

    protected GraphConfigurer(@NonNull T target) {
        this.target = target;
    }

    public V first() {
        if (graphVertex.isEmpty()) {
            return null;
        }
        return graphVertex.getFirst();
    }

    public V last() {
        if (graphVertex.isEmpty()) {
            return null;
        }
        return graphVertex.getLast();
    }

    public GraphConfigurer<T, V> next(@NonNull V nextNode, float weight) {
        return next(nextNode, weight, null);
    }

    public <E extends GraphEdge> GraphConfigurer<T, V> next(@NonNull V nextNode, @NonNull E graphEdge) {
        return next(nextNode, 1, graphEdge);
    }

    public <E extends GraphEdge> GraphConfigurer<T, V> route(@NonNull E graphEdge,
            @NonNull GraphConfigurer<T, V> configurer) {
        return route(1, graphEdge, configurer);
    }

    public GraphConfigurer<T, V> route(float weight, @NonNull GraphConfigurer<T, V> configurer) {
        return route(weight, null, configurer);
    }

    public T and() {
        return target;
    }

    private <E extends GraphEdge> GraphConfigurer<T, V> next(@NonNull V nextNode, float weight, E graphEdge) {
        V from = last();
        if (from != null) {
            if (target.contains(from) && target.contains(nextNode)) {
                if (target.getWeight(from.getGraphId(), nextNode.getGraphId()) != Graph.INFINITE) {
                    return this;
                }
            }
            if (graphEdge == null) {
                target.insertEdge(from, nextNode, weight);
            } else {
                target.insertEdge(from, nextNode, graphEdge);
            }
        } else if (!target.contains(nextNode)) {
            target.insertVertex(nextNode);
        }
        graphVertex.add(nextNode);
        return this;
    }

    private <E extends GraphEdge> GraphConfigurer<T, V> route(float weight, E graphEdge,
            @NonNull GraphConfigurer<T, V> configurer) {
        V to = configurer.first();
        if (to == null) {
            return this;
        }
        V from = last();
        if (from == null) {
            throw new IllegalStateException("FromVertex should not be null");
        }
        if (target.contains(from) && target.contains(to)) {
            if (target.getWeight(from.getGraphId(), to.getGraphId()) != Graph.INFINITE) {
                return this;
            }
        }
        if (graphEdge != null) {
            target.insertEdge(from, to, graphEdge);
        } else {
            target.insertEdge(from, to, weight);
        }
        return this;
    }

}
