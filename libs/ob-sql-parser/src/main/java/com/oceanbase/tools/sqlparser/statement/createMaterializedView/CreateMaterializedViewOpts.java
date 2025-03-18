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
package com.oceanbase.tools.sqlparser.statement.createMaterializedView;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2025/3/17 20:59
 * @since: 4.3.4
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class CreateMaterializedViewOpts extends BaseStatement {
    // This property is set to false if there is no relevant content in the sql
    private boolean enableQueryRewrite;
    // This property is set to false if there is no relevant content in the sql
    private boolean enableQueryComputation;
    private MaterializedViewRefreshOpts materializedViewRefreshOpts;
}
