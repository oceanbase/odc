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

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * DAG
 *
 * @author yh263208
 * @date 2022-01-14 18:05
 * @since ODC_release_3.3.0
 */
@Slf4j
public class Graph {

    @Getter
    protected final List<GraphEdge> edgeList = new LinkedList<>();
    @Getter
    protected final List<GraphVertex> vertexList = new LinkedList<>();
    private final AtomicInteger edgeIdGenerator = new AtomicInteger(0);
    public final static float INFINITE = -1;

    public GraphVertex insertVertex(@NonNull GraphVertex vertex) {
        if (findVertextById(vertex.getGraphId()) != null) {
            throw new IllegalArgumentException("Duplicated vertex id " + vertex.getGraphId());
        }
        vertexList.add(vertex);
        return vertex;
    }

    public GraphEdge insertEdge(@NonNull GraphVertex from, @NonNull GraphVertex to,
            @NonNull GraphEdge targetGraphEdge) {
        List<GraphEdge> graphEdges = from.getOutEdges();
        for (GraphEdge graphEdge : graphEdges) {
            if (Objects.equals(graphEdge.getTo(), to)) {
                throw new IllegalArgumentException(
                        "Edge already exists between Vertex " + from.getGraphId() + " and " + to.getGraphId());
            }
        }
        graphEdges = to.getInEdges();
        for (GraphEdge graphEdge : graphEdges) {
            if (Objects.equals(graphEdge.getFrom(), from)) {
                throw new IllegalStateException("Unknown error");
            }
        }
        targetGraphEdge.setFrom(from);
        targetGraphEdge.setTo(to);
        from.addOutEdge(targetGraphEdge);
        to.addInEdge(targetGraphEdge);
        edgeList.add(targetGraphEdge);
        if (findVertextById(from.getGraphId()) == null) {
            vertexList.add(from);
        }
        if (findVertextById(to.getGraphId()) == null) {
            vertexList.add(to);
        }
        return targetGraphEdge;
    }

    public GraphEdge insertEdge(@NonNull GraphVertex from, @NonNull GraphVertex to, float weight) {
        GraphEdge graphEdge =
                new GraphEdge(edgeIdGenerator.incrementAndGet() + "", from.getGraphId() + "->" + to.getGraphId());
        graphEdge.setWeight(weight);
        return insertEdge(from, to, graphEdge);
    }

    public List<GraphVertex> getTopoOrderedVertices() {
        List<GraphVertex> returnVal = new LinkedList<>();
        Set<GraphEdge> deletedEdges = new HashSet<>();
        while (true) {
            GraphVertex target = null;
            for (GraphVertex graphVertex : this.vertexList) {
                List<GraphEdge> inEdges = graphVertex.getInEdges().stream()
                        .filter(graphEdge -> !deletedEdges.contains(graphEdge)).collect(Collectors.toList());
                if (!returnVal.contains(graphVertex) && inEdges.isEmpty()) {
                    target = graphVertex;
                    break;
                }
            }
            if (target == null) {
                break;
            }
            returnVal.add(target);
            deletedEdges.addAll(target.getOutEdges());
        }
        if (returnVal.size() < this.vertexList.size()) {
            throw new IllegalStateException("Graph existence cycle");
        }
        return returnVal;
    }

    public GraphVertex deleteVertex(@NonNull String targetVertexId) {
        GraphVertex target = findVertextById(targetVertexId);
        if (target == null) {
            throw new NullPointerException("Not found vertex by id " + targetVertexId);
        }
        this.vertexList.removeIf(vertex -> targetVertexId.equals(vertex.getGraphId()));
        return target;
    }

    public GraphEdge deleteEdge(@NonNull String fromVertexId, @NonNull String toVertexId) {
        GraphVertex from = findVertextById(fromVertexId);
        GraphVertex to = findVertextById(toVertexId);
        if (from == null) {
            throw new NullPointerException("Vertex not found by id " + fromVertexId);
        } else if (to == null) {
            throw new NullPointerException("Vertex not found by id " + toVertexId);
        }
        Iterator<GraphEdge> outEdgesIter = from.getOutEdges().iterator();
        Iterator<GraphEdge> inEdgesIter = to.getInEdges().iterator();
        GraphEdge destEdge = null;
        while (outEdgesIter.hasNext()) {
            GraphEdge outEdge = outEdgesIter.next();
            while (inEdgesIter.hasNext()) {
                GraphEdge inEdge = inEdgesIter.next();
                if (Objects.equals(outEdge, inEdge)) {
                    outEdgesIter.remove();
                    inEdgesIter.remove();
                    destEdge = inEdge;
                }
            }
        }
        if (destEdge != null) {
            GraphEdge finalDestEdge = destEdge;
            this.edgeList.removeIf(graphEdge -> Objects.equals(graphEdge.getGraphId(), finalDestEdge.getGraphId()));
        }
        return destEdge;
    }

    public boolean contains(@NonNull GraphVertex vertex) {
        return findVertextById(vertex.getGraphId()) != null;
    }

    public float getWeight(@NonNull String fromVertexId, @NonNull String toVertexId) {
        GraphVertex fromVertex = findVertextById(fromVertexId);
        GraphVertex toVertex = findVertextById(toVertexId);
        if (fromVertex == null) {
            throw new NullPointerException("Vertex not found by id " + fromVertexId);
        } else if (toVertex == null) {
            throw new NullPointerException("Vertex not found by id " + toVertexId);
        }
        List<GraphEdge> outEdges = fromVertex.getOutEdges();
        List<GraphEdge> inEdges = toVertex.getInEdges();
        for (GraphEdge outEdge : outEdges) {
            for (GraphEdge inEdge : inEdges) {
                if (Objects.equals(outEdge.getGraphId(), inEdge.getGraphId())) {
                    return inEdge.getWeight();
                }
            }
        }
        return INFINITE;
    }

