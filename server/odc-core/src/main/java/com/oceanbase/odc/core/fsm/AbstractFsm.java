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
package com.oceanbase.odc.core.fsm;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * {@link AbstractFsm}
 *
 * @author yh263208
 * @date 2024-09-04 20:21
 * @since ODC_release_4.3.2
 */
public abstract class AbstractFsm<STATE, EVENT> {

    @Setter
    @Getter
    private STATE currentState;
    private final List<FsmStateTransfer<STATE, EVENT>> fsmStateTransfers;

    public AbstractFsm(@NonNull List<FsmStateTransfer<STATE, EVENT>> fsmStateTransfers) {
        this.fsmStateTransfers = fsmStateTransfers;
    }

    public AbstractFsm<STATE, EVENT> next(EVENT event) throws Exception {
        if (this.currentState == null) {
            throw new IllegalStateException("Current state is not set");
        }
        List<FsmStateTransfer<STATE, EVENT>> transfers = this.fsmStateTransfers.stream()
                .filter(t -> t.matchesState(this.currentState) && t.matchesEvent(event))
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(transfers)) {
            throw new IllegalStateException("Illegal state " + this.currentState + " for event " + event);
        } else if (transfers.size() != 1) {
            throw new IllegalStateException("More than one routes for state "
                    + this.currentState + " and event " + event);
        }
        STATE nextState = transfers.get(0).next();
        onStateTransfer(currentState, nextState, event);
        this.currentState = nextState;
        return this;
    }

    protected abstract void onStateTransfer(STATE currentState, STATE nextState, EVENT event) throws Exception;

}
