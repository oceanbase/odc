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

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.service.resource.ResourceState;
import com.oceanbase.odc.service.resource.k8s.model.K8sPod;
import com.oceanbase.odc.service.resource.k8s.status.K8sPodStatusDfa;

import io.kubernetes.client.openapi.models.V1ContainerState;
import io.kubernetes.client.openapi.models.V1ContainerStateRunning;
import io.kubernetes.client.openapi.models.V1ContainerStateTerminated;
import io.kubernetes.client.openapi.models.V1ContainerStateWaiting;
import io.kubernetes.client.openapi.models.V1ContainerStatus;
import io.kubernetes.client.openapi.models.V1PodStatus;

/**
 * Test cases for {@link K8sPodStatusDfa}
 *
 * @author yh263208
 * @date 2024-09-11 20:16
 * @since ODC_release_4.3.2
 */
public class K8sPodStatusDfaTest {

    @Test
    public void next_currentStateIsCreatingPodIsCreating_nextStateCreating() throws Exception {
        K8sPodStatusDfa dfa = K8sPodStatusDfa.getInstance();
        dfa.setCurrentState(ResourceState.CREATING);
        ResourceState actual = dfa.next(getRunningCreatingPod()).getCurrentState();
        Assert.assertEquals(ResourceState.CREATING, actual);
    }

    @Test
    public void next_currentStateIsPending_nextStateCreating() throws Exception {
        K8sPodStatusDfa dfa = K8sPodStatusDfa.getInstance();
        dfa.setCurrentState(ResourceState.CREATING);
        ResourceState actual = dfa.next(getPendingCreatingPod()).getCurrentState();
        Assert.assertEquals(ResourceState.CREATING, actual);
    }

    @Test
    public void next_currentStateIsAvailable_nextStateAvailable() throws Exception {
        K8sPodStatusDfa dfa = K8sPodStatusDfa.getInstance();
        dfa.setCurrentState(ResourceState.CREATING);
        ResourceState actual = dfa.next(getAvailablePod()).getCurrentState();
        Assert.assertEquals(ResourceState.AVAILABLE, actual);
    }

    @Test
    public void next_currentStateIsFailedError_nextStateErrorState() throws Exception {
        K8sPodStatusDfa dfa = K8sPodStatusDfa.getInstance();
        dfa.setCurrentState(ResourceState.CREATING);
        ResourceState actual = dfa.next(getFailedErrorPod()).getCurrentState();
        Assert.assertEquals(ResourceState.ERROR_STATE, actual);
    }

    @Test
    public void next_currentStateIsUnknownError_nextStateErrorState() throws Exception {
        K8sPodStatusDfa dfa = K8sPodStatusDfa.getInstance();
        dfa.setCurrentState(ResourceState.CREATING);
        ResourceState actual = dfa.next(getUnknownErrorPod()).getCurrentState();
        Assert.assertEquals(ResourceState.ERROR_STATE, actual);
    }

    @Test
    public void next_currentStateIsRunningError_nextStateErrorState() throws Exception {
        K8sPodStatusDfa dfa = K8sPodStatusDfa.getInstance();
        dfa.setCurrentState(ResourceState.CREATING);
        ResourceState actual = dfa.next(getRunningErrorPod()).getCurrentState();
        Assert.assertEquals(ResourceState.ERROR_STATE, actual);
    }

    @Test
    public void next_podIsNull_nextStateUnknown() throws Exception {
        K8sPodStatusDfa dfa = K8sPodStatusDfa.getInstance();
        dfa.setCurrentState(ResourceState.CREATING);
        ResourceState actual = dfa.next(null).getCurrentState();
        Assert.assertEquals(ResourceState.UNKNOWN, actual);
    }

    private K8sPod getRunningCreatingPod() {
        K8sPod k8sPod = new K8sPod();
        V1PodStatus status = new V1PodStatus();
        status.setPhase("Running");
        status.setContainerStatuses(Arrays.asList(getRunningStatus(true),
                getRunningStatus(false), getWaitingStatus("ContainerCreating")));
        k8sPod.setStatus(status);
        return k8sPod;
    }

    private K8sPod getFailedErrorPod() {
        K8sPod k8sPod = new K8sPod();
        V1PodStatus status = new V1PodStatus();
        status.setPhase("Failed");
        k8sPod.setStatus(status);
        return k8sPod;
    }

    private K8sPod getUnknownErrorPod() {
        K8sPod k8sPod = new K8sPod();
        V1PodStatus status = new V1PodStatus();
        status.setPhase("Unknown");
        k8sPod.setStatus(status);
        return k8sPod;
    }

    private K8sPod getRunningErrorPod() {
        K8sPod k8sPod = new K8sPod();
        V1PodStatus status = new V1PodStatus();
        status.setPhase("Running");
        status.setContainerStatuses(Arrays.asList(getRunningStatus(true), getRunningStatus(false),
                getWaitingStatus("ContainerCannotRun"), getTerminatedStatus("DeadlineExceeded")));
        k8sPod.setStatus(status);
        return k8sPod;
    }

    private K8sPod getPendingCreatingPod() {
        K8sPod k8sPod = new K8sPod();
        V1PodStatus status = new V1PodStatus();
        status.setPhase("Pending");
        k8sPod.setStatus(status);
        return k8sPod;
    }

    private K8sPod getAvailablePod() {
        K8sPod k8sPod = new K8sPod();
        V1PodStatus status = new V1PodStatus();
        status.setPhase("Running");
        status.setContainerStatuses(Arrays.asList(getRunningStatus(true), getRunningStatus(true)));
        k8sPod.setStatus(status);
        return k8sPod;
    }

    private V1ContainerStatus getRunningStatus(boolean ready) {
        V1ContainerStatus status = new V1ContainerStatus();
        status.setReady(ready);
        V1ContainerState state = new V1ContainerState();
        V1ContainerStateRunning running = new V1ContainerStateRunning();
        running.setStartedAt(OffsetDateTime.of(LocalDateTime.now(), ZoneOffset.UTC));
        state.setRunning(running);
        status.setState(state);
        return status;
    }

    private V1ContainerStatus getWaitingStatus(String reason) {
        V1ContainerStatus status = new V1ContainerStatus();
        status.setReady(false);
        V1ContainerState state = new V1ContainerState();
        V1ContainerStateWaiting waiting = new V1ContainerStateWaiting();
        waiting.setReason(reason);
        state.setWaiting(waiting);
        status.setState(state);
        return status;
    }

    private V1ContainerStatus getTerminatedStatus(String reason) {
        V1ContainerStatus status = new V1ContainerStatus();
        status.setReady(false);
        V1ContainerState state = new V1ContainerState();
        V1ContainerStateTerminated terminated = new V1ContainerStateTerminated();
        terminated.setReason(reason);
        state.setTerminated(terminated);
        status.setState(state);
        return status;
    }

}
