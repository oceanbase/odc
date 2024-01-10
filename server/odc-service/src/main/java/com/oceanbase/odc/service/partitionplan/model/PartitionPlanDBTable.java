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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.model.DBTableIndex;
import com.oceanbase.tools.dbbrowser.model.DBTablePartition;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionOption;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionType;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * {@link PartitionPlanDBTable}
 *
 * @author yh263208
 * @date 2024-01-09 11:55
 * @since ODC_release_4.2.4
 * @see DBTable
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class PartitionPlanDBTable extends DBTable {

    private boolean inTablegroup;
    private List<PartitionPlanStrategy> strategies;

    public boolean isContainsGlobalIndexes() {
        List<DBTableIndex> indexList = getIndexes();
        if (CollectionUtils.isEmpty(indexList)) {
            return false;
        }
        return indexList.stream().anyMatch(i -> Boolean.TRUE.equals(i.getGlobal()));
    }

    public boolean isContainsCreateStrategy() {
        return CollectionUtils.containsAny(this.strategies, PartitionPlanStrategy.CREATE);
    }

    public boolean isContainsDropStrategy() {
        return CollectionUtils.containsAny(this.strategies, PartitionPlanStrategy.DROP);
    }

    public boolean isRangePartitioned() {
        DBTablePartition partition = getPartition();
        if (partition == null) {
            return false;
        }
        DBTablePartitionType partitionType = partition.getPartitionOption().getType();
        return Objects.equals(DBTablePartitionType.RANGE, partitionType)
                || Objects.equals(DBTablePartitionType.RANGE_COLUMNS, partitionType);
    }

    public String getPartitionMode() {
        DBTablePartition partition = getPartition();
        if (partition == null) {
            return "0";
        }
        int mode = 0;
        DBTablePartitionOption option = partition.getPartitionOption();
        switch (option.getType()) {
            case KEY:
                mode |= 0x1;
                break;
            case HASH:
                mode |= 0x2;
                break;
            case LIST:
                mode |= 0x4;
                break;
            case RANGE:
                mode |= 0x8;
                break;
            case LIST_COLUMNS:
                mode |= 0x10;
                break;
            case RANGE_COLUMNS:
                mode |= 0x20;
                break;
            default:
                return "0";
        }
        if (Boolean.TRUE.equals(partition.getSubpartitionTemplated())) {
            mode |= 0x40;
        }
        StringBuilder builder = new StringBuilder(mode);
        if (option.getExpression() != null) {
            builder.insert(0, option.getExpression().hashCode());
        }
        if (CollectionUtils.isNotEmpty(option.getColumnNames())) {
            List<DBTableColumn> columns = getColumns();
            Map<String, String> colName2TypeName = new HashMap<>();
            if (CollectionUtils.isNotEmpty(columns)) {
                colName2TypeName = columns.stream().collect(
                        Collectors.toMap(DBTableColumn::getName, DBTableColumn::getTypeName));
            }
            StringBuilder tmp = new StringBuilder();
            for (String col : option.getColumnNames()) {
                tmp.append(col).append(colName2TypeName.getOrDefault(col, "unknown"));
            }
            builder.insert(0, tmp.toString().hashCode());
        }
        return builder.toString();
    }

}
