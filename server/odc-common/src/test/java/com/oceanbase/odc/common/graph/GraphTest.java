/*
 * Copyright (c) 2024 OceanBase.
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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.oceanbase.odc.common.lang.Pair;

/**
 * Test cases for {@link Graph}
 *
 * @author yh263208
 * @date 2022-01-17 17:16
 * @since ODC_release_3.3.0
 */
public class GraphTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void forBreadthFirstEach_buildDag_traverseEveryVertex() {
        float[][] weight = new float[][] {
                {0, 1, 2, 0, 0, 0, 0, 0},
                {0, 0, 0, 3, 4, 0, 0, 0},
                {0, 0, 0, 0, 0, 5, 6, 0},
                {0, 0, 0, 0, 0, 0, 0, 7},
                {0, 0, 0, 0, 0, 0, 0, 8},
                {0, 0, 0, 0, 0, 0, 0, 9},
                {0, 0, 0, 0, 0, 0, 0, 10},
                {0, 0, 0, 0, 0, 0, 0, 0}
        };
        Graph graph = buildDAG(weight);

        Assert.assertEquals(weight.length, graph.getVertexCount());
        Assert.assertEquals(10, graph.getEdgeCount());
        Assert.assertEquals(8.0, graph.getWeight("v4", "v7"), 0);

        Queue<String> queue = new LinkedList<>();
        for (int i = 0; i < weight.length; i++) {
            queue.add("v" + i);
        }

