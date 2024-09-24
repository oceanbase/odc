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

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.service.resource.ResourceState;
import com.oceanbase.odc.service.resource.k8s.model.K8sDeployment;
import com.oceanbase.odc.service.resource.k8s.status.K8sDeploymentStatusDfa;

import io.kubernetes.client.openapi.models.V1DeploymentSpec;

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
        ResourceState actual = dfa.next(getCreatingDeploymentWithNonEnoughReplicas(), ResourceState.CREATING);
        Assert.assertEquals(ResourceState.CREATING, actual);
    }

    @Test
    public void next_currentStateIsCreatingPodIsCreatingWithNonEnoughRep_nextStateCreating() throws Exception {
        K8sDeploymentStatusDfa dfa = K8sDeploymentStatusDfa.buildInstance(ResourceState.CREATING);
        ResourceState actual = dfa.next(getCreatingDeploymentWithEnoughReplicas(), ResourceState.CREATING);
        Assert.assertEquals(ResourceState.CREATING, actual);
    }

    @Test
    public void next_currentStateIsCreatingPodIsAllAvailable_nextStateAvailable() throws Exception {
        K8sDeploymentStatusDfa dfa = K8sDeploymentStatusDfa.buildInstance(ResourceState.CREATING);
        ResourceState actual = dfa.next(getAvailableDeployment(), ResourceState.CREATING);
        Assert.assertEquals(ResourceState.AVAILABLE, actual);
    }

    @Test
    public void next_currentStateIsCreatingPodIsFailed_nextStateError() throws Exception {
        K8sDeploymentStatusDfa dfa = K8sDeploymentStatusDfa.buildInstance(ResourceState.CREATING);
        ResourceState actual = dfa.next(getErrorDeployment(), ResourceState.CREATING);
        Assert.assertEquals(ResourceState.ERROR_STATE, actual);
    }

    @Test
    public void next_currentStateIsCreatingDeploymentIsAbsent_nextStateCreating() throws Exception {
        K8sDeploymentStatusDfa dfa = K8sDeploymentStatusDfa.buildInstance(ResourceState.CREATING);
        ResourceState actual = dfa.next(null, ResourceState.CREATING);
        Assert.assertEquals(ResourceState.CREATING, actual);
    }

    @Test
    public void next_currentStateIsAvailableDeploymentIsAbsent_nextStateCreating() throws Exception {
        K8sDeploymentStatusDfa dfa = K8sDeploymentStatusDfa.buildInstance(ResourceState.AVAILABLE);
        ResourceState actual = dfa.next(null, ResourceState.AVAILABLE);
        Assert.assertEquals(ResourceState.UNKNOWN, actual);
    }

    @Test
    public void next_currentStateIsDestroyingDeploymentIsAbsent_nextStateDestroyed() throws Exception {
        K8sDeploymentStatusDfa dfa = K8sDeploymentStatusDfa.buildInstance(ResourceState.DESTROYING);
        ResourceState actual = dfa.next(null, ResourceState.DESTROYING);
        Assert.assertEquals(ResourceState.DESTROYED, actual);
    }

    public static K8sDeployment getCreatingDeploymentWithNonEnoughReplicas() {
        K8sDeployment deployment = new K8sDeployment();
        V1DeploymentSpec spec = new V1DeploymentSpec();
        spec.setReplicas(3);
        deployment.setSpec(spec);
        deployment.setK8sPodList(Arrays.asList(
                K8sPodStatusDfaTest.getAvailablePod(),
                K8sPodStatusDfaTest.getAvailablePod()));
        return deployment;
    }

    public static K8sDeployment getCreatingDeploymentWithEnoughReplicas() {
        K8sDeployment deployment = new K8sDeployment();
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
        K8sDeployment deployment = new K8sDeployment();
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
        K8sDeployment deployment = new K8sDeployment();
        V1DeploymentSpec spec = new V1DeploymentSpec();
        spec.setReplicas(3);
        deployment.setSpec(spec);
        deployment.setK8sPodList(Arrays.asList(
                K8sPodStatusDfaTest.getAvailablePod(),
                K8sPodStatusDfaTest.getFailedErrorPod(),
                K8sPodStatusDfaTest.getAvailablePod()));
        return deployment;
    }

}
