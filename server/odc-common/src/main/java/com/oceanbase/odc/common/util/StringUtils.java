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
package com.oceanbase.odc.common.util;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.text.StringSubstitutor;
import org.springframework.util.CollectionUtils;

import com.google.common.primitives.Chars;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.model.datatype.DataTypeUtil;

import lombok.NonNull;

/**
 * this is just a sample util class for package place holder <br>
 * 
 * <pre>
 * an util class only contains static method should
 * 1. end with 'Utils' postfix
 * 2. statement with 'abstract'
 * 3. contains a 'private constructor`
 * for avoid newInstance call reference of this class
 * </pre>
 * 
 * @author yizhou.xw
 * @version : StringUtils.java, v 0.1 2021-02-19 16:47
 */
public abstract class StringUtils extends org.apache.commons.lang3.StringUtils {
    /**
     * replace line breaker(s) and multiple white char to single blank char, change into an single line
     * string
     */
    private static final Pattern CHANGE_TO_SINGLE_LINE_STR_PATTERN = Pattern.compile("(\\s|\t|\r|\n)+");
    private static final Pattern PORT_PATTERN =
            Pattern.compile("^(([1-9]\\d{0,3}|[1-5]\\d{4}|6[0-4]\\d{3}|65[0-4]\\d{2}|655[0-2]\\d|6553"
                    + "[0-5]))$");
    private static final String ORACLE_ESCAPE_KEYWORD = "ESCAPE '\\'";
    private static final String DEFAULT_VARIABLE_PREFIX = "${";
    private static final String DEFAULT_VARIABLE_SUFFIX = "}";
    private static final char MYSQL_IDENTIFIER_WRAP_CHAR = '`';
    private static final char ORACLE_IDENTIFIER_WRAP_CHAR = '"';

    private StringUtils() {}

    public static Boolean checkMysqlIdentifierQuoted(final String str) {
        if (StringUtils.isBlank(str)) {
            return false;
        }
        return str.length() > 2 && str.startsWith(String.valueOf(MYSQL_IDENTIFIER_WRAP_CHAR))
                && str.endsWith(String.valueOf(MYSQL_IDENTIFIER_WRAP_CHAR));
    }

    public static Boolean checkOracleIdentifierQuoted(final String str) {
        if (StringUtils.isBlank(str)) {
            return false;
        }
        return str.length() > 2 && str.startsWith(String.valueOf(ORACLE_IDENTIFIER_WRAP_CHAR))
                && str.endsWith(String.valueOf(ORACLE_IDENTIFIER_WRAP_CHAR));
    }

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

    public static String unquoteOracleValue(final String str) {
        return unquoteSqlValue(str, '\'');
    }

    public static String unquoteMysqlValue(final String str) {
        return unquoteSqlValue(str, '\'', new char[] {'\'', '\\'});
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

    public static String unquoteSqlValue(final String str, final char wrapChar) {
        final char[] escapeChars = new char[] {wrapChar};
        return unquoteSqlValue(str, wrapChar, escapeChars);
    }

    public static String unquoteSqlValue(final String str, final char wrapChar, final char[] escapeChars) {
        if (null == str) {
            return null;
        }
        String unwrap = unwrap(str, wrapChar);
        return unescapeUseDouble(unwrap, escapeChars);
    }

    public static void quoteColumnDefaultValuesForMySQL(DBTable table) {
        if (!CollectionUtils.isEmpty(table.getColumns())) {
            table.getColumns().forEach(column -> {
                String defaultValue = column.getDefaultValue();
                if (StringUtils.isNotEmpty(defaultValue)) {
                    if (!isDefaultValueBuiltInFunction(column)) {
                        column.setDefaultValue("'".concat(defaultValue.replace("'", "''")).concat("'"));
                    }
                }
            });
        }
    }

    /**
     * Check whether the data_default contain built in function. Any of the synonyms for
     * CURRENT_TIMESTAMP have the same meaning as CURRENT_TIMESTAMP. These are CURRENT_TIMESTAMP(),
     * NOW(), LOCALTIME, LOCALTIME(), LOCALTIMESTAMP, and LOCALTIMESTAMP().
     */
    private static boolean isDefaultValueBuiltInFunction(DBTableColumn column) {
        return com.oceanbase.tools.dbbrowser.util.StringUtils.isEmpty(column.getDefaultValue())
                || (!DataTypeUtil.isStringType(column.getTypeName())
                        && column.getDefaultValue().trim().toUpperCase(Locale.getDefault())
                                .startsWith("CURRENT_TIMESTAMP"));
    }

    public static String singleLine(final String str) {
        if (StringUtils.isEmpty(str)) {
            return str;
        }
        Matcher matcher = CHANGE_TO_SINGLE_LINE_STR_PATTERN.matcher(str);
        return matcher.replaceAll(" ");
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
            if (Chars.contains(escapeChars, char1)) {
                sb.append(char1);
            }
        }
        return sb.toString();
    }

