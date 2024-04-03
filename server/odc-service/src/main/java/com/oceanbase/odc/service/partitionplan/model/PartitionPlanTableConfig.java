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
package com.oceanbase.odc.service.partitionplan.model;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.plugin.task.api.partitionplan.invoker.drop.KeepMostLatestPartitionGenerator;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

/**
 * {@link PartitionPlanTableConfig}
 *
 * @author yh263208
 * @date 2024-01-09 16:49
 * @since ODC_release_4.2.4
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class PartitionPlanTableConfig implements Serializable {

    private static final long serialVersionUID = 7099051008183574787L;
    private Long id;
    private boolean enabled;
    private String tableName;
    private List<PartitionPlanKeyConfig> partitionKeyConfigs;
    private String partitionNameInvoker;
    private Map<String, Object> partitionNameInvokerParameters;

    public PartitionPlanTableConfig(@NonNull PartitionPlanTableConfig tableConfig) {
        this.id = tableConfig.id;
        this.enabled = tableConfig.enabled;
        this.tableName = tableConfig.tableName;
        this.partitionKeyConfigs = tableConfig.partitionKeyConfigs;
        this.partitionNameInvoker = tableConfig.partitionNameInvoker;
        this.partitionNameInvokerParameters = tableConfig.partitionNameInvokerParameters;
    }

    @JsonProperty(access = Access.READ_ONLY)
    public boolean isContainsCreateStrategy() {
        if (CollectionUtils.isEmpty(this.partitionKeyConfigs)) {
            return false;
        }
        return this.partitionKeyConfigs.stream().anyMatch(i -> i.getStrategy() == PartitionPlanStrategy.CREATE);
    }

    @JsonProperty(access = Access.READ_ONLY)
    public boolean isContainsDropStrategy() {
        if (CollectionUtils.isEmpty(this.partitionKeyConfigs)) {
            return false;
        }
        return this.partitionKeyConfigs.stream().anyMatch(i -> i.getStrategy() == PartitionPlanStrategy.DROP);
    }

    @JsonProperty(access = Access.READ_ONLY)
    public Boolean getReloadIndexes() {
        if (CollectionUtils.isEmpty(this.partitionKeyConfigs)) {
            return null;
        }
        Optional<PartitionPlanKeyConfig> dropConfig = this.partitionKeyConfigs.stream()
                .filter(p -> p.getStrategy() == PartitionPlanStrategy.DROP).findFirst();
        if (!dropConfig.isPresent()) {
            return null;
        }
        Map<String, Object> parameters = dropConfig.get().getPartitionKeyInvokerParameters();
        Object value = parameters.get(KeepMostLatestPartitionGenerator.RELOAD_INDEXES);
        if (value == null) {
            return null;
        }
        return JsonUtils.fromJson(JsonUtils.toJson(value), Boolean.class);
    }

}
