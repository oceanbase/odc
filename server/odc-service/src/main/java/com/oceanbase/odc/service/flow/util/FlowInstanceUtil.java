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
package com.oceanbase.odc.service.flow.util;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.service.flow.instance.BaseFlowNodeInstance;
import com.oceanbase.odc.service.flow.instance.FlowApprovalInstance;
import com.oceanbase.odc.service.flow.instance.FlowGatewayInstance;
import com.oceanbase.odc.service.flow.instance.FlowInstance;
import com.oceanbase.odc.service.flow.instance.FlowInstanceConfigurer;
import com.oceanbase.odc.service.flow.instance.FlowNodeInstanceKey;
import com.oceanbase.odc.service.flow.instance.FlowSequenceInstance;
import com.oceanbase.odc.service.flow.instance.FlowTaskInstance;

import lombok.NonNull;

/**
 * Service Object for {@link FlowInstance}
 *
 * @author yh263208
 * @date 2022-02-24 21:37
 * @since ODC_release_3.3.0
 */
public class FlowInstanceUtil {

    /**
     * Get the execution path of the process instance
     *
     * @param flowInstance {@link FlowInstance}
     * @param router {@link Router}
     * @return collections of execution
     */
    public static List<BaseFlowNodeInstance> getExecutionRoute(@NonNull FlowInstance flowInstance,
            @NonNull Router router) {
        flowInstance.getTopoOrderedVertices();
        List<BaseFlowNodeInstance> instances =
                flowInstance.filterInstanceNode(BaseFlowNodeInstance::isStartEndpoint);
        Verify.singleton(instances, "RootInstances");

        List<BaseFlowNodeInstance> returnVal = new LinkedList<>();
        BaseFlowNodeInstance rootInstance = instances.get(0);
        returnVal.add(rootInstance);

        List<BaseFlowNodeInstance> nextInstances =
                flowInstance.getNextNodeInstances(rootInstance.getId(), rootInstance.getNodeType(),
                        rootInstance.getCoreFlowableElementType());
        while (!nextInstances.isEmpty()) {
            BaseFlowNodeInstance target = router.route(nextInstances);
            if (Objects.isNull(target)) {
                break;
            }
            returnVal.add(target);
            nextInstances = flowInstance.getNextNodeInstances(target.getId(), target.getNodeType(),
                    target.getCoreFlowableElementType());
        }
        return returnVal;
    }

    /**
     * Load the topology of the {@link FlowInstance}. The prerequisite for loading is that the topology
     * of the {@link FlowInstance} is empty.
     *
     * @param instance target {@link FlowInstance}
     * @param nodeInstances all {@link BaseFlowNodeInstance} set
     * @param sequences topo structure of the {@link FlowInstance}
     */
    public static void loadTopology(@NonNull FlowInstance instance,
            @NonNull List<BaseFlowNodeInstance> nodeInstances,
            @NonNull Set<FlowSequenceInstance> sequences) {
        Verify.verify(instance.getNodeInstanceCount() == 0 && instance.getSequenceCount() == 0,
                "FlowInstance's topology is not clear");
        if (nodeInstances.size() == 0) {
            return;
        }
        Map<FlowNodeInstanceKey, BaseFlowNodeInstance> key2NodeIntance = nodeInstances.stream()
                .collect(Collectors.toMap(FlowNodeInstanceKey::new, nodeInstance -> nodeInstance));
        Set<FlowNodeInstanceKey> instanceKeys = new HashSet<>(key2NodeIntance.keySet());
        FlowNodeInstanceKey rootInstanceKey = null;
        for (Map.Entry<FlowNodeInstanceKey, BaseFlowNodeInstance> entry : key2NodeIntance.entrySet()) {
            BaseFlowNodeInstance nodeInstance = entry.getValue();
            if (nodeInstance.isStartEndpoint()) {
                rootInstanceKey = entry.getKey();
                break;
            }
        }
        Verify.notNull(rootInstanceKey, "Root Instance can not be null");

        BaseFlowNodeInstance target = key2NodeIntance.get(rootInstanceKey);
        Verify.notNull(target, "Instance can not be null for key " + rootInstanceKey);
        FlowInstanceConfigurer startConfigurer = instance.newFlowInstance();
        next(startConfigurer, target);
        Set<FlowNodeInstanceKey> visitedKeys = new HashSet<>();
        buildFlowInstance(instance, startConfigurer, sequences, key2NodeIntance, visitedKeys);
        instanceKeys.removeAll(visitedKeys);

        while (!instanceKeys.isEmpty()) {
            target = key2NodeIntance.get(instanceKeys.iterator().next());
            Verify.notNull(target, "Instance can not be null for key " + rootInstanceKey);
            FlowInstanceConfigurer configurer = instance.newFlowInstanceConfigurer();
            next(configurer, target);
            visitedKeys = new HashSet<>();
            buildFlowInstance(instance, configurer, sequences, key2NodeIntance, visitedKeys);
            instanceKeys.removeAll(visitedKeys);
        }
    }

