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

import com.oceanbase.tools.dbbrowser.model.DBPLParam;
import com.oceanbase.tools.dbbrowser.model.DBPLParamMode;
import com.oceanbase.tools.dbbrowser.model.DBProcedure;
import com.oceanbase.tools.dbbrowser.util.OracleSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;
import com.oceanbase.tools.dbbrowser.util.StringUtils;

/**
 * {@link OracleProcedureTemplate}
 *
 * @author yh263208
 * @date 2022-02-22 17:38
 * @since db-browser-1.0.0-SNAPSHOT
 */
public class OracleProcedureTemplate extends BaseOraclePLTemplate<DBProcedure> {

    @Override
    public String generateCreateObjectTemplate(@NotNull DBProcedure dbObject) {
        Validate.notBlank(dbObject.getProName(), "Procedure name can not be blank");
        SqlBuilder sqlBuilder = new OracleSqlBuilder();
        sqlBuilder.append("CREATE OR REPLACE PROCEDURE ").identifier(dbObject.getProName());
        List<DBPLParam> paramList = dbObject.getParams();
        if (CollectionUtils.isNotEmpty(paramList)) {
            String params = paramList.stream().map(p -> {
                StringBuilder builder = new StringBuilder();
                DBPLParamMode type = p.getParamMode();
                String defaultValue = p.getDefaultValue();
                builder.append("\n\t").append(p.getParamName())
                        .append(" ").append(generateInOutString(type))
                        .append(" ").append(p.getDataType());
                if (StringUtils.isNotBlank(defaultValue) && DBPLParamMode.IN == type) {
                    builder.append(" DEFAULT ").append(StringUtils.quoteOracleValue(defaultValue));
                }
                return builder.toString();
            }).collect(Collectors.joining(","));
            sqlBuilder.append("(").append(params).append(")");
        }
        sqlBuilder.append(" IS")
                .append("\nBEGIN")
                .append("\n\t-- Enter your procedure code")
                .append("\nEND");
        return sqlBuilder.toString();
    }

}
