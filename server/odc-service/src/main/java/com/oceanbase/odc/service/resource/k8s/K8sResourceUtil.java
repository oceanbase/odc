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
package com.oceanbase.odc.service.resource.k8s;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import com.oceanbase.odc.service.resource.Resource;
import com.oceanbase.odc.service.resource.ResourceLocation;
import com.oceanbase.odc.service.resource.ResourceManager;
import com.oceanbase.odc.service.resource.ResourceWithID;
import com.oceanbase.odc.service.task.config.K8sProperties;
import com.oceanbase.odc.service.task.resource.K8sPodResource;
import com.oceanbase.odc.service.task.resource.K8sResourceContext;
import com.oceanbase.odc.service.task.resource.manager.strategy.k8s.K8sResourceContextBuilder;

/**
 * @author longpeng.zlp
 * @date 2025/3/19 17:05
 */
public class K8sResourceUtil {
    protected static final Integer MIN_PORT = 30000;
    protected static final Integer MAX_PORT = 32767;

    protected static final Random PORT_RANDOM = new Random();

    public static ResourceWithID<K8sPodResource> createK8sPodResource(
            ResourceManager resourceManager, ResourceLocation resourceLocation, String k8sImplType, String imageName,
            K8sProperties k8sProperties, long id,
            List<Pair<Integer, Integer>> portMapper, Integer servicePort)
            throws Exception {
        K8sResourceContextBuilder contextBuilder = new K8sResourceContextBuilder(k8sProperties, portMapper,
                k8sImplType, imageName);
        K8sResourceContext k8sResourceContext =
                contextBuilder.buildK8sResourceContext(id, resourceLocation);
        ResourceWithID<K8sPodResource> k8sPodResource = null;
        // allocate resource failed, send alarm event and throws exception
        try {
            k8sPodResource = resourceManager.create(resourceLocation,
                    k8sResourceContext);
        } catch (Exception e) {
            throw e;
        }
        K8sPodResource podResource = k8sPodResource.getResource();
        podResource.setServicePort(String.valueOf(servicePort));
        return k8sPodResource;
    }

    public static K8sPodResource queryIpAndAddress(ResourceManager resourceManager, long resourceID) throws Exception {
        ResourceWithID<Resource> resourceWithID = resourceManager.query(resourceID)
                .orElseThrow(() -> new RuntimeException("resource not found, id = " + resourceID));
        return (K8sPodResource) resourceWithID.getResource();
    }

    // ports length should not large than 2767
    public static List<Pair<Integer, Integer>> buildRandomPortMapper(Integer... ports) {
        List<Pair<Integer, Integer>> ret = new ArrayList<>();
        Set<Integer> allocated = new HashSet<>();
        // same with k8s node range
        int min = MIN_PORT;
        int max = MAX_PORT;
        for (Integer port : ports) {
            if (null == port) {
                continue;
            }
            int tryCount = 0;
            boolean succeedAllocated = false;
            int candidatePort = -1;
            // try random loop
            while (tryCount++ < 10) {
                candidatePort = PORT_RANDOM.nextInt((max - min) + 1) + min;
                if (!allocated.contains(candidatePort)) {
                    succeedAllocated = true;
                    break;
                }
            }
            // go through
            if (!succeedAllocated) {
                for (candidatePort = min; candidatePort <= max; candidatePort++) {
                    if (!allocated.contains(candidatePort)) {
                        succeedAllocated = true;
                        break;
                    }
                }
            }
            if (!succeedAllocated) {
                throw new RuntimeException("invalid port allocate");
            }
            allocated.add(candidatePort);
            ret.add(Pair.of(port, candidatePort));
        }
        return ret;
    }
}
