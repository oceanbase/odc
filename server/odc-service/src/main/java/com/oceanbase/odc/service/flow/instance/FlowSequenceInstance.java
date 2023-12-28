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
package com.oceanbase.odc.service.flow.instance;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

/**
 * {@link FlowSequenceInstance}
 *
 * @author yh263208
 * @date 2022-02-24 21:40
 * @since ODC_release_3.3.0
 */
@Getter
@ToString
@EqualsAndHashCode
public class FlowSequenceInstance {

    private final FlowNodeInstanceKey source;
    private final FlowNodeInstanceKey target;

    public FlowSequenceInstance(@NonNull FlowNodeInstanceKey source, @NonNull FlowNodeInstanceKey target) {
        this.source = source;
        this.target = target;
    }

    public FlowSequenceInstance(@NonNull BaseFlowNodeInstance source, @NonNull BaseFlowNodeInstance target) {
        this.source = new FlowNodeInstanceKey(source);
        this.target = new FlowNodeInstanceKey(target);
    }
}
