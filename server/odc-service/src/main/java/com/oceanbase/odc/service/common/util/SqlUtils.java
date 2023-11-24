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
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.google.common.collect.Sets;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.constant.OdcConstants;
import com.oceanbase.odc.core.sql.split.OffsetString;
import com.oceanbase.odc.core.sql.split.SqlCommentProcessor;
import com.oceanbase.odc.core.sql.split.SqlSplitter;
import com.oceanbase.tools.dbbrowser.parser.SqlParser;
import com.oceanbase.tools.dbbrowser.parser.result.ParseSqlResult;
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

    private static final String SELECT_KEYWORD = "select";
    private static final int SELECT_KEYWORD_LENGTH = SELECT_KEYWORD.length();
    private static final String SELECT_ODC_INTERNAL_ROWID_STMT =
            " ROWID AS \"" + OdcConstants.ODC_INTERNAL_ROWID + "\",";

    private static final String FROM_KEYWORD = "from";
    private static final String LITERAL_STAR = "*";
    private static final String LITERAL_STAR_SUFFIX = ".*";
    private static final String STAR_REGEX = "\\*";
    private static final String DEFAULT_DELIMITER = ";";
    private static final String MULTI_SPACE_REGEX = "\\s+";


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
    public static List<String> split(ConnectionSession connectionSession, String sql, boolean removeCommentPrefix) {
        SqlCommentProcessor processor = ConnectionSessionUtil.getSqlCommentProcessor(connectionSession);
        return split(connectionSession.getDialectType(), processor, sql, removeCommentPrefix);
    }

    private static List<String> split(DialectType dialectType, SqlCommentProcessor processor, String sql,
            boolean removeCommentPrefix) {
        PreConditions.notBlank(processor.getDelimiter(), "delimiter", "Empty or blank delimiter is not allowed");
        if (DialectType.OB_ORACLE == dialectType
                && (";".equals(processor.getDelimiter()) || "/".equals(processor.getDelimiter()))) {
            SqlSplitter sqlSplitter = new SqlSplitter(PlSqlLexer.class, processor.getDelimiter());
            sqlSplitter.setRemoveCommentPrefix(removeCommentPrefix);
            List<String> sqls = sqlSplitter.split(sql);
            processor.setDelimiter(sqlSplitter.getDelimiter());
            return sqls;
        } else {
            StringBuffer buffer = new StringBuffer();
            List<String> sqls = processor.split(buffer, sql).stream().map(OffsetString::getStr).collect(Collectors.toList());
            String bufferStr = buffer.toString();
            if (bufferStr.trim().length() != 0) {
                // if buffer is not empty, there will be some errors in syntax
                log.warn("sql processor's buffer is not empty, there may be some errors. buffer={}", bufferStr);
                sqls.add(bufferStr);
            }
            return sqls;
        }
    }

    // For mysql sql
    public static String appendLimit(String originalSql, int queryLimit) {
        try {
            ParseSqlResult result = SqlParser.parseMysql(originalSql);
            if (result.isSupportLimit() && !result.isLimitClause()) {
                SqlCommentProcessor commentProcessor = new SqlCommentProcessor(DialectType.OB_MYSQL, false, false);
                StringBuilder stringBuilder = new StringBuilder();
                String sql = removeComments(commentProcessor, originalSql);
                sql = StringUtils.removeEnd(sql.trim(), ";");
                stringBuilder.append(sql).append(" limit ").append(queryLimit);
                return stringBuilder.toString();
            }
        } catch (Exception e) {
            log.warn("Failed to append limit due parse failed, will use original sentence, sql={}, errorMessage={}",
                    originalSql, StringUtils.substring(e.getMessage(), 0, 200));
        }

        return originalSql;
    }

    /**
     * add ROWID column for oracle mode
     * 
     * @param originalSql
     * @return
     */
    public static String addInternalROWIDColumn(String originalSql) {
        if (StringUtils.isBlank(originalSql)) {
            return originalSql;
        }
        SqlCommentProcessor commentProcessor = new SqlCommentProcessor(DialectType.OB_ORACLE, false, false);
        String sql = removeComments(commentProcessor, originalSql).trim();
        if (!StringUtils.startsWithIgnoreCase(sql, SELECT_KEYWORD)) {
            return originalSql;
        }
        if (!StringUtils.containsIgnoreCase(sql, FROM_KEYWORD)) {
            return originalSql;
        }
        /**
         * 只有 SELECT 语句中包含 *，且不包含 .* 的语句，才会尝试将 * 替换为 table.* 因为包含 .* 的语句，添加 ROWID 后语法是正确的，不需要改写
         */
        String[] fromSegs = originalSql.split("(?i)" + FROM_KEYWORD, 2);
        String beforeFrom = fromSegs[0];
        String afterFrom = fromSegs[1];
        if (beforeFrom.contains(LITERAL_STAR) && !beforeFrom.contains(LITERAL_STAR_SUFFIX)) {
            String tableName = afterFrom.trim().split(MULTI_SPACE_REGEX)[0];
            if (tableName.contains(DEFAULT_DELIMITER)) {
                tableName = tableName.replace(DEFAULT_DELIMITER, LITERAL_STAR_SUFFIX);
            } else {
                tableName = tableName + LITERAL_STAR_SUFFIX;
            }
            sql = originalSql.replaceFirst(STAR_REGEX, Matcher.quoteReplacement(tableName));
        }

        String select = StringUtils.substring(sql, 0, SELECT_KEYWORD_LENGTH);
        String afterSelect = StringUtils.substring(sql, SELECT_KEYWORD_LENGTH);
        return select + SELECT_ODC_INTERNAL_ROWID_STMT + afterSelect;
    }

    // For oracle sql on ob server > 2250
    public static String appendFetchFirst(String originalSql, int queryLimit) {
        try {
            ParseSqlResult result = SqlParser.parseOracle(originalSql);
            if (result.isSupportLimit() && !result.isFetchClause()) {
                SqlCommentProcessor commentProcessor = new SqlCommentProcessor(DialectType.OB_ORACLE, false, false);
                StringBuilder stringBuilder = new StringBuilder();
                String sql = removeComments(commentProcessor, originalSql);
                sql = StringUtils.removeEnd(sql.trim(), ";");
                stringBuilder.append(sql).append(" fetch first ").append(queryLimit).append(" rows only");
                return stringBuilder.toString();
            }
        } catch (Exception e) {
            log.warn("Failed to parse sql={}, will use original sentence", originalSql, e);
        }

        return originalSql;
    }

    public static ParseSqlResult parseOracle(String originalSql) {
        try {
            return SqlParser.parseOracle(originalSql);
        } catch (Exception e) {
            log.warn("Failed to parse sql, will use original sentence, sql={}, exception={}",
                    originalSql, ExceptionUtils.getRootCauseMessage(e));
            return null;
        }
    }

    // For oracle sql
    public static String appendRownumCondition(String originalSql, int queryLimit) {
        try {
            ParseSqlResult result = SqlParser.parseOracle(originalSql);
            if (result.isSupportLimit() && !result.isWhereClause()) {
                SqlCommentProcessor commentProcessor = new SqlCommentProcessor(DialectType.OB_ORACLE, false, false);
                StringBuilder stringBuilder = new StringBuilder();
                String sql = removeComments(commentProcessor, originalSql);
                sql = StringUtils.removeEnd(sql.trim(), ";");
                stringBuilder.append(sql).append(" where rownum <= ").append(queryLimit);
                return stringBuilder.toString();
            }
        } catch (Exception e) {
            log.warn("Failed to parse sql={}, will use original sentence", originalSql, e);
        }

        return originalSql;
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
            List<String> splitedSqls = processor.split(buffer, sql).stream().map(OffsetString::getStr).collect(Collectors.toList());
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
