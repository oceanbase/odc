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
package com.oceanbase.odc.service.partitionplan;

import com.oceanbase.odc.metadb.partitionplan.TablePartitionPlanEntity;
import com.oceanbase.odc.service.partitionplan.model.TablePartitionPlan;
import com.oceanbase.odc.service.partitionplan.model.TablePartitionPlanDetail;

public class TablePartitionPlanMapper {
    public TablePartitionPlan entityToModel(TablePartitionPlanEntity entity) {
        TablePartitionPlanDetail detail = TablePartitionPlanDetail.builder()
                .isAutoPartition(entity.getIsAutoPartition())
                .partitionInterval(entity.getPartitionInterval())
                .partitionNamingPrefix(entity.getPartitionNamingPrefix())
                .partitionNamingSuffixExpression(entity.getPartitionNamingSuffixExpression())
                .preCreatePartitionCount(entity.getPreCreatePartitionCount())
                .expirePeriod(entity.getExpirePeriod())
                .partitionIntervalUnit(entity.getPartitionIntervalUnit())
                .expirePeriodUnit(entity.getExpirePeriodUnit()).build();
        return TablePartitionPlan.builder()
                .tableName(entity.getTableName())
                .schemaName(entity.getSchemaName())
                .detail(detail).build();
    }

    public TablePartitionPlanEntity modelToEntity(TablePartitionPlan model) {
        return TablePartitionPlanEntity.builder()
                .isConfigEnable(model.getDetail().getIsAutoPartition())
                .expirePeriod(model.getDetail().getExpirePeriod())
                .expirePeriodUnit(model.getDetail().getExpirePeriodUnit())
                .partitionInterval(model.getDetail().getPartitionInterval())
                .partitionIntervalUnit(model.getDetail().getPartitionIntervalUnit())
                .partitionNamingPrefix(model.getDetail().getPartitionNamingPrefix())
                .partitionNamingSuffixExpression(model.getDetail().getPartitionNamingSuffixExpression())
                .preCreatePartitionCount(model.getDetail().getPreCreatePartitionCount())
                .tableName(model.getTableName())
                .schemaName(model.getSchemaName()).build();
    }
}
