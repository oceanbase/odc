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
package com.oceanbase.odc.core.shared.model;

import java.util.LinkedHashMap;

import lombok.Data;

/**
 * {@link PlanNode}
 *
 * @author yh263208
 * @date 2023-03-10 14:29
 * @since ODC_release_4.2.0
 */
@Data
public class PlanNode {
    private int id;
    private String name;
    private String operator;
    private String rowCount;
    private String cost;
    private int depth;
    private String outputFilter;
    private LinkedHashMap<String, PlanNode> children = new LinkedHashMap<>();
}
