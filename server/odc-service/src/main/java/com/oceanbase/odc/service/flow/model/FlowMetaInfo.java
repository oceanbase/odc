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
package com.oceanbase.odc.service.flow.model;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import com.oceanbase.odc.metadb.flow.UserTaskInstanceEntity;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

/**
 * Meta info for flow module
 *
 * @author yh263208
 * @date 2022-03-16 16:68
 * @since ODC_release_3.3.0
 */
@Getter
@Setter
@ToString
public class FlowMetaInfo {
    private Set<Long> pendingApprovalInstanceIds;

    public static FlowMetaInfo of(@NonNull Collection<UserTaskInstanceEntity> entities) {
        FlowMetaInfo metaInfo = new FlowMetaInfo();
        if (entities.isEmpty()) {
            metaInfo.setPendingApprovalInstanceIds(Collections.emptySet());
            return metaInfo;
        }
        metaInfo.setPendingApprovalInstanceIds(
                entities.stream().map(UserTaskInstanceEntity::getId).collect(Collectors.toSet()));
        return metaInfo;
    }
}
