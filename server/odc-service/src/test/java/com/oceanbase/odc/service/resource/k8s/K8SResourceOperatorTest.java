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

import java.util.Date;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.oceanbase.odc.service.resource.ResourceState;
import com.oceanbase.odc.service.resource.k8s.client.K8sJobClient;
import com.oceanbase.odc.service.task.exception.JobException;

/**
 * test for K8SResourceOperator
 * 
 * @author longpeng.zlp
 * @date 2024/9/3 11:30
 */
public class K8SResourceOperatorTest {
    private final String resourceName = "myResource";
    private final String regionName = "region";
    private final String groupName = "group";
    private K8sResourceOperatorContext context;
    private MockK8sJobClient mockK8sJobClient;
    private K8sResourceContext k8sResourceContext;
    private PodConfig podConfig;
    private Object extraData = new String("objectName");

    @Before
    public void init() {
        mockK8sJobClient = new MockK8sJobClient();
        context = new K8sResourceOperatorContext(mockK8sJobClient, (r) -> 1000L, 5000L);
        podConfig = new PodConfig();
        podConfig.setImage("myImage");
        k8sResourceContext = new K8sResourceContext(podConfig, resourceName,
                regionName, groupName, extraData);
    }

    /**
     * test create operation
     */
    @Test
    public void testK8SResourceOperatorCreate() throws JobException {
        K8SResourceOperator operator = new K8SResourceOperator(context);
        K8sPodResource k8sPodResource = operator.create(k8sResourceContext);
        Assert.assertEquals(k8sPodResource.getArn(), resourceName);
        Assert.assertEquals(k8sPodResource.getPodIpAddress(), "localhost:8080");
    }

    @Test
    public void testK8SResourceOperatorQuery() throws JobException {
        K8SResourceOperator operator = new K8SResourceOperator(context);
        operator.create(k8sResourceContext);
        K8sPodResourceID resourceID = new K8sPodResourceID(regionName, groupName, "", resourceName);
        Optional<K8sPodResource> k8sPodResource = operator.query(resourceID);
        Assert.assertTrue(k8sPodResource.isPresent());
        K8sPodResource resource = k8sPodResource.get();
        Assert.assertEquals(resource.getArn(), resourceName);
        Assert.assertEquals(resource.getPodIpAddress(), "localhost:8080");
    }

    @Test
    public void testK8SResourceOperatorDestroy() throws JobException {
        K8SResourceOperator operator = new K8SResourceOperator(context);
        K8sPodResourceID resourceID = new K8sPodResourceID(regionName, groupName, "name", resourceName);
        operator.create(k8sResourceContext);
        Assert.assertEquals(operator.destroy(resourceID), "name:" + resourceName);
    }

    @Test
    public void testK8SResourceOperatorCanNotBeDestroyed() throws JobException {
        K8SResourceOperator operator = new K8SResourceOperator(context);
        K8sPodResourceID resourceID = new K8sPodResourceID(regionName, groupName, "name", resourceName);
        operator.create(k8sResourceContext);
        Assert.assertFalse(operator.canBeDestroyed(resourceID));
    }

    @Test
    public void testK8SResourceOperatorCanBeDestroyed() throws JobException {
        context = new K8sResourceOperatorContext(mockK8sJobClient, (r) -> 1000L, 500L);
        K8SResourceOperator operator = new K8SResourceOperator(context);
        K8sPodResourceID resourceID = new K8sPodResourceID(regionName, groupName, "name", resourceName);
        operator.create(k8sResourceContext);
        Assert.assertTrue(operator.canBeDestroyed(resourceID));
    }

    /**
     * mock k8s client
     */
    private static final class MockK8sJobClient implements K8sJobClient {
        private K8sResourceContext createContext;
        private volatile boolean deleted = false;

        @Override
        public K8sPodResource create(K8sResourceContext k8sResourceContext) throws JobException {
            this.createContext = k8sResourceContext;
            return buildByK8sContext(this.createContext);
        }

        @Override
        public Optional<K8sPodResource> get(String namespace, String arn) throws JobException {
            return Optional.of(buildByK8sContext(this.createContext));
        }

        @Override
        public String delete(String namespace, String arn) throws JobException {
            this.deleted = true;
            return namespace + ":" + arn;
        }

        private K8sPodResource buildByK8sContext(K8sResourceContext k8sResourceContext) {
            return new K8sPodResource(k8sResourceContext.region(), k8sResourceContext.getGroup(),
                    k8sResourceContext.resourceNamespace(), k8sResourceContext.getResourceName(),
                    ResourceState.CREATING,
                    "localhost:8080", new Date(1024));
        }
    }
}
