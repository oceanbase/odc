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
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * {@link PartitionPlanKeyConfig}
 *
 * @author yh263208
 * @date 2024-01-10 15:36
 * @since ODC_release_4.2.4
 * @see java.io.Serializable
 */
@Getter
@Setter
@ToString
public class PartitionPlanKeyConfig implements Serializable {

    private static final long serialVersionUID = 7176051008183974787L;
    private PartitionPlanStrategy strategy;
    private String partitionKey;
    private String partitionKeyInvoker;
    private Map<String, Serializable> partitionKeyInvokerParameters;

}
