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
package com.oceanbase.odc.service.session.util;

import java.util.List;

import javax.validation.constraints.NotEmpty;

import org.apache.commons.collections.CollectionUtils;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.common.util.VersionUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.constant.OdcConstants;
import com.oceanbase.odc.core.sql.parser.AbstractSyntaxTree;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.select.ExpressionReference;
import com.oceanbase.tools.sqlparser.statement.select.FromReference;
import com.oceanbase.tools.sqlparser.statement.select.NameReference;
import com.oceanbase.tools.sqlparser.statement.select.Projection;
import com.oceanbase.tools.sqlparser.statement.select.Select;
import com.oceanbase.tools.sqlparser.statement.select.SelectBody;
import com.oceanbase.tools.sqlparser.statement.select.oracle.Pivot;
import com.oceanbase.tools.sqlparser.statement.select.oracle.UnPivot;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SqlRewriteUtil {

    private static final String SELECT_ODC_INTERNAL_ROWID_STMT =
            ", ROWID AS \"" + OdcConstants.ODC_INTERNAL_ROWID + "\" ";

    public static String addInternalRowIdColumn(String sql, @NonNull AbstractSyntaxTree ast) {
        if (StringUtils.isBlank(sql)) {
            return sql;
        }
        Statement statement = ast.getStatement();
        if (!(statement instanceof Select)) {
            return sql;
        }
        Select select = (Select) statement;
        SelectBody selectBody = select.getSelectBody();
        String queryOptions = selectBody.getQueryOptions();
        if ("distinct".equalsIgnoreCase(queryOptions) || "unique".equalsIgnoreCase(queryOptions)) {
            return sql;
        }
        List<FromReference> froms = selectBody.getFroms();
        if (CollectionUtils.isEmpty(froms) || froms.size() != 1) {
            return sql;
        }
        FromReference from = froms.get(0);
        Pivot pivot = null;
        UnPivot unPivot = null;
        boolean containsdblink = false;
        if (from instanceof NameReference) {
            NameReference nameFrom = (NameReference) from;
            pivot = nameFrom.getPivot();
            unPivot = nameFrom.getUnPivot();
            /**
             * 如果 {@link containsdblink} 为 true 意味着表名是 {@code xxx@xxx} 样式的，这代表着 dblink，含有 dblink 的情况下不能改写。
             */
            containsdblink = StringUtils.startsWith(nameFrom.getUserVariable(), "@");
        } else if (from instanceof ExpressionReference) {
            ExpressionReference exprFrom = (ExpressionReference) from;
            pivot = exprFrom.getPivot();
            unPivot = exprFrom.getUnPivot();
        }
        if (pivot != null || unPivot != null || containsdblink) {
            return sql;
        }
        StringBuilder newSql = new StringBuilder(sql);
        List<Projection> selectItems = selectBody.getSelectItems();
        int lastSelectItemStop = selectItems.get(selectItems.size() - 1).getStop();
        newSql.insert(lastSelectItemStop + 1, SELECT_ODC_INTERNAL_ROWID_STMT);
        Projection star = new Projection();
        if (selectItems.contains(star)) {
            if (!(from instanceof NameReference)) {
                return sql;
            }
            NameReference table = (NameReference) from;
            String tableName;
            if (table.getAlias() != null) {
                tableName = table.getAlias();
            } else {
                tableName = (table.getSchema() == null ? "" : (table.getSchema() + "."))
                        + table.getRelation();
            }
            int starIndex = selectItems.indexOf(star);
            newSql.insert(selectItems.get(starIndex).getStart(), tableName + ".");
        }
        return newSql.toString();
    }

    public static String addQueryLimit(@NotEmpty String sql, ConnectionSession session, Long maxRows) {
        StringBuilder result = new StringBuilder("select * from (");
        result.append(sql.endsWith(";") ? sql.substring(0, sql.length() - 1) : sql).append(")");

        if (session.getDialectType() == DialectType.MYSQL) {
            result.append(" as ").append(OdcConstants.ODC_INTERNAL_RESULT_SET);
        }
        if (session.getDialectType().isMysql()) {
            result.append(" limit ").append(maxRows);
        } else {
            if (VersionUtils.isGreaterThanOrEqualsTo(ConnectionSessionUtil.getVersion(session), "2.2.50")) {
                result.append(" fetch first ").append(maxRows).append(" rows only");
            } else {
                result.append(" where rownum <= ").append(maxRows);
            }
        }

        String explain = "explain " + result;
        try {
            session.getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY).execute(explain);
        } catch (Exception e) {
            log.warn("failed to add max-rows limit, will use original sql, reason:{}", e.getMessage());
            return sql;
        }
        return result.toString();
    }

}
