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

import java.util.Objects;

import com.oceanbase.tools.dbbrowser.model.DBView;
import com.oceanbase.tools.dbbrowser.model.DBViewCheckOption;
import com.oceanbase.tools.dbbrowser.template.BaseViewTemplate;
import com.oceanbase.tools.dbbrowser.util.OracleSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;

/**
 * {@link OracleViewTemplate}
 *
 * @author yh263208
 * @date 2023-02-23 20:05
 * @since db-browser_1.0.0_SNAPSHOT
 */
public class OracleViewTemplate extends BaseViewTemplate {

    @Override
    protected String preHandle(String str) {
        return str.toUpperCase();
    }

    @Override
    protected SqlBuilder sqlBuilder() {
        return new OracleSqlBuilder();
    }

    @Override
    protected String doGenerateCreateObjectTemplate(SqlBuilder sqlBuilder, DBView dbObject) {
        // add check options with read only
        if (Objects.equals(dbObject.getCheckOption(), DBViewCheckOption.READ_ONLY)) {
            sqlBuilder.append("\n").append("WITH READ ONLY");
        }
        return sqlBuilder.toString();
    }

}
