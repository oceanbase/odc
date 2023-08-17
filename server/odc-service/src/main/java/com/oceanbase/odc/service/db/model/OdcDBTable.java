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
package com.oceanbase.odc.service.db.model;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;

import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.model.DBTablePartition;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionOption;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionType;

import lombok.NoArgsConstructor;
import lombok.NonNull;

@NoArgsConstructor
public class OdcDBTable extends DBTable {

    public OdcDBTable(@NonNull DBTable table) {
        BeanUtils.copyProperties(table, this);
    }

    @Override
    public List<DBTableColumn> getColumns() {
        if (CollectionUtils.isEmpty(super.getColumns())) {
            return super.getColumns();
        }
        return super.getColumns().stream().map(OdcDBTableColumn::new).collect(Collectors.toList());
    }

    @Override
    public DBTableStatsResponse getStats() {
        if (super.getStats() == null) {
            return null;
        }
        return new DBTableStatsResponse(super.getStats());
    }

    public String getTableName() {
        return this.name();
    }

    public void setTableName(String tableName) {
        super.setName(tableName);
    }

    public String getDatabaseName() {
        return this.getSchemaName();
    }

    public Boolean getPartitioned() {
        DBTablePartition partition = this.getPartition();
        if (partition != null) {
            DBTablePartitionOption option = partition.getPartitionOption();
            if (option == null) {
                return false;
            }
            return option.getType() != null && option.getType() != DBTablePartitionType.NOT_PARTITIONED;
        }
        return false;
    }

}
