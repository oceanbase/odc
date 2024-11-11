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
package com.oceanbase.tools.dbbrowser.parser;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import com.oceanbase.tools.dbbrowser.parser.constant.GeneralSqlType;
import com.oceanbase.tools.dbbrowser.parser.constant.SqlType;
import com.oceanbase.tools.dbbrowser.parser.result.BasicResult;
import com.oceanbase.tools.sqlparser.SyntaxErrorException;

import lombok.extern.slf4j.Slf4j;

/**
 * @author wenniu.ly
 * @date 2021/9/8
 */

@Slf4j
public class ParserUtil {

    private static final int DEFAULT_PREFIX_SIZE = 200;

    public static GeneralSqlType getGeneralSqlType(BasicResult result) {
        if (Objects.isNull(result.getSqlType())) {
            return GeneralSqlType.OTHER;
        }
        switch (result.getSqlType()) {
            case INSERT:
            case UPDATE:
            case DELETE:
            case SORT:
            case REPLACE:
                return GeneralSqlType.DML;
            case SELECT:
            case SHOW:
            case EXPLAIN:
                return GeneralSqlType.DQL;
            case CREATE:
            case DROP:
            case TRUNCATE:
            case ALTER:
            case COMMENT_ON:
                return GeneralSqlType.DDL;
            case UNKNOWN:
            default:
                return GeneralSqlType.OTHER;
        }
    }

    public static boolean isSelectType(SqlType type) {
        return SqlType.SELECT == type;
    }

    public static BasicResult parseMysqlType(String sql) {
        return parseMysqlType(sql, 0);
    }

    public static BasicResult parseMysqlType(String sql, long timeoutMillis) {
        BasicResult result = null;
        Boolean isSqlSyntaxError = false;
        Boolean isPLSqlSyntaxError = false;

        try {
            result = SqlParser.parseMysql(sql, timeoutMillis);
            result.setSyntaxError(false);
            if (Objects.nonNull(result.getSqlType())) {
                return result;
            }
        } catch (Exception e) {
            log.debug("Parse mysql type by SqlParser failed, sql={}, errorMessage={}",
                    sql, prefix(e.getMessage()));
            if (e instanceof SyntaxErrorException) {
                isSqlSyntaxError = true;
            }
        }

        try {
            result = PLParser.parseObMysql(sql, timeoutMillis);
        } catch (Exception e) {
            log.debug("Parse mysql type by PLParser failed, sql={}, errorMessage={}",
                    sql, prefix(e.getMessage()));
            if (e instanceof SyntaxErrorException) {
                isPLSqlSyntaxError = true;
            }
        }
        if (Objects.nonNull(result) && Objects.nonNull(result.getSqlType())) {
            result.setSyntaxError(isSqlSyntaxError && isPLSqlSyntaxError);
            return result;
        } else {
            BasicResult basicResult = new BasicResult(SqlType.UNKNOWN);
            basicResult.setSyntaxError(isSqlSyntaxError && isPLSqlSyntaxError);
            return basicResult;
        }
    }

    public static BasicResult parseOracleType(String sql) {
        return parseOracleType(sql, 0);
    }

    public static BasicResult parseOracleType(String sql, long timeoutMillis) {
        BasicResult result = null;
        Boolean isSqlSyntaxError = false;
        Boolean isPLSqlSyntaxError = false;
        try {
            result = SqlParser.parseOracle(sql, timeoutMillis);
            result.setSyntaxError(false);
            if (Objects.nonNull(result.getSqlType())) {
                return result;
            }
        } catch (Exception e) {
            log.debug("Parse oracle type by SqlParser failed, sql={}, errorMessage={}",
                    sql, prefix(e.getMessage()));
            if (e instanceof SyntaxErrorException) {
                isSqlSyntaxError = true;
            }
        }

        try {
            result = PLParser.parseObOracle(sql, timeoutMillis);
        } catch (Exception e) {
            log.debug("Parse oracle type by PLParser failed, sql={}, errorMessage={}",
                    sql, prefix(e.getMessage()));
            if (e instanceof SyntaxErrorException) {
                isPLSqlSyntaxError = true;
            }
        }
        if (Objects.nonNull(result) && Objects.nonNull(result.getSqlType())) {
            result.setSyntaxError(isSqlSyntaxError && isPLSqlSyntaxError);
            return result;
        } else {
            BasicResult basicResult = new BasicResult(SqlType.UNKNOWN);
            basicResult.setSyntaxError(isSqlSyntaxError && isPLSqlSyntaxError);
            return basicResult;
        }
    }

    /**
     * prefix string content for reduce log output size
     */
    private static String prefix(String content) {
        if (Objects.isNull(content)) {
            return null;
        }
        return StringUtils.substring(content, 0, DEFAULT_PREFIX_SIZE);
    }

}
