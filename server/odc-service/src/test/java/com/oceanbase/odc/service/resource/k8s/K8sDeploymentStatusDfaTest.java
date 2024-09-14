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

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.service.resource.ResourceState;
import com.oceanbase.odc.service.resource.k8s.model.K8sDeployment;
import com.oceanbase.odc.service.resource.k8s.model.K8sPod;
import com.oceanbase.odc.service.resource.k8s.status.K8sDeploymentStatusDfa;

import io.kubernetes.client.openapi.models.V1DeploymentSpec;
import lombok.Setter;

/**
 * Test cases for {@link K8sDeploymentStatusDfa}
 *
 * @author yh263208
 * @date 2024-09-12 18:10
 * @since ODC_release_4.3.2
 */
public class K8sDeploymentStatusDfaTest {

    @Test
    public void next_currentStateIsCreatingPodIsCreatingWithEnoughRep_nextStateCreating() throws Exception {
        K8sDeploymentStatusDfa dfa = K8sDeploymentStatusDfa.buildInstance(ResourceState.CREATING);
        dfa.setCurrentState(ResourceState.CREATING);
        ResourceState actual = dfa.next(getCreatingDeploymentWithNonEnoughReplicas()).getCurrentState();
        Assert.assertEquals(ResourceState.CREATING, actual);
    }

    @Test
    public void next_currentStateIsCreatingPodIsCreatingWithNonEnoughRep_nextStateCreating() throws Exception {
        K8sDeploymentStatusDfa dfa = K8sDeploymentStatusDfa.buildInstance(ResourceState.CREATING);
        dfa.setCurrentState(ResourceState.CREATING);
        ResourceState actual = dfa.next(getCreatingDeploymentWithEnoughReplicas()).getCurrentState();
        Assert.assertEquals(ResourceState.CREATING, actual);
    }

    @Test
    public void next_currentStateIsCreatingPodIsAllAvailable_nextStateAvailable() throws Exception {
        K8sDeploymentStatusDfa dfa = K8sDeploymentStatusDfa.buildInstance(ResourceState.CREATING);
        dfa.setCurrentState(ResourceState.CREATING);
        ResourceState actual = dfa.next(getAvailableDeployment()).getCurrentState();
        Assert.assertEquals(ResourceState.AVAILABLE, actual);
    }

    @Test
    public void next_currentStateIsCreatingPodIsFailed_nextStateError() throws Exception {
        K8sDeploymentStatusDfa dfa = K8sDeploymentStatusDfa.buildInstance(ResourceState.CREATING);
        dfa.setCurrentState(ResourceState.CREATING);
        ResourceState actual = dfa.next(getErrorDeployment()).getCurrentState();
        Assert.assertEquals(ResourceState.ERROR_STATE, actual);
    }

    @Test
    public void next_currentStateIsCreatingDeploymentIsAbsent_nextStateUnknown() throws Exception {
        K8sDeploymentStatusDfa dfa = K8sDeploymentStatusDfa.buildInstance(ResourceState.CREATING);
        dfa.setCurrentState(ResourceState.CREATING);
        ResourceState actual = dfa.next(null).getCurrentState();
        Assert.assertEquals(ResourceState.UNKNOWN, actual);
    }

    @Test
    public void next_currentStateIsDestroyingDeploymentIsAbsent_nextStateDestroyed() throws Exception {
        K8sDeploymentStatusDfa dfa = K8sDeploymentStatusDfa.buildInstance(ResourceState.DESTROYING);
        dfa.setCurrentState(ResourceState.DESTROYING);
        ResourceState actual = dfa.next(null).getCurrentState();
        Assert.assertEquals(ResourceState.DESTROYED, actual);
    }

    public static K8sDeployment getCreatingDeploymentWithNonEnoughReplicas() {
        MyK8sDeployment deployment = new MyK8sDeployment();
        V1DeploymentSpec spec = new V1DeploymentSpec();
        spec.setReplicas(3);
        deployment.setSpec(spec);
        deployment.setK8sPodList(Arrays.asList(
                K8sPodStatusDfaTest.getAvailablePod(),
                K8sPodStatusDfaTest.getAvailablePod()));
        return deployment;
    }

    public static K8sDeployment getCreatingDeploymentWithEnoughReplicas() {
        MyK8sDeployment deployment = new MyK8sDeployment();
        V1DeploymentSpec spec = new V1DeploymentSpec();
        spec.setReplicas(3);
        deployment.setSpec(spec);
        deployment.setK8sPodList(Arrays.asList(
                K8sPodStatusDfaTest.getRunningCreatingPod(),
                K8sPodStatusDfaTest.getAvailablePod(),
                K8sPodStatusDfaTest.getRunningCreatingPod()));
        return deployment;
    }

    public static K8sDeployment getAvailableDeployment() {
        MyK8sDeployment deployment = new MyK8sDeployment();
        V1DeploymentSpec spec = new V1DeploymentSpec();
        spec.setReplicas(3);
        deployment.setSpec(spec);
        deployment.setK8sPodList(Arrays.asList(
                K8sPodStatusDfaTest.getAvailablePod(),
                K8sPodStatusDfaTest.getAvailablePod(),
                K8sPodStatusDfaTest.getAvailablePod()));
        return deployment;
    }

    public static K8sDeployment getErrorDeployment() {
        MyK8sDeployment deployment = new MyK8sDeployment();
        V1DeploymentSpec spec = new V1DeploymentSpec();
        spec.setReplicas(3);
        deployment.setSpec(spec);
        deployment.setK8sPodList(Arrays.asList(
                K8sPodStatusDfaTest.getAvailablePod(),
                K8sPodStatusDfaTest.getFailedErrorPod(),
                K8sPodStatusDfaTest.getAvailablePod()));
        return deployment;
    }

    @Setter
    private static class MyK8sDeployment extends K8sDeployment {

        private List<K8sPod> k8sPodList;

        public List<K8sPod> getK8sPods() {
            return this.k8sPodList;
        }
    }

}