        AtomicBoolean flag = new AtomicBoolean(false);
        graph.forBreadthFirstEach("v0", vertex -> {
            if (!Objects.equals(vertex.getGraphId(), queue.poll())) {
                flag.set(true);
            }
        });
        Assert.assertFalse(flag.get());
    }

    @Test
    public void forDeepthFirstEach_buildDag_traverseEveryVertex() {
        float[][] weight = new float[][] {
                {0, 1, 0, 0, 0, 2, 0, 0},
                {0, 0, 2, 0, 4, 0, 0, 0},
                {0, 0, 0, 3, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 3, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 6, 7},
                {0, 0, 0, 3, 0, 0, 0, 0},
                {0, 0, 0, 3, 0, 0, 0, 0}
        };
        Graph graph = buildDAG(weight);

        Assert.assertEquals(weight.length, graph.getVertexCount());
        Assert.assertEquals(10, graph.getEdgeCount());
        Assert.assertEquals(-1, graph.getWeight("v4", "v7"), 0);

        Queue<String> queue = new LinkedList<>();
        for (int i = 0; i < weight.length; i++) {
            queue.add("v" + i);
        }

        AtomicBoolean flag = new AtomicBoolean(false);
        graph.forDeepthFirstEach("v0", vertex -> {
            if (!Objects.equals(vertex.getGraphId(), queue.poll())) {
                flag.set(true);
            }
        });
        Assert.assertFalse(flag.get());
    }

    @Test
    public void insertVertex_insertVertex_insertSucceed() {
        Graph graph = new Graph();
        graph.insertVertex(new GraphVertex("1", "1"));

        Assert.assertEquals(1, graph.getVertexCount());
        Assert.assertEquals(0, graph.getEdgeCount());

        AtomicInteger counter = new AtomicInteger(0);
        graph.forDeepthFirstEach("1", vertex -> counter.incrementAndGet());
        graph.forBreadthFirstEach("1", vertex -> counter.incrementAndGet());
        Assert.assertEquals(2, counter.get());
    }

    @Test
    public void insertVertex_insertDuplicatedVertex_expThrown() {
        Graph graph = new Graph();
        Assert.assertNotNull(graph.insertVertex(new GraphVertex("1", "1")));

        thrown.expectMessage("Duplicated vertex id 1");
        thrown.expect(IllegalArgumentException.class);
        graph.insertVertex(new GraphVertex("1", "1"));
    }

    @Test
    public void deleteEdge_deleteEdgeAndVertex_deleteSucceed() {
        Graph graph = new Graph();
        GraphEdge edge = graph.insertEdge(new GraphVertex("1", "1"), new GraphVertex("2", "2"), 120);

        Assert.assertNotNull(edge);
        Assert.assertEquals(2, graph.getVertexCount());
        Assert.assertEquals(1, graph.getEdgeCount());

        graph.deleteEdge("1", "2");
        graph.deleteVertex("1");
        graph.deleteVertex("2");
        Assert.assertEquals(0, graph.getVertexCount());
        Assert.assertEquals(0, graph.getEdgeCount());
    }

    @Test
    public void deleteEdge_deleteEdgeNonExists_expThrown() {
        Graph graph = new Graph();

        thrown.expectMessage("Vertex not found by id 1");
        thrown.expect(NullPointerException.class);
        graph.deleteEdge("1", "2");
    }

    @Test
    public void deleteEdge_deleteEdgeNonExistsVertexExist_expThrown() {
        Graph graph = new Graph();
        graph.insertVertex(new GraphVertex("1", "1"));

        thrown.expectMessage("Vertex not found by id 2");
        thrown.expect(NullPointerException.class);
        graph.deleteEdge("1", "2");
    }

    @Test
    public void insertEdge_insertEdgeTwice_expThrown() {
        Graph graph = new Graph();
        GraphVertex vertex1 = new GraphVertex("1", "1");
        GraphVertex vertex2 = new GraphVertex("2", "2");

        graph.insertEdge(vertex1, vertex2, 120);
        thrown.expectMessage("Edge already exists between Vertex 1 and 2");
        thrown.expect(IllegalArgumentException.class);
        graph.insertEdge(vertex1, vertex2, 120);
    }

    @Test
    public void insertEdge_insertEdgeDuplicated_expThrown() {
        Graph graph = new Graph();
        GraphVertex vertex1 = new GraphVertex("1", "1");
        GraphVertex vertex2 = new GraphVertex("2", "2");

        graph.insertEdge(vertex1, vertex2, 120);
        vertex1.getOutEdges().removeIf(graphEdge -> true);
        thrown.expectMessage("Unknown error");
        thrown.expect(IllegalStateException.class);
        graph.insertEdge(vertex1, vertex2, 120);
    }

    @Test
    public void buildDag_withoutCycle_buildSucceed() {
        float[][] weight = new float[][] {
                {0, 1, 1, 1, 0},
                {0, 0, 0, 0, 0},
                {0, 1, 0, 0, 1},
                {0, 0, 0, 0, 1},
                {0, 0, 0, 0, 0},
        };
        Graph graph = buildDAG(weight);
        List<GraphVertex> vertexList = graph.getTopoOrderedVertices();
        Assert.assertEquals(weight.length, vertexList.size());
    }

    @Test
    public void buildDag_withCycle_expThrown() {
        float[][] weight = new float[][] {
                {0, 1, 1, 1, 0},
                {1, 0, 0, 0, 0},
                {0, 1, 0, 0, 1},
                {0, 0, 0, 0, 1},
                {0, 0, 0, 0, 0},
        };
        Graph graph = buildDAG(weight);

        thrown.expectMessage("Graph existence cycle");
        thrown.expect(IllegalStateException.class);
        graph.getTopoOrderedVertices();
    }

    @Test
    public void create_dagUsingGraphConfigurer_buildSucceed() {
        float[][] weight = new float[][] {
                {0, 1, 0, 0, 0, 2, 0, 0},
                {0, 0, 2, 0, 4, 0, 0, 0},
                {0, 0, 0, 3, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 3, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 6, 7},
                {0, 0, 0, 3, 0, 0, 0, 0},
                {0, 0, 0, 3, 0, 0, 0, 0}
        };
        List<Pair<String, String>> sequences = new LinkedList<>();
        for (int i = 0; i < weight.length; i++) {
            float[] subWeights = weight[i];
            for (int j = 0; j < subWeights.length; j++) {
                if (i == j) {
                    continue;
                }
                if (weight[i][j] != 0) {
                    sequences.add(new Pair<>(i + "", j + ""));
                }
            }
        }
        Graph target = new Graph();
        String id = sequences.get(0).left;
        Map<String, GraphVertex> id2Vertex = new HashMap<>();
        GraphVertex start = id2Vertex.computeIfAbsent(id, s -> new GraphVertex(id, id));
        buildDAG(target, target.newGraphConfigurer(start), sequences, id2Vertex);

        Assert.assertEquals(weight.length, target.getVertexCount());
        Assert.assertEquals(10, target.getEdgeCount());
        Assert.assertEquals(-1.0, target.getWeight("4", "7"), 0);
    }

    private void buildDAG(Graph target, GraphConfigurer<Graph, GraphVertex> configurer,
            List<Pair<String, String>> sequences, Map<String, GraphVertex> id2Vertex) {
        GraphVertex currentSrcVertex = configurer.last();
        List<String> destVertexIds =
                sequences.stream().filter(pair -> Objects.equals(pair.left, currentSrcVertex.getGraphId())).map(
                        pair -> pair.right).collect(Collectors.toList());
        if (destVertexIds.isEmpty()) {
            return;
        } else if (destVertexIds.size() == 1) {
            String vertexId = destVertexIds.get(0);
            GraphVertex toVertext = id2Vertex.computeIfAbsent(vertexId, s -> new GraphVertex(s, s));
            configurer.next(toVertext, 1);
            buildDAG(target, configurer, sequences, id2Vertex);
            return;
        }
        for (String subVertexId : destVertexIds) {
            GraphVertex toVertext = id2Vertex.computeIfAbsent(subVertexId, s -> new GraphVertex(s, s));
            GraphConfigurer<Graph, GraphVertex> newConfigurer = target.newGraphConfigurer(toVertext);
            configurer.route(1, newConfigurer);
            buildDAG(target, newConfigurer, sequences, id2Vertex);
        }
    }

    private Graph buildDAG(float[][] weight) {
        Graph graph = new Graph();
        Map<String, GraphVertex> id2GraphVertex = new HashMap<>();
        for (int i = 0; i < weight.length; i++) {
            for (int j = 0; j < weight[i].length; j++) {
                if (weight[i][j] <= 0 || i == j) {
                    continue;
                }
                String nameAndId = "v" + i;
                String nameAndId1 = "v" + j;
                GraphVertex from =
                        id2GraphVertex.computeIfAbsent(nameAndId, s -> new GraphVertex(nameAndId, nameAndId));
                GraphVertex to =
                        id2GraphVertex.computeIfAbsent(nameAndId1, s -> new GraphVertex(nameAndId1, nameAndId1));
                graph.insertEdge(from, to, weight[i][j]);
            }
        }
        return graph;
    }
}
