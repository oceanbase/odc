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

package com.oceanbase.odc.service.flow.instance;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.flow.graph.GraphEdge;
import com.oceanbase.odc.core.flow.graph.GraphVertex;
import com.oceanbase.odc.core.flow.model.FlowableElementType;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.metadb.flow.NodeInstanceEntity;
import com.oceanbase.odc.metadb.flow.NodeInstanceEntityRepository;
import com.oceanbase.odc.metadb.flow.SequenceInstanceEntity;
import com.oceanbase.odc.metadb.flow.SequenceInstanceRepository;
import com.oceanbase.odc.service.flow.model.FlowNodeType;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class TopologyBuilder {

    @Autowired
    private NodeInstanceEntityRepository nodeInstanceEntityRepository;

    @Autowired
    private SequenceInstanceRepository sequenceRepository;


    public void buildTopology(FlowInstance flowInstance) {
        List<BaseFlowNodeInstance> sourceNodes = flowInstance.getVertexList().stream().map(v -> {
            Verify.verify(v instanceof BaseFlowNodeInstance,
                    "GraphVertex has to be a instance of AbstractFlowNodeInstance");
            return (BaseFlowNodeInstance) v;
        }).collect(Collectors.toList());

        List<BaseFlowNodeInstance> targetNodes = flowInstance.getVertexList().stream()
                .map(v -> {
                    List<GraphEdge> outEdges = v.getOutEdges();
                    return outEdges.stream().map(outEdge -> {
                        GraphVertex to = outEdge.getTo();
                        if (!(to instanceof BaseFlowNodeInstance)) {
                            throw new IllegalStateException(
                                    "GraphVertex has to be an instance of BaseFlowNodeInstance");
                        }
                        return (BaseFlowNodeInstance) to;
                    }).collect(Collectors.toList());
                })
                .flatMap(List::stream).collect(Collectors.toList());


        Set<BaseFlowNodeInstance> allNodesSet = distinctByIdentical(Stream
                .concat(sourceNodes.stream(), targetNodes.stream())
                .collect(Collectors.toList()));

        List<NodeInstanceEntity> toCreate = diffToCreate(allNodesSet);

        nodeInstanceEntityRepository.batchCreate(toCreate);

        List<Long> sourceInstanceIds =
                sourceNodes.stream().map(BaseFlowNodeInstance::getId).collect(Collectors.toList());
        sequenceRepository.deleteBySourceNodeInstanceIdIn(sourceInstanceIds);

        Set<Long> instanceIds = allNodesSet.stream().map(BaseFlowNodeInstance::getId).collect(Collectors.toSet());
        List<NodeInstanceEntity> all = nodeInstanceEntityRepository.findByInstanceIdIn(instanceIds);

        Map<NodeInstanceIdentical, List<NodeInstanceEntity>> nodeMap = all.stream().collect(
                Collectors.groupingBy(NodeInstanceIdentical::new));

        List<SequenceInstanceEntity> sequences = flowInstance.getVertexList().stream()
                .map(v -> vertexToCreateSequence(v, nodeMap))
                .flatMap(List::stream).collect(Collectors.toList());

        sequenceRepository.batchCreate(sequences);
    }

    private List<SequenceInstanceEntity> vertexToCreateSequence(GraphVertex v,
            Map<NodeInstanceIdentical, List<NodeInstanceEntity>> nodeMap) {
        BaseFlowNodeInstance source = (BaseFlowNodeInstance) v;
        List<NodeInstanceEntity> sourceEntities = nodeMap.get(new NodeInstanceIdentical(source));
        if (sourceEntities.size() >= 2) {
            log.warn("Duplicate records are found, id={}, nodeType={}, coreType={} ", source.getId(),
                    source.getNodeType(), source.getCoreFlowableElement());
            throw new IllegalStateException("Duplicate records are found");
        }

        NodeInstanceEntity sourceInstanceEntity = sourceEntities.get(0);
        List<GraphEdge> outEdges = v.getOutEdges();
        return outEdges.stream()
                .map(outEdge -> {
                    GraphVertex graphVertex = outEdge.getTo();
                    if (!(graphVertex instanceof BaseFlowNodeInstance)) {
                        throw new IllegalStateException(
                                "GraphVertex has to be an instance of BaseFlowNodeInstance");
                    }
                    BaseFlowNodeInstance target = (BaseFlowNodeInstance) graphVertex;
                    List<NodeInstanceEntity> nodeInstanceEntities =
                            nodeMap.get(new NodeInstanceIdentical(target));
                    if (nodeInstanceEntities.size() >= 2) {
                        log.warn("Duplicate records are found, id={}, nodeType={}, coreType={} ",
                                target.getId(),
                                target.getNodeType(), target.getCoreFlowableElement());
                        throw new IllegalStateException("Duplicate records are found");
                    }
                    NodeInstanceEntity nodeInstanceEntity = nodeInstanceEntities.get(0);
                    SequenceInstanceEntity sequenceEntity = new SequenceInstanceEntity();
                    sequenceEntity.setSourceNodeInstanceId(sourceInstanceEntity.getId());
                    sequenceEntity.setTargetNodeInstanceId(nodeInstanceEntity.getId());
                    sequenceEntity.setFlowInstanceId(source.getFlowInstanceId());
                    return sequenceEntity;
                })
                .collect(Collectors.toList());
    }


    private Set<BaseFlowNodeInstance> distinctByIdentical(List<BaseFlowNodeInstance> allNodes) {
        Set<BaseFlowNodeInstance> allNodesSet = new HashSet<BaseFlowNodeInstance>() {
            @Override
            public boolean add(BaseFlowNodeInstance i) {
                for (BaseFlowNodeInstance element : this) {
                    if (Objects.equals(element.getId(), i.getId()) && element.getNodeType() == i.getNodeType()
                            && element.getCoreFlowableElementType() == i.getCoreFlowableElementType()) {
                        return false;
                    }
                }
                return super.add(i);
            }
        };
        allNodesSet.addAll(allNodes);
        return allNodesSet;
    }

    private List<NodeInstanceEntity> diffToCreate(Set<BaseFlowNodeInstance> allNodesSet) {
        Set<Long> instanceIds = allNodesSet.stream().map(BaseFlowNodeInstance::getId).collect(Collectors.toSet());

        List<NodeInstanceEntity> byInstanceIdIn = nodeInstanceEntityRepository.findByInstanceIdIn(instanceIds);
        Map<NodeInstanceIdentical, List<NodeInstanceEntity>> dbNodeMap = byInstanceIdIn.stream().collect(
                Collectors.groupingBy(NodeInstanceIdentical::new));


        return allNodesSet.stream().filter(n -> filterNoInDb(n, dbNodeMap)).map(i -> {
            Verify.notNull(i.getName(), "Name");
            Verify.notNull(i.getActivityId(), "ActivityId");
            NodeInstanceEntity nodeEntity = new NodeInstanceEntity();
            nodeEntity.setInstanceId(i.getId());
            nodeEntity.setInstanceType(i.getNodeType());
            nodeEntity.setFlowInstanceId(i.getFlowInstanceId());
            nodeEntity.setActivityId(i.getActivityId());
            nodeEntity.setName(i.getName());
            nodeEntity.setFlowableElementType(i.getCoreFlowableElementType());
            dbNodeMap.put(new NodeInstanceIdentical(nodeEntity), Collections.singletonList(nodeEntity));
            return nodeEntity;
        }).collect(Collectors.toList());
    }

    private boolean filterNoInDb(BaseFlowNodeInstance node,
            Map<NodeInstanceIdentical, List<NodeInstanceEntity>> dbNodeMap) {
        node.validExists();
        List<NodeInstanceEntity> nodeInstanceEntities = dbNodeMap.get(new NodeInstanceIdentical(node));
        if (CollectionUtils.isNotEmpty(nodeInstanceEntities)) {
            if (nodeInstanceEntities.size() >= 2) {
                log.warn("Duplicate records are found, id={}, nodeType={}, coreType={} ", node.getId(),
                        node.getNodeType(),
                        node.getCoreFlowableElement());
                throw new IllegalStateException("Duplicate records are found");
            }
            if (!nodeInstanceEntities.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Data
    static class NodeInstanceIdentical {
        private long instanceId;
        private FlowNodeType instanceType;
        private FlowableElementType flowableElementType;

        public NodeInstanceIdentical(BaseFlowNodeInstance instance) {
            this.instanceId = instance.getId();
            this.instanceType = instance.getNodeType();
            this.flowableElementType = instance.getCoreFlowableElementType();
        }

        public NodeInstanceIdentical(NodeInstanceEntity nodeInstanceEntity) {
            this.instanceId = nodeInstanceEntity.getInstanceId();
            this.instanceType = nodeInstanceEntity.getInstanceType();
            this.flowableElementType = nodeInstanceEntity.getFlowableElementType();
        }

    }
}
