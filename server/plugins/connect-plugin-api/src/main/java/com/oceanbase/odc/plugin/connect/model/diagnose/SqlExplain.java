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
package com.oceanbase.odc.plugin.connect.model.diagnose;

import lombok.Data;

/**
 * {@link SqlExplain}
 *
 * @author yh263208
 * @date 2023-03-10 14:22
 * @since ODC_release_4.2.0
 */
@Data
public class SqlExplain {
    private String expTree;
    private String outline;
    private String originalText;
    private Boolean showFormatInfo;
    private PlanGraph graph;
}
