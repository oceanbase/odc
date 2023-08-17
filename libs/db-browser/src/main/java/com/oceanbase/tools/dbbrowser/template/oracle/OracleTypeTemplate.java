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

import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.Validate;

import com.oceanbase.tools.dbbrowser.model.DBType;
import com.oceanbase.tools.dbbrowser.model.DBTypeCode;
import com.oceanbase.tools.dbbrowser.util.OracleSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;

/**
 * {@link OracleTypeTemplate}
 *
 * @author yh263208
 * @date 2023-02-23 17:13
 * @since db-browser_1.0.0-SNAPSHOT
 */
public class OracleTypeTemplate extends BaseOraclePLTemplate<DBType> {

    @Override
    public String generateCreateObjectTemplate(@NotNull DBType dbObject) {
        Validate.notBlank(dbObject.getTypeName(), "Type name can not be blank");
        Validate.notNull(dbObject.getTypeCode(), "Type code can not be null");
        SqlBuilder sqlBuilder = new OracleSqlBuilder();
        sqlBuilder.append("CREATE OR REPLACE TYPE ")
                .identifier(dbObject.getTypeName());
        if (DBTypeCode.OBJECT == dbObject.getTypeCode()) {
            sqlBuilder.append("\nAS OBJECT")
                    .append("(/* TODO enter attribute and method declarations here */)");
        } else if (DBTypeCode.VARRAY == dbObject.getTypeCode()) {
            sqlBuilder.append("\nAS VARRAY")
                    .append("(/* array size */) OF /* datatype */");
        } else {
            sqlBuilder.append("\nAS TABLE OF /* datatype */");
        }
        return sqlBuilder.toString();
    }

}
