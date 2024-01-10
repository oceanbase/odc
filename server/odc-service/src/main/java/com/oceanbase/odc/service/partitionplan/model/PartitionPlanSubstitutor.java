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

import org.springframework.context.i18n.LocaleContextHolder;

import lombok.NonNull;

/**
 * {@link PartitionPlanSubstitutor}
 *
 * @author yh263208
 * @date 2024-01-09 16:14
 * @since ODC_release_4.2.4
 */
public class PartitionPlanSubstitutor {

    private final PartitionPlanSubstitutorKey key;

    public PartitionPlanSubstitutor(@NonNull PartitionPlanSubstitutorKey key) {
        this.key = key;
    }

    public String getLocalizedName() {
        return this.key.translate(new Object[] {}, LocaleContextHolder.getLocale());
    }

    public String getSubstitutor() {
        return this.key.getSubstitutor();
    }

}
