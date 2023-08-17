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
package com.oceanbase.tools.dbbrowser.template.oracle;

import java.util.List;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;

import com.oceanbase.tools.dbbrowser.model.DBTrigger;
import com.oceanbase.tools.dbbrowser.model.DBTriggerReference;
import com.oceanbase.tools.dbbrowser.template.DBObjectTemplate;
import com.oceanbase.tools.dbbrowser.util.OracleSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;
import com.oceanbase.tools.dbbrowser.util.StringUtils;

/**
 * {@link OracleTriggerTemplate}
 *
 * @author yh263208
 * @date 2023-02-23 16:12
 * @since db-browser_1.0.0_SNAPSHOT
 */
public class OracleTriggerTemplate implements DBObjectTemplate<DBTrigger> {

    @Override
    public String generateCreateObjectTemplate(@NotNull DBTrigger dbObject) {
        Validate.notBlank(dbObject.getTriggerName(), "Trigger name can not be blank");
        Validate.notNull(dbObject.getTriggerMode(), "Trigger mode can not be null");
        Validate.notEmpty(dbObject.getTriggerEvents(), "Trigger events can not be blank");
        Validate.notBlank(dbObject.getSchemaName(), "Schema name can not be blank");
        Validate.notBlank(dbObject.getSchemaMode(), "Schema mode can not be blank");

        SqlBuilder sqlBuilder = new OracleSqlBuilder();
        sqlBuilder.append("CREATE OR REPLACE TRIGGER ")
                .identifier(dbObject.getTriggerName())
                .append(" ").append(dbObject.getTriggerMode().name());
        String events = dbObject.getTriggerEvents().stream().map(event -> {
            StringBuilder builder = new StringBuilder();
            builder.append(event.getDmlEvent().name());
            String column = event.getColumn();
            if (StringUtils.isNotBlank(column)) {
                builder.append(" OF ").append(StringUtils.quoteOracleIdentifier(column));
            }
            return builder.toString();
        }).collect(Collectors.joining(" OR "));
        sqlBuilder.append("\n\t").append(events)
                .append(" ON ").identifier(dbObject.getSchemaName());
        List<DBTriggerReference> references = dbObject.getReferences();
        if (CollectionUtils.isNotEmpty(references)) {
            String refers = references.stream().map(r -> r.getReferenceType().name() + " AS " + r.getReferName())
                    .collect(Collectors.joining(" "));
            sqlBuilder.append("\n\tREFERENCING ").append(refers);
        }
        if (dbObject.getRowLevel()) {
            sqlBuilder.append("\n\tFOR EACH ROW");
        }
        if (dbObject.isEnable()) {
            sqlBuilder.append("\n\tENABLE");
        } else {
            sqlBuilder.append("\n\tDISABLE");
        }
        if (StringUtils.isNotBlank(dbObject.getSqlExpression())) {
            sqlBuilder.append("\n\tWHEN (")
                    .append(dbObject.getSqlExpression())
                    .append(")");
        }
        return sqlBuilder.append("\nBEGIN\n\t --your trigger body\nEND;").toString();
    }

}
