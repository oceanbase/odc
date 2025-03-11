/*
 * Copyright (c) 2025 OceanBase.
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

package com.oceanbase.tools.dbbrowser.template;

import com.oceanbase.tools.dbbrowser.model.DBMView;
import com.oceanbase.tools.dbbrowser.model.DBView;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;
import org.apache.commons.lang3.Validate;

import javax.validation.constraints.NotNull;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2025/3/10 21:46
 * @since: 4.3.4
 */
public abstract class BaseMViewTemplate extends BaseViewTemplate{
    // CREATE MATERIALIZED VIEW view_name
    // 1. column_list
    // 2. primary key
    // 3. table_option_list
    // 4. partition_option
    // 5. mv_column_group_option(存储格式)：行存,列存，列存+行存
    // 6. refresh_clause ：REFRESH [COMPLETE | FAST | FORCE] [mv_refresh_on_clause] | NEVER REFRESH
    // 7. mv_refresh_on_clause ：    [ON DEMAND] [[START WITH expr] [NEXT expr]]
    // 8. query_rewrite_clause
    // 9. on_query_computation_clause
    // AS view_select_stmt
    //CREATE MATERIALIZED VIEW view_name [([column_list] [PRIMARY KEY(column_list)])] [table_option_list] [partition_option] [mv_column_group_option] [refresh_clause] [query_rewrite_clause] [on_query_computation_clause] AS view_select_stmt;
    //
    //column_list:
    //    column_name [, column_name ...]
    //
    //refresh_clause:
    //    REFRESH [COMPLETE | FAST | FORCE] [mv_refresh_on_clause]
    //    | NEVER REFRESH
    //
    //mv_refresh_on_clause:
    //    [ON DEMAND] [[START WITH expr] [NEXT expr]]
    //
    //query_rewrite_clause:
    //    DISABLE QUERY REWRITE
    //    | ENABLE QUERY REWRITE
    //
    //on_query_computation_clause:
    //    DISABLE ON QUERY COMPUTATION
    //    | ENABLE ON QUERY COMPUTATION
    //
    //mv_column_group_option:
    //    WITH COLUMN GROUP(all columns)
    //    | WITH COLUMN GROUP(each column)
    //    | WITH COLUMN GROUP(all columns, each column)

    @Override
    public String generateCreateObjectTemplate(@NotNull DBView dbObject) {
        Validate.notBlank(dbObject.getViewName(), "View name can not be blank");
        Validate.isTrue(dbObject.getOperations() == null
                || dbObject.getViewUnits() != null,
            "Unable to calculate while operation set but table not set");
        Validate.isTrue(dbObject.getOperations() != null
                || dbObject.getViewUnits() == null
                || dbObject.getViewUnits().size() == 1,
            "Unable to calculate while operation not set but table size not 1");
        Validate.isTrue(dbObject.getOperations() == null
                || dbObject.getViewUnits() == null
                || dbObject.getOperations().size() == 0 && dbObject.getViewUnits().size() == 0
                || dbObject.getOperations().size() == dbObject.getViewUnits().size() - 1,
            "Unable to calculate, operationSize<>tableSize-1");
        SqlBuilder sqlBuilder = sqlBuilder();
        sqlBuilder.append(preHandle("create or replace view "))
            .identifier(dbObject.getViewName())
            .append(preHandle(" as"));
        generateQueryStatement(dbObject, sqlBuilder);
        return doGenerateCreateObjectTemplate(sqlBuilder, dbObject);
    }
}
