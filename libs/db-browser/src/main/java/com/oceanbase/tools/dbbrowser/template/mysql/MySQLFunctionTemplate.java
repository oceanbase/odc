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
package com.oceanbase.tools.dbbrowser.template.mysql;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;

import com.oceanbase.tools.dbbrowser.model.DBFunction;
import com.oceanbase.tools.dbbrowser.model.DBPLParam;
import com.oceanbase.tools.dbbrowser.model.DBRoutineDataNature;
import com.oceanbase.tools.dbbrowser.util.MySQLSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;

/**
 * {@link MySQLFunctionTemplate}
 *
 * @author yh263208
 * @date 2023-02-22 14:08
 * @since db-browser_1.0.0-SNAPSHOT
 */
public class MySQLFunctionTemplate extends BaseMySQLPLTemplate<DBFunction> {

    @Override
    public String generateCreateObjectTemplate(@NotNull DBFunction dbObject) {
        Validate.notBlank(dbObject.getFunName(), "Function name can not be blank");
        Validate.notBlank(dbObject.getReturnType(), "Function return type can not be blank");
        SqlBuilder sqlBuilder = new MySQLSqlBuilder();
        sqlBuilder.append("create function ")
                .identifier(dbObject.getFunName())
                .append("(");
        List<DBPLParam> paramList = dbObject.getParams();
        if (CollectionUtils.isNotEmpty(paramList)) {
            sqlBuilder.append(paramList.stream().map(p -> "\n\t" + p.getParamName() + " " + p.getDataType())
                    .collect(Collectors.joining(",")));
        }
        sqlBuilder.append(")")
                .append("\nreturns ").append(dbObject.getReturnType()).line();

        if (Objects.nonNull(dbObject.getCharacteristic())) {
            if (Objects.nonNull(dbObject.getCharacteristic().getComment())) {
                sqlBuilder.space().append("COMMENT ").value(dbObject.getCharacteristic().getComment()).line();
            }
            if (Objects.nonNull(dbObject.getCharacteristic().getDeterministic())
                    && dbObject.getCharacteristic().getDeterministic()) {
                sqlBuilder.space().append("DETERMINISTIC").line();
            }
            if (Objects.nonNull(dbObject.getCharacteristic().getDataNature())) {
                switch (dbObject.getCharacteristic().getDataNature()) {
                    case CONTAINS_SQL:
                        sqlBuilder.space().append(DBRoutineDataNature.CONTAINS_SQL.getValue()).line();
                        break;
                    case NO_SQL:
                        sqlBuilder.space().append(DBRoutineDataNature.NO_SQL.getValue()).line();
                        break;
                    case READS_SQL:
                        sqlBuilder.space().append(DBRoutineDataNature.READS_SQL.getValue()).line();
                        break;
                    case MODIFIES_SQL:
                        sqlBuilder.space().append(DBRoutineDataNature.MODIFIES_SQL.getValue()).line();
                        break;
                }
            }
            if (Objects.nonNull(dbObject.getCharacteristic().getSqlSecurity())) {
                sqlBuilder.space().append("SQL SECURITY ").append(dbObject.getCharacteristic().getSqlSecurity()).line();
            }
        }

        sqlBuilder.append("begin").line()
                .append("\t-- Enter your function code").line()
                .append("end");
        return sqlBuilder.toString();
    }

}
