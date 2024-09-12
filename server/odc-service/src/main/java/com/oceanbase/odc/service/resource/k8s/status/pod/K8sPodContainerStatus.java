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
package com.oceanbase.odc.service.resource.k8s.status.pod;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import io.kubernetes.client.openapi.models.V1ContainerStatus;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

@Getter
@EqualsAndHashCode
public class K8sPodContainerStatus {

    private final boolean ready;
    private final boolean running;
    private final boolean waiting;
    private final String waitingReason;
    private final boolean terminated;

    public static Set<K8sPodContainerStatus> getNonErrorStatuses() {
        Set<K8sPodContainerStatus> statuses = new HashSet<>();
        statuses.add(getRunningStatus());
        statuses.addAll(getCreatingStatuses());
        return statuses;
    }

    public static Set<K8sPodContainerStatus> getCreatingStatuses() {
        return new HashSet<>(Arrays.asList(new K8sPodContainerStatus(false, true, false, null, false),
                new K8sPodContainerStatus(false, false, true, "Pending", false),
                new K8sPodContainerStatus(false, false, true, "ContainerCreating", false)));
    }

    public static K8sPodContainerStatus getRunningStatus() {
        return new K8sPodContainerStatus(true, true, false, null, false);
    }

    public K8sPodContainerStatus(@NonNull V1ContainerStatus status) {
        Validate.notNull(status.getReady());
        Validate.notNull(status.getState());
        this.ready = status.getReady();
        this.running = status.getState().getRunning() != null;
        this.waiting = status.getState().getWaiting() != null;
        if (status.getState().getWaiting() == null
                || status.getState().getWaiting().getReason() == null) {
            this.waitingReason = null;
        } else {
            this.waitingReason = status.getState().getWaiting().getReason().toUpperCase();
        }
        this.terminated = status.getState().getTerminated() != null;
    }

    private K8sPodContainerStatus(boolean ready, boolean running,
            boolean waiting, String waitingReason, boolean terminated) {
        this.ready = ready;
        this.running = running;
        this.waiting = waiting;
        this.waitingReason = waitingReason == null ? null : waitingReason.toUpperCase();
        this.terminated = terminated;
    }

}