    private static void buildFlowInstance(FlowInstance target, FlowInstanceConfigurer configurer,
            Set<FlowSequenceInstance> sequences,
            Map<FlowNodeInstanceKey, BaseFlowNodeInstance> key2NodeIntance, Set<FlowNodeInstanceKey> visitedKeys) {
        BaseFlowNodeInstance currentInstance = configurer.last();
        Verify.notNull(currentInstance, "currentInstance");
        visitedKeys.add(new FlowNodeInstanceKey(currentInstance));
        List<FlowNodeInstanceKey> destInstances = sequences.stream().filter(
                flowSequenceInstance -> Objects.equals(flowSequenceInstance.getSource(),
                        new FlowNodeInstanceKey(currentInstance)))
                .map(FlowSequenceInstance::getTarget).collect(Collectors.toList());
        if (destInstances.isEmpty()) {
            Verify.verify(currentInstance.isEndEndPoint(), "CurrentInstance type is illegal");
            Verify.verify(!(currentInstance instanceof FlowGatewayInstance), "Gateway instance is illegal here");
            configurer.endFlowInstance();
            return;
        } else if (destInstances.size() == 1 && !(currentInstance instanceof FlowGatewayInstance)) {
            FlowNodeInstanceKey targetInstance = destInstances.get(0);
            BaseFlowNodeInstance nextNode = key2NodeIntance.get(targetInstance);
            Verify.notNull(nextNode, "Key " + targetInstance + " is not found");
            next(configurer, nextNode);
            buildFlowInstance(target, configurer, sequences, key2NodeIntance, visitedKeys);
            return;
        }
        Verify.verify(currentInstance instanceof FlowGatewayInstance, "Only gateway can be routed");
        if (!currentInstance.isEndEndPoint()) {
            Verify.greaterThan(destInstances.size(), 1, "DestInstances");
        }
        for (FlowNodeInstanceKey subNodeInstance : destInstances) {
            if (currentInstance.isEndEndPoint()) {
                configurer.route(target.endFlowInstance());
            }
            BaseFlowNodeInstance nextNode = key2NodeIntance.get(subNodeInstance);
            Verify.notNull(nextNode, "Key " + subNodeInstance + " is not found");
            FlowInstanceConfigurer newConfigurer;
            if (nextNode instanceof FlowApprovalInstance) {
                newConfigurer = target.newFlowInstanceConfigurer((FlowApprovalInstance) nextNode);
            } else if (nextNode instanceof FlowGatewayInstance) {
                newConfigurer = target.newFlowInstanceConfigurer((FlowGatewayInstance) nextNode);
            } else if (nextNode instanceof FlowTaskInstance) {
                newConfigurer = target.newFlowInstanceConfigurer((FlowTaskInstance) nextNode);
            } else {
                throw new IllegalStateException("Type for " + nextNode + " is illegal");
            }
            configurer.route(newConfigurer);
            buildFlowInstance(target, newConfigurer, sequences, key2NodeIntance, visitedKeys);
        }
    }

    private static void next(@NonNull FlowInstanceConfigurer configurer, @NonNull BaseFlowNodeInstance instance) {
        if (instance instanceof FlowApprovalInstance) {
            configurer.nextLogicTask((FlowApprovalInstance) instance);
        } else if (instance instanceof FlowGatewayInstance) {
            configurer.nextLogicTask((FlowGatewayInstance) instance);
        } else if (instance instanceof FlowTaskInstance) {
            configurer.nextLogicTask((FlowTaskInstance) instance);
        } else {
            throw new IllegalStateException("Type for " + instance + " is illegal");
        }
    }

}
