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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author longpeng.zlp
 * @date 2025/4/2 16:47
 */
public class K8sResourceUtilTest {
    @Test(expected = RuntimeException.class)
    public void testAllocateIPFailed() {
        List<Integer> ports = new ArrayList<>();
        for (int i = 0; i < 10240; ++i) {
            ports.add(i);
        }
        K8sResourceUtil.buildRandomPortMapper(ports.toArray(new Integer[0]));
    }

    @Test
    public void testAllocateIPNormal() {
        List<Integer> ports = new ArrayList<>();
        for (int i = 0; i < 10; ++i) {
            ports.add(i);
        }
        List<Pair<Integer, Integer>> portMappers = K8sResourceUtil.buildRandomPortMapper(ports.toArray(new Integer[0]));
        Assert.assertEquals(portMappers.size(), 10);
        Set<Integer> keys = portMappers.stream().map(p -> p.getLeft()).collect(Collectors.toSet());
        Set<Integer> values = portMappers.stream().map(p -> p.getRight()).collect(Collectors.toSet());
        Assert.assertEquals(keys.size(), 10);
        Assert.assertEquals(values.size(), 10);
        values.forEach(v -> Assert.assertTrue(v >= K8sResourceUtil.MIN_PORT && v <= K8sResourceUtil.MAX_PORT));
    }
}