    public static String unescapeUseDouble(final String str, final char[] escapeChars) {
        if (null == str || null == escapeChars) {
            return null;
        }
        if (str.length() < 2 || escapeChars.length == 0) {
            return str;
        }
        char[] chars = str.toCharArray();
        StringBuilder sb = new StringBuilder();
        char lastChar = chars[0];
        sb.append(lastChar);
        boolean lastCharUnescaped = false;
        for (int i = 1; i < chars.length; i++) {
            char currentChar = chars[i];
            if (!lastCharUnescaped && Chars.contains(escapeChars, lastChar) && currentChar == lastChar) {
                lastCharUnescaped = true;
            } else {
                sb.append(currentChar);
                lastCharUnescaped = false;
            }
            lastChar = currentChar;
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

    public static String offerOracleEscapeKeyword() {
        return ORACLE_ESCAPE_KEYWORD;
    }

    public static String uuidNoHyphen() {
        return UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }

    public static String uuid() {
        return UUID.randomUUID().toString().toUpperCase();
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

    public static String getBitString(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        if (bytes != null) {
            for (byte b : bytes) {
                builder.append(getBitString(b));
            }
        }
        return builder.toString();
    }

    public static String getBitString(byte b) {
        return Integer.toBinaryString((b & 0xFF) + 0x100).substring(1);
    }

    /**
     * 去除字符串首尾单双引号，用于 csv 列头名称处理 <br>
     *
     * @param str
     * @return
     */
    public static String removeQuotation(String str) {
        String unwrap = unwrap(str, '\'');
        if (!Objects.equals(unwrap, str)) {
            return unwrap;
        }
        return unwrap(str, '"');
    }

    public static String urlEncode(String str) {
        try {
            return URLEncoder.encode(str, StandardCharsets.UTF_8.toString());
        } catch (Exception ignore) {
            return str;
        }
    }

    public static String urlDecode(String str) {
        try {
            return URLDecoder.decode(str, StandardCharsets.UTF_8.toString());
        } catch (Exception ignore) {
            return str;
        }
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

    public static String removeFirstStart(String str, String... removes) {
        if (str == null || removes == null || removes.length == 0) {
            return str;
        }
        String current = str;
        for (String remove : removes) {
            current = org.apache.commons.lang3.StringUtils.removeStart(str, remove);
            if (!org.apache.commons.lang3.StringUtils.equals(str, current)) {
                return current;
            }
        }
        return current;
    }

    public static Boolean checkPort(String port) {
        if (StringUtils.isEmpty(port)) {
            return false;
        }
        Matcher matcher = PORT_PATTERN.matcher(port);
        return matcher.matches();
    }

    public static String removeWhitespace(String input) {
        if (input == null) {
            return null;
        }
        return input.replaceAll("\\s+", "");
    }

    public static String camelCaseToSnakeCase(String camelCase) {
        String regex = "([a-z])([A-Z]+)";
        String replacement = "$1_$2";
        return StringUtils.lowerCase(camelCase.replaceAll(regex, replacement));
    }

    public static String getBriefSql(String sql, Integer maxLength) {
        if (sql == null) {
            return null;
        }
        if (maxLength == 0) {
            return "";
        }
        if (maxLength < 50) {
            return sql.substring(0, maxLength);
        }

        final int sqlLength = sql.length();
        if (sqlLength <= maxLength) {
            return sql;
        }
        String lowerCaseSql = sql.toLowerCase();
        final String from = "from";
        final String omit = "...";
        int fromIndex = lowerCaseSql.indexOf(from);
        // tree part
        if (fromIndex != -1) {
            int partLength = maxLength / 3;
            int fromPartEnd = Math.min(fromIndex + partLength, sqlLength);
            String middle = sql.substring(fromIndex, fromPartEnd);
            if (sqlLength - fromPartEnd <= partLength) {
                // means end part not need compress
                int beginPartEndPox = maxLength - (sqlLength - fromIndex);
                String beginPart = sql.substring(0, beginPartEndPox - omit.length()) + omit;
                return beginPart + sql.substring(fromIndex, sqlLength);
            }
            if (fromIndex <= partLength) {
                int endPartLength = maxLength - fromPartEnd;
                String endPart = omit + sql.substring(sqlLength - endPartLength + omit.length(), sqlLength);
                return sql.substring(0, fromPartEnd) + endPart;
            }
            String beginPart = sql.substring(0, partLength - omit.length()) + omit;
            int endPartLength = maxLength - (beginPart.length() + middle.length());
            String endPart = omit + sql.substring(sqlLength - endPartLength + omit.length(), sqlLength);
            return beginPart + middle + endPart;
        } else {
            int partLength = maxLength / 2;
            String start = sql.substring(0, partLength - omit.length());
            String end = sql.substring(sqlLength - partLength);
            return start + "..." + end;
        }
    }

    public static boolean startsWithIgnoreSpaceAndNewLines(@NonNull String target, @NonNull String prefix,
            boolean ignoreCase, int maxMatchLength) {
        char[] targets;
        char[] prefixes;
        if (ignoreCase) {
            targets = target.toLowerCase().toCharArray();
            prefixes = prefix.toLowerCase().toCharArray();
        } else {
            targets = target.toCharArray();
            prefixes = prefix.toCharArray();
        }
        int matchLength = 0;
        int j = 0;
        for (int i = 0; i < targets.length && j < prefixes.length && matchLength < maxMatchLength;) {
            char iChar = targets[i];
            char jChar = prefixes[j];
            if (Character.isWhitespace(iChar)) {
                i++;
                continue;
            } else if (Character.isWhitespace(jChar)) {
                j++;
                continue;
            }
            if (iChar != jChar) {
                return false;
            }
            i++;
            j++;
            matchLength++;
        }
        return matchLength >= maxMatchLength || j >= prefixes.length;
    }

    public static boolean isTranslatable(@NonNull String str) {
        return str.startsWith(DEFAULT_VARIABLE_PREFIX) && str.endsWith(DEFAULT_VARIABLE_SUFFIX);
    }

    public static String getTranslatableKey(@NonNull String str) {
        if (!isTranslatable(str)) {
            throw new IllegalStateException(str + " is not translatable");
        }
        return str.substring(DEFAULT_VARIABLE_PREFIX.length(), str.length() - DEFAULT_VARIABLE_SUFFIX.length());
    }
}
