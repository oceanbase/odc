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

package com.oceanbase.tools.sqlparser.statement.insert;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;
import com.oceanbase.tools.sqlparser.statement.common.oracle.LogErrors;
import com.oceanbase.tools.sqlparser.statement.common.oracle.Returning;
import com.oceanbase.tools.sqlparser.statement.insert.mysql.SetColumn;
import com.oceanbase.tools.sqlparser.statement.select.Select;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * {@link Insert}
 *
 * @author yh263208
 * @date 2023-11-08 17:01
 * @since ODC_release_4.2.3
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class Insert extends BaseStatement {

    private Returning returning;
    private LogErrors logErrors;
    private Select select;
    private boolean all;
    private boolean first;
    private boolean replace;
    private boolean ignore;
    private boolean highPriority;
    private boolean lowPriority;
    private boolean overwrite;
    private List<SetColumn> onDuplicateKeyUpdateColumns;
    private final List<InsertTable> tableInsert;
    private final ConditionalInsert conditionalInsert;

    public Insert(@NonNull ParserRuleContext context,
            List<InsertTable> tableInsert,
            ConditionalInsert conditionalInsert) {
        super(context);
        Validate.isTrue(CollectionUtils.isNotEmpty(tableInsert) || conditionalInsert != null);
        this.conditionalInsert = conditionalInsert;
        this.tableInsert = tableInsert == null ? Collections.emptyList() : tableInsert;
    }

    public Insert(@NonNull ParserRuleContext context, @NonNull Insert target) {
        super(context);
        this.returning = target.returning;
        this.logErrors = target.logErrors;
        this.select = target.select;
        this.all = target.all;
        this.first = target.first;
        this.replace = target.replace;
        this.ignore = target.ignore;
        this.onDuplicateKeyUpdateColumns = target.onDuplicateKeyUpdateColumns;
        this.tableInsert = target.tableInsert;
        this.overwrite = target.overwrite;
        this.highPriority = target.highPriority;
        this.lowPriority = target.lowPriority;
        this.conditionalInsert = target.conditionalInsert;
    }

    public Insert(List<InsertTable> tableInsert, ConditionalInsert conditionalInsert) {
        Validate.isTrue(CollectionUtils.isNotEmpty(tableInsert) || conditionalInsert != null);
        this.conditionalInsert = conditionalInsert;
        this.tableInsert = tableInsert == null ? Collections.emptyList() : tableInsert;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (this.replace) {
            builder.append("REPLACE");
        } else {
            builder.append("INSERT");
        }
        if (this.highPriority) {
            builder.append(" HIGH_PRIORITY");
        } else if (this.lowPriority) {
            builder.append(" LOW_PRIORITY");
        }
        if (this.all) {
            builder.append(" ALL");
        } else if (this.first) {
            builder.append(" FIRST");
        } else if (this.ignore) {
            builder.append(" IGNORE");
        }
        if (this.overwrite) {
            builder.append(" OVERWRITE");
        }
        if (CollectionUtils.isNotEmpty(this.tableInsert)) {
            builder.append(" ").append(this.tableInsert.stream()
                    .map(InsertTable::toString).collect(Collectors.joining("\n")));
        } else if (this.conditionalInsert != null) {
            builder.append(" ").append(this.conditionalInsert);
        }
        if (this.select != null) {
            builder.append(" ").append(this.select);
        }
        if (this.returning != null) {
            builder.append(" ").append(this.returning);
        }
        if (this.logErrors != null) {
            builder.append(" ").append(this.logErrors);
        }
        if (CollectionUtils.isNotEmpty(this.onDuplicateKeyUpdateColumns)) {
            builder.append(" ON DUPLICATE KEY UPDATE ").append(this.onDuplicateKeyUpdateColumns.stream()
                    .map(SetColumn::toString).collect(Collectors.joining(",")));
        }
        return builder.toString();
    }

}
