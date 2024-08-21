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
package com.oceanbase.tools.sqlparser.statement.alter.table;

import java.util.List;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;
import com.oceanbase.tools.sqlparser.statement.common.RelationFactor;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * {@link AlterTable}
 *
 * @author yh263208
 * @date 2023-06-14 14:47
 * @since ODC_release_4.2.0
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class AlterTable extends BaseStatement {

    private RelationFactor relation;
    private String userVariable;
    private boolean external;
    private final List<AlterTableAction> alterTableActions;

    public AlterTable(@NonNull ParserRuleContext context,
            RelationFactor relation, List<AlterTableAction> alterTableActions) {
        super(context);
        this.relation = relation;
        this.alterTableActions = alterTableActions;
    }

    public AlterTable(@NonNull RelationFactor relation, List<AlterTableAction> alterTableActions) {
        this.relation = relation;
        this.alterTableActions = alterTableActions;
    }

    public String getSchema() {
        if (this.relation == null) {
            return null;
        }
        return this.relation.getSchema();
    }

    public String getTableName() {
        if (this.relation == null) {
            return null;
        }
        return this.relation.getRelation();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (this.external) {
            builder.append("ALTER EXTERNAL TABLE");
        } else {
            builder.append("ALTER TABLE");
        }
        if (getSchema() != null) {
            builder.append(" ").append(getSchema()).append(".").append(getTableName());
        } else if (getTableName() != null) {
            builder.append(" ").append(getTableName());
        }
        if (this.userVariable != null) {
            builder.append(this.userVariable);
        }
        if (CollectionUtils.isNotEmpty(this.alterTableActions)) {
            builder.append(" ").append(this.alterTableActions.stream()
                    .map(AlterTableAction::toString).collect(Collectors.joining(",")));
        }
        return builder.toString();
    }

}
