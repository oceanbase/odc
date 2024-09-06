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

public class K8sPodStatusDfaTransfer implements DfaStateTransfer<String, String> {

    private final String targetState;
    private final String nextState;
    private final String targetEvent;

    public K8sPodStatusDfaTransfer(String targetState, String nextState, String targetEvent) {
        this.targetState = targetState;
        this.nextState = nextState;
        this.targetEvent = targetEvent;
    }

    @Override
    public String next() {
        return this.nextState;
    }

    @Override
    public boolean matchesState(String s) {
        return this.targetState.equals(s);
    }

    @Override
    public boolean matchesInput(String s) {
        return this.targetEvent.equals(s);
    }

}
