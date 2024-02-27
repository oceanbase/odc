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
package com.oceanbase.tools.dbbrowser.editor.mysql;

import com.oceanbase.tools.dbbrowser.model.DBTablePartition;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionType;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;

/**
 * @Author: Lebie
 * @Date: 2022/10/10 下午9:04
 * @Description: []
 */
public class OBMySQLLessThan2277PartitionEditor extends OBMySQLLessThan400DBTablePartitionEditor {

    @Override
    protected void appendDefinitions(DBTablePartition partition, SqlBuilder sqlBuilder) {
        DBTablePartitionType type = partition.getPartitionOption().getType();
        // ObMySQL 1479 HASH/KEY 分区不支持设置 definition
        if (type == DBTablePartitionType.HASH || type == DBTablePartitionType.KEY) {
            return;
        }
        super.appendDefinitions(partition, sqlBuilder);
    }

}
