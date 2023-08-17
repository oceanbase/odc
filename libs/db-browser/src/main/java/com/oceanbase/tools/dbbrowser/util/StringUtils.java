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
package com.oceanbase.tools.dbbrowser.util;

import java.util.Map;
import java.util.Objects;

import org.apache.commons.text.StringSubstitutor;

/**
 * @author jingtian
 * @date
 * @since
 */
public abstract class StringUtils extends org.apache.commons.lang3.StringUtils {

    private static final char MYSQL_IDENTIFIER_WRAP_CHAR = '`';
    private static final char ORACLE_IDENTIFIER_WRAP_CHAR = '"';
    private static final String DEFAULT_VARIABLE_SUFFIX = "}";
    private static final String DEFAULT_VARIABLE_PREFIX = "${";

    /**
     * Operations on identifier
     */
    public static String quoteMysqlIdentifier(final String str) {
        return quoteSqlIdentifier(str, MYSQL_IDENTIFIER_WRAP_CHAR);
    }

    public static String quoteOracleIdentifier(final String str) {
        return quoteSqlIdentifier(str, ORACLE_IDENTIFIER_WRAP_CHAR);
    }

    static String quoteSqlIdentifier(final String str, final char wrapChar) {
        if (null == str) {
            return null;
        }
        String escaped = escapeUseDouble(str, new char[] {wrapChar});
        return wrapChar + escaped + wrapChar;
    }

    public static String unquoteMySqlIdentifier(final String str) {
        return unquoteSqlIdentifier(str, MYSQL_IDENTIFIER_WRAP_CHAR);
    }

    public static String unquoteOracleIdentifier(final String str) {
        return unquoteSqlIdentifier(str, ORACLE_IDENTIFIER_WRAP_CHAR);
    }

    public static String unquoteSqlIdentifier(final String str, final char wrapChar) {
        String escaped = unwrap(str, wrapChar);
        if (Objects.equals(escaped, str)) {
            return str;
        }
        return replace(escaped, "" + wrapChar + wrapChar, "" + wrapChar);
    }

    /**
     * Operations on value
     */
    public static String quoteOracleValue(final String str) {
        return quoteSqlValue(str, '\'');
    }

    public static String quoteMysqlValue(final String str) {
        return quoteSqlValue(str, '\'', new char[] {'\'', '\\'});
    }

    /**
     * for oracle mode
     */
    public static String quoteSqlValue(final String str, final char wrapChar) {
        final char[] escapeChars = new char[] {wrapChar};
        return quoteSqlValue(str, wrapChar, escapeChars);
    }

    /**
     * for mysql mode
     */
    public static String quoteSqlValue(final String str, final char wrapChar, final char[] escapeChars) {
        if (null == str) {
            return null;
        }
        String escaped = escapeUseDouble(str, escapeChars);
        return wrapChar + escaped + wrapChar;
    }

    /**
     * 转换名称，可以使用单引号或者反斜杠作为命名， 但是 oracle 模式以及 mysql 模式下下单引号和反斜杠在字符串中是作为转义符号出现的， <br>
     * 要想让单引号在字符串中也正常出现就必须在单引号的前面再加一个单引号作为转义。
     *
     * @param name 对象名称
     * @param escapeChar 转义字符
     * @return 返回转换后的字符串
     */
    public static String escapeUseDouble(String name, char escapeChar) {
        return escapeUseDouble(name, new char[] {escapeChar});
    }

    /**
     * escape characters use doubles, examples: <br>
     * - abc, [', \] --> abc <br>
     * - a'\, [', \] --> a''\\ <br>
     */
    public static String escapeUseDouble(final String str, final char[] escapeChars) {
        if (null == str || null == escapeChars) {
            return null;
        }
        char[] chars = str.toCharArray();
        StringBuilder sb = new StringBuilder();
        for (char char1 : chars) {
            sb.append(char1);
            if (contains(escapeChars, char1)) {
                sb.append(char1);
            }
        }
        return sb.toString();
    }

    /**
     * For like clause escaping in both mysql && oracle
     *
     * Oracle like clause needs Escape Keyword
     *
     * @param value
     * @return
     */
    public static String escapeLike(String value) {
        if (StringUtils.isEmpty(value)) {
            return value;
        }
        value = replace(value, "%", "\\%");
        return replace(value, "_", "\\_");
    }

    /**
     * Returns {@code true} if {@code target} is present as an element anywhere in {@code array}.
     *
     * @param array an array of {@code char} values, possibly empty
     * @param target a primitive {@code char} value
     * @return {@code true} if {@code array[i] == target} for some value of {@code i}
     */
    public static boolean contains(char[] array, char target) {
        for (char value : array) {
            if (value == target) {
                return true;
            }
        }
        return false;
    }


    public static String replaceVariables(final String template, final Map<String, String> valuesMap) {
        return replaceVariables(template, valuesMap, DEFAULT_VARIABLE_PREFIX, DEFAULT_VARIABLE_SUFFIX);
    }

    public static String replaceVariables(final String template, final Map<String, String> valuesMap,
            final String prefix, final String suffix) {
        if (StringUtils.isEmpty(template)) {
            return "";
        }
        if (valuesMap == null || valuesMap.isEmpty()) {
            return template;
        }
        StringSubstitutor sub = new StringSubstitutor(valuesMap, prefix, suffix).setDisableSubstitutionInValues(true);
        return sub.replace(template);
    }
}