    public List<GraphVertex> getNeighbors(@NonNull String rootVertexId) {
        GraphVertex fromVertex = findVertextById(rootVertexId);
        if (fromVertex == null) {
            throw new NullPointerException("Vertex not found by id " + rootVertexId);
        }
        return fromVertex.getOutEdges().stream().map(GraphEdge::getTo).collect(Collectors.toList());
    }

    public int getVertexCount() {
        return this.vertexList.size();
    }

    public int getEdgeCount() {
        return this.edgeList.size();
    }

    public void forDeepthFirstEach(@NonNull String rootVertexId, @NonNull Consumer<GraphVertex> consumer) {
        GraphVertex rootVertex = findVertextById(rootVertexId);
        if (rootVertex == null) {
            throw new NullPointerException("Vertex not found by id " + rootVertexId);
        }
        Set<String> visited = new HashSet<>();
        visited.add(rootVertexId);
        try {
            consumer.accept(rootVertex);
        } catch (Exception exception) {
            log.warn("Deepth-first traversal encountered an error", exception);
        } finally {
            visited.add(rootVertex.getGraphId());
        }
        innerDeepthFirstEach(rootVertex, visited, consumer);
    }

    public void forBreadthFirstEach(@NonNull String rootVertexId, @NonNull Consumer<GraphVertex> consumer) {
        GraphVertex rootVertex = findVertextById(rootVertexId);
        if (rootVertex == null) {
            throw new NullPointerException("Vertex not found by id " + rootVertexId);
        }
        Set<String> visited = new HashSet<>();
        Queue<GraphVertex> queue = new LinkedList<>();
        try {
            consumer.accept(rootVertex);
        } catch (Exception exception) {
            log.warn("Breadth-first traversal encountered an error", exception);
        } finally {
            visited.add(rootVertex.getGraphId());
            queue.add(rootVertex);
        }
        while (!queue.isEmpty()) {
            GraphVertex target = queue.poll();
            GraphVertex neighbor = getFirstNeighbor(target.getGraphId());
            while (neighbor != null) {
                if (!visited.contains(neighbor.getGraphId())) {
                    try {
                        consumer.accept(neighbor);
                    } catch (Exception exception) {
                        log.warn("Breadth-first traversal encountered an error", exception);
                    } finally {
                        visited.add(neighbor.getGraphId());
                        queue.add(neighbor);
                    }
                }
                neighbor = getNextNeighbor(target.getGraphId(), neighbor.getGraphId());
            }
        }
    }

    public GraphConfigurer<Graph, GraphVertex> newGraphConfigurer(@NonNull GraphVertex vertex) {
        GraphConfigurer<Graph, GraphVertex> configurer = new GraphConfigurer<>(this);
        configurer.next(vertex, 1);
        return configurer;
    }

    public Graph converge(
            @NonNull List<GraphConfigurer<Graph, GraphVertex>> configurerList,
            @NonNull GraphConfigurer<Graph, GraphVertex> convergedExecution) {
        GraphVertex to = convergedExecution.first();
        if (to == null) {
            throw new IllegalStateException("Dest execution can not be empty");
        }
        for (GraphConfigurer<Graph, GraphVertex> executionConfigurer : configurerList) {
            executionConfigurer.next(to, 1f);
        }
        return this;
    }

    private GraphVertex findVertextById(@NonNull String vertexId) {
        for (GraphVertex vertex : vertexList) {
            if (Objects.equals(vertexId, vertex.getGraphId())) {
                return vertex;
            }
        }
        return null;
    }

    private GraphVertex getFirstNeighbor(@NonNull String rootVertexId) {
        List<GraphVertex> vertices = getNeighbors(rootVertexId);
        if (vertices.isEmpty()) {
            return null;
        }
        return vertices.get(0);
    }

    private GraphVertex getNextNeighbor(@NonNull String rootVertexId, @NonNull String targetVertexId) {
        List<GraphVertex> vertices = getNeighbors(rootVertexId);
        if (vertices.isEmpty()) {
            return null;
        }
        int i = 0;
        for (; i < vertices.size(); i++) {
            if (Objects.equals(vertices.get(i).getGraphId(), targetVertexId)) {
                break;
            }
        }
        if ((++i) < vertices.size()) {
            return vertices.get(i);
        }
        return null;
    }

    private void innerDeepthFirstEach(@NonNull GraphVertex rootVertex, Set<String> visited,
            @NonNull Consumer<GraphVertex> consumer) {
        List<GraphVertex> neighbors = getNeighbors(rootVertex.getGraphId());
        if (neighbors.isEmpty()) {
            return;
        }
        for (GraphVertex graphVertex : neighbors) {
            if (visited.contains(graphVertex.getGraphId())) {
                continue;
            }
            try {
                consumer.accept(graphVertex);
            } catch (Exception exception) {
                log.warn("Deepth-first traversal encountered an error", exception);
            } finally {
                visited.add(graphVertex.getGraphId());
            }
            innerDeepthFirstEach(graphVertex, visited, consumer);
        }
    }

}
