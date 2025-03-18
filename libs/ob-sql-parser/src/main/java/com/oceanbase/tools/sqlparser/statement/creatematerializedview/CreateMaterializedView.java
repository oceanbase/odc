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
package com.oceanbase.tools.sqlparser.statement.creatematerializedview;

import java.util.List;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;
import com.oceanbase.tools.sqlparser.statement.common.ColumnGroupElement;
import com.oceanbase.tools.sqlparser.statement.createtable.Partition;
import com.oceanbase.tools.sqlparser.statement.createtable.TableOptions;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2025/3/17 20:28
 * @since: 4.3.4
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class CreateMaterializedView extends BaseStatement {
    private TableOptions tableOptions;
    private Partition partition;
    private List<ColumnGroupElement> columnGroupElements;
    private CreateMaterializedViewOpts createMaterializedViewOpts;

}
