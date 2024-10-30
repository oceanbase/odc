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
package com.oceanbase.odc.common.dfa;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import lombok.NonNull;

/**
 * {@link AbstractDfa}
 *
 * @author yh263208
 * @date 2024-09-04 20:21
 * @since ODC_release_4.3.2
 */
public abstract class AbstractDfa<STATE, INPUT> {

    private final List<DfaStateTransfer<STATE, INPUT>> dfaStateTransfers;

    public AbstractDfa(@NonNull List<DfaStateTransfer<STATE, INPUT>> dfaStateTransfers) {
        this.dfaStateTransfers = dfaStateTransfers;
    }

    public STATE next(INPUT input, STATE currentState) throws Exception {
        List<DfaStateTransfer<STATE, INPUT>> transfers = this.dfaStateTransfers.stream()
                .filter(t -> t.matchesState(currentState)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(transfers)) {
            throw new IllegalStateException("State " + currentState + " is the final state");
        }
        transfers = transfers.stream().filter(t -> t.matchesInput(input)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(transfers)) {
            throw new IllegalStateException("Unknown input " + input + " for state " + currentState);
        } else if (transfers.size() != 1) {
            throw new IllegalStateException("More than one routes for state "
                    + currentState + " and input " + input);
        }
        STATE nextState = transfers.get(0).next();
        onStateTransfer(currentState, nextState, input);
        return nextState;
    }

    protected abstract void onStateTransfer(STATE currentState, STATE nextState, INPUT input) throws Exception;

}
