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
package com.oceanbase.odc.service.common.util;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.sql.split.OffsetString;
import com.oceanbase.odc.core.sql.split.SqlCommentProcessor;
import com.oceanbase.odc.core.sql.split.SqlSplitter;
import com.oceanbase.tools.sqlparser.oracle.PlSqlLexer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SqlUtils {
    /**
     * mysql 5.7/oceanbase does not support UTC_TIMESTAMP as default value, however it was supported in
     * mysql 8.0, we keep here for further compatibility
     */
    private static final String ALL_CURRENT_TIMESTAMP_EXPRESSIONS_STR = "NOW(),"
            + "UTC_TIMESTAMP,UTC_TIMESTAMP(),LOCALTIME, LOCALTIME(),"
            + "LOCALTIMESTAMP,LOCALTIMESTAMP(),CURRENT_TIMESTAMP,CURRENT_TIMESTAMP()";
    private static final Set<String> ALL_CURRENT_TIMESTAMP_EXPRESSIONS =
            Sets.newHashSet(ALL_CURRENT_TIMESTAMP_EXPRESSIONS_STR.split(","));

    /**
     * quote for mysql default value <br>
     * 1. if match current timestamp expression, skip quote <br>
     * 2. else quote with '' <br>
     * 
     * @param value
     * @return quoted value if not current timestamp expression
     */
    public static String quoteMysqlDefaultValue(String value) {
        if (Objects.isNull(value)) {
            return null;
        }
        if (isCurrentTimestampExpression(value)) {
            return value;
        }
        return StringUtils.quoteMysqlValue(value);
    }

    public static boolean isCurrentTimestampExpression(String express) {
        if (StringUtils.isBlank(express)) {
            return false;
        }
        return ALL_CURRENT_TIMESTAMP_EXPRESSIONS.contains(express.toUpperCase());
    }

    /**
     * for executing batch sql, and we need to split sql script by delimiter
     *
     * @param dialectType dialectype
     * @param sql sql
     * @return splited sql
     */
    public static List<String> split(DialectType dialectType, String sql, String delimiter) {
        return splitWithOffset(dialectType, sql, delimiter).stream().map(OffsetString::getStr).collect(
                Collectors.toList());
    }

    public static List<OffsetString> splitWithOffset(DialectType dialectType, String sql, String delimiter) {
        SqlCommentProcessor processor = new SqlCommentProcessor(dialectType, true, true);
        processor.setDelimiter(delimiter);
        return split(dialectType, processor, sql, false);
    }

    /**
     * for executing batch sql, and we need to split sql script by delimiter
     *
     * @param connectionSession connection engine
     * @param sql sql
     * @return splited sql
     */
    public static List<String> split(ConnectionSession connectionSession, String sql,
            boolean removeCommentPrefix) {
        SqlCommentProcessor processor = ConnectionSessionUtil.getSqlCommentProcessor(connectionSession);
        return split(connectionSession.getDialectType(), processor, sql, removeCommentPrefix).stream()
                .map(OffsetString::getStr).collect(
                        Collectors.toList());
    }

    public static List<OffsetString> splitWithOffset(ConnectionSession connectionSession, String sql,
            boolean removeCommentPrefix) {
        SqlCommentProcessor processor = ConnectionSessionUtil.getSqlCommentProcessor(connectionSession);
        return split(connectionSession.getDialectType(), processor, sql, removeCommentPrefix);
    }



    private static List<OffsetString> split(DialectType dialectType, SqlCommentProcessor processor, String sql,
            boolean removeCommentPrefix) {
        PreConditions.notBlank(processor.getDelimiter(), "delimiter", "Empty or blank delimiter is not allowed");
        if (DialectType.OB_ORACLE == dialectType
                && (";".equals(processor.getDelimiter()) || "/".equals(processor.getDelimiter()))) {
            SqlSplitter sqlSplitter = new SqlSplitter(PlSqlLexer.class, processor.getDelimiter());
            sqlSplitter.setRemoveCommentPrefix(removeCommentPrefix);
            List<OffsetString> sqls = sqlSplitter.split(sql);
            processor.setDelimiter(sqlSplitter.getDelimiter());
            return sqls;
        } else {
            StringBuffer buffer = new StringBuffer();
            List<OffsetString> sqls = processor.split(buffer, sql);
            String bufferStr = buffer.toString();
            if (bufferStr.trim().length() != 0) {
                // if buffer is not empty, there will be some errors in syntax
                log.info("sql processor's buffer is not empty, there may be some errors. buffer={}", bufferStr);
                if (sqls.size() == 0) {
                    sqls.add(new OffsetString(0, bufferStr));
                } else {
                    sqls.add(new OffsetString(
                            sqls.get(sqls.size() - 1).getOffset() + sqls.get(sqls.size() - 1).getStr().length(),
                            bufferStr));
                }
            }
            return sqls;
        }
    }

    /**
     * the sql need to be rewritten to pass
     * <code>OdcSqlParser.supportLimitExpr(sql, connectionEngine)</code>. When original sqls are splited
     * by sql-pre-processor with comments, the sql list will be as follow: eg. ["select 1+2 from dual --
     * this is a single comment", "select 3+4 \/*this is a \n multi-comments*\/ from dual --
     * comments",...]
     *
     * The sql parser can only accept the sql like this: eg. "select 1+2 from dual" -> without any
     * comments and delimiter
     *
     * @param processor sql-pre-processor
     * @param sql input sql, eg. "select xxx from table -- this is a comment"
     * @return sql without comments. eg. "select xxx from table"
     */
    public static String removeComments(SqlCommentProcessor processor, String sql) {
        try {
            StringBuffer buffer = new StringBuffer();
            List<String> splitedSqls = processor.split(buffer, sql).stream().map(OffsetString::getStr)
                    .collect(Collectors.toList());
            String bufferStr = buffer.toString();
            /**
             * The input sql does not contain delimiters (eg. "select xxx from table -- this is a comment"),
             * non-PL object statements will be stored in the buffer after being processed by the preprocessing
             * module and the splitSqls<code>List<String> splitedSqls</code>list will be empty.
             */
            if (splitedSqls != null && splitedSqls.size() == 0) {
                /**
                 * if the input sql represents a non-PL objects, code does this
                 */
                while (true) {
                    if (bufferStr.endsWith("\n")) {
                        /**
                         * remove all <code>\n</code> from sqls
                         */
                        bufferStr = bufferStr.substring(0, bufferStr.length() - 1);
                    } else {
                        break;
                    }
                }
                return bufferStr;
            }
        } catch (Throwable e) {
            log.warn("something wrong when sql splitting. sql={}", sql, e);
        }
        return sql;
    }

    public static String anyLike(String fuzzyKeywords) {
        if (StringUtils.isEmpty(fuzzyKeywords)) {
            return fuzzyKeywords;
        }
        return "%" + StringUtils.escapeLike(fuzzyKeywords) + "%";
    }

}
