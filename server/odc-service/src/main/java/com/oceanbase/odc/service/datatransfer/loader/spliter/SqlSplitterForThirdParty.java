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
package com.oceanbase.odc.service.datatransfer.loader.spliter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Getter;

public class SqlSplitterForThirdParty {
    private static final Pattern[] PATTERNS = new Pattern[] {
            Pattern.compile("(\\?*)(PROMPT|REM)( [^\\r\\n]*)?[\\n\\r]?\0", Pattern.CASE_INSENSITIVE),
            Pattern.compile("SET +\\w+ +('([^'\\r\\n]|''|\\r?\\n)*'|ON|OFF|\\d+|\\w+)[\\n\\r]?\0",
                    Pattern.CASE_INSENSITIVE),
            Pattern.compile("SPOOL ([^\\r\\n]*)?[\\n\\r]?\0", Pattern.CASE_INSENSITIVE)
    };

    /**
     * 是否保留格式
     */
    private boolean preserveFormat;
    /**
     * 默认分隔符名称
     */
    private static final String DELIMITER_NAME = "delimiter";
    /**
     * 默认分隔符
     */
    private String delimiter = ";";
    /**
     * 当前是否多行注释的标识量
     */
    private boolean mlComment = false;
    /**
     * 是否处于字符串或表达式中的标识量
     */
    private char inString = '\0';
    /**
     * 是否保留单行注释
     */
    @Getter
    private boolean preserveSingleComments;
    /**
     * 是否保留多行注释
     */
    @Getter
    private boolean preserveMultiComments;
    /**
     * 是否处于一个正常的sql语句中
     */
    private boolean inNormalSql = false;
    /**
     * oracle 模式下字符串可能存在 Q 转义，需要对这种情况做适配
     */
    private char escapeString = '\0';

    public SqlSplitterForThirdParty(boolean preserveFormat,
            boolean preserveSingleComments, boolean preserveMultiComments) {
        this.preserveFormat = preserveFormat;
        this.preserveSingleComments = preserveSingleComments;
        this.preserveMultiComments = preserveMultiComments;
    }

    public List<String> split(File input) throws Exception {
        List<String> pls = new ArrayList<>();
        StringBuilder builderForPl = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(input))) {
            String line;
            while ((line = reader.readLine()) != null) {
                addLineOracleForPl(pls, builderForPl, line);
            }
        }
        if (builderForPl.length() > 0) {
            pls.add(builderForPl.toString());
        }

        this.preserveFormat = true;
        this.preserveSingleComments = true;
        this.preserveMultiComments = true;

        return pls.stream().map(String::trim).flatMap(sql -> {
            if (sql.endsWith("/")) {
                return Stream.of(sql);
            }
            List<String> sqls = new ArrayList<>();
            StringBuilder builderForSql = new StringBuilder();
            for (String line : sql.split("\n")) {
                addLineOracleForSql(sqls, builderForSql, line);
            }
            if (builderForSql.length() > 0) {
                sqls.add(builderForSql.toString());
            }
            return sqls.stream();
        }).collect(Collectors.toList());
    }

    private void addLineOracleForSql(List<String> sqls, StringBuilder builder, String line) {
        int pos, out;
        boolean needSpace = false;
        // 标识量，用于标识当前是否处于HINT，CONDITIONAL中
        SSC ssComment = SSC.NONE;

        boolean isSameLine = false;
        int lineLength = line.length();
        char[] lines = Arrays.copyOf(line.toCharArray(), lineLength + 1);
        if (lines[0] == 0 && builder.length() == 0) {
            return;
        }

        lines[lineLength] = 0;
        for (pos = out = 0; pos < lineLength; pos++) {
            char inChar = lines[pos];
            // 去掉每一行SQL语句最开始的空格
            if (inChar == ' ' && out == 0 && builder.length() == 0 && !preserveFormat) {
                continue;
            }
            String delimiterHeader = new String(lines, 0, out);
            if (preserveFormat) {
                delimiterHeader = delimiterHeader.replaceAll("^ +", "");
            }
            if ((DELIMITER_NAME + " ").equalsIgnoreCase(delimiterHeader)) {
                // 检测到"delimiter "字符串，且不在多行注释以及多行字符串中，说明有设定分隔符的语句
                StringBuilder newDelimiter = new StringBuilder();
                for (; pos < lineLength; pos++) {
                    char tempChar = lines[pos];
                    if (tempChar != ' ') {
                        newDelimiter.append(tempChar);
                    } else if (newDelimiter.length() != 0) {
                        break;
                    }
                }
                out = 0;
                this.delimiter = newDelimiter.toString();
                continue;
            }
            if (!mlComment && inString == '\0' && ssComment != SSC.HINT && isPrefix(lines, pos, delimiter)) {
                // 不是多行注释，未在字符串中，不是hint且以delimiter开头，通常是扫描到了sql的末尾
                pos += delimiter.length();
                if (out != 0) {
                    builder.append(lines, 0, out);
                    out = 0;
                }
                // builder.append(";").append('\n');
                sqls.add(builder.toString());
                pos--;
                builder.setLength(0);
                isSameLine = true;
                inNormalSql = false;
            } else if (!mlComment && (inString == '\0' && (inChar == '-' && lines[pos + 1] == '-'
                    && (lines[pos + 2] != '+' || (lines[pos + 2] == ' '
                            || lines[pos + 2] == '\0'))))) {
                // 处于单行注释中，注意规避单行HINT
                builder.append(lines, 0, out);
                out = 0;
                if (preserveSingleComments) {
                    // 如果保留单行注释则需要将注释完整地拷贝到缓冲中不能丢弃
                    for (; pos < lineLength; pos++) {
                        lines[out++] = lines[pos];
                    }
                    if (isOnlyWhiteSpace(builder)) {
                        // 缓冲中全部是空格，或者缓冲为空说明注释要么处于第一行要么处于个已经完结的sql语句之后
                        if (sqls.size() != 0) {
                            // 说明注释处于一个已经完结的sql之后，且该sql已经被加入到sql集合中，此处的注释需要追加到最后一句sql中
                            builder.append(lines, 0, out);
                            int lastIndex = sqls.size() - 1;
                            String lastSql = sqls.get(lastIndex);
                            if (!isSameLine) {
                                lastSql += '\n';
                            }
                            lastSql += builder.append("\n");
                            sqls.set(lastIndex, lastSql);
                            builder.setLength(0);
                        } else {
                            lines[out++] = '\n';
                            builder.append(lines, 0, out - 1);
                        }
                    } else {
                        lines[out++] = '\n';
                        builder.append(lines, 0, out - 1);
                    }
                    out = 0;
                }
                break;
            } else if (inString == '\0' && (inChar == '/' && lines[pos + 1] == '*') && lines[pos + 2] != '+'
                    && ssComment != SSC.HINT) {
                // 处于多行注释中，注意规避了HINT和CONDITIONAL，Oracle模式下没有conditional
                if (preserveMultiComments) {
                    lines[out++] = '/';
                    lines[out++] = '*';
                }
                pos++;
                mlComment = true;
            } else if (mlComment && ssComment == SSC.NONE && inChar == '*' && lines[pos + 1] == '/') {
                // 多行注释结束
                pos++;
                mlComment = false;
                builder.append(lines, 0, out);
                out = 0;
                if (preserveMultiComments) {
                    lines[out++] = '*';
                    lines[out++] = '/';
                    builder.append(lines, 0, out);
                    out = 0;
                    if (sqls.size() != 0 && !inNormalSql) {
                        int lastIndex = sqls.size() - 1;
                        String lastSql = sqls.get(lastIndex) + builder;
                        sqls.set(lastIndex, lastSql);
                        builder.setLength(0);
                    }
                }
                needSpace = true;
            } else {
                if (inString == '\0' && inChar == '/' && lines[pos + 1] == '*') {
                    if (lines[pos + 2] == '+') {
                        // 处于HINT中
                        ssComment = SSC.HINT;
                    }
                } else if (inString == '\0' && ssComment != SSC.NONE && inChar == '*' && lines[pos + 1] == '/') {
                    // HINT或CONDITIONAL结束
                    ssComment = SSC.NONE;
                } else if (inString == '\0' && inChar == '-' && lines[pos + 1] == '-' && lines[pos + 2] == '+') {
                    // 在Oracle模式下Hint有单行Hint和多行Hint之分，这里处理Oracle模式下的单行Hint
                    ssComment = SSC.HINT;
                }
                if (inChar == inString) {
                    // 字符指针出字符串或表达式
                    if (escapeString == '\0') {
                        inString = '\0';
                    } else if (pos >= 1 && matchQEscape(lines[pos - 1])) {
                        inString = '\0';
                        escapeString = '\0';
                    }
                } else if (!mlComment && inString == '\0' && ssComment != SSC.HINT
                        && (inChar == '\'' || inChar == '"' || inChar == '`')) {
                    // 字符指针进入字符串或者表达式
                    inString = inChar;
                    if (pos >= 1 && (lines[pos - 1] == 'q' || lines[pos - 1] == 'Q')) {
                        // oracle 特有语法，Q 转义
                        escapeString = lines[pos + 1];
                    }
                }
                if (!mlComment) {
                    if (needSpace && inChar == ' ') {
                        lines[out++] = ' ';
                    }
                    needSpace = false;
                    // 正常的SQL语句，将其放入line缓冲当中，在合适的实际flush如buffer缓存
                    lines[out++] = inChar;
                    if (inChar != ' ') {
                        inNormalSql = true;
                    }
                } else if (preserveMultiComments) {
                    // 保留多行注释
                    lines[out++] = inChar;
                }
            }
        }
        // 拦截性的处理，如果out指针没有为0，说明lines中还有内容没有被刷入到buffer，在这里进行flush
        if (out != 0 || builder.length() != 0) {
            lines[out++] = '\n';
            builder.append(lines, 0, out);
        }
    }

    private void addLineOracleForPl(List<String> sqls, StringBuilder builder, String line) {

        int pos, out;
        boolean needSpace = false;
        // 标识量，用于标识当前是否处于HINT，CONDITIONAL中
        SSC ssComment = SSC.NONE;

        boolean isSameLine = false;
        int lineLength = line.length();
        char[] lines = Arrays.copyOf(line.toCharArray(), lineLength + 1);
        if (lines[0] == 0 && builder.length() == 0) {
            return;
        }

        lines[lineLength] = 0;
        for (pos = out = 0; pos < lineLength; pos++) {
            char inChar = lines[pos];
            // 去掉每一行SQL语句最开始的空格
            if (inChar == ' ' && out == 0 && builder.length() == 0 && !preserveFormat) {
                continue;
            }
            int len = isComment(lines, pos);
            if (!mlComment && inString == '\0' && ssComment != SSC.HINT && len != -1) {
                // 不是多行注释，未在字符串中，不是hint且以delimiter开头，通常是扫描到了sql的末尾
                pos += len;
                if (out != 0) {
                    builder.append(lines, 0, out);
                    out = 0;
                }
                // builder.append(";").append('\n');
                sqls.add(builder.toString());
                pos--;
                builder.setLength(0);
                isSameLine = true;
                inNormalSql = false;
            } else if (!mlComment && inString == '\0' && (inChar == '-' && lines[pos + 1] == '-'
                    && (lines[pos + 2] != '+' || (lines[pos + 2] == ' ' || lines[pos + 2] == '\0')))) {
                // 处于单行注释中，注意规避单行HINT
                builder.append(lines, 0, out);
                out = 0;
                if (preserveSingleComments) {
                    // 如果保留单行注释则需要将注释完整地拷贝到缓冲中不能丢弃
                    for (; pos < lineLength; pos++) {
                        lines[out++] = lines[pos];
                    }
                    if (isOnlyWhiteSpace(builder)) {
                        // 缓冲中全部是空格，或者缓冲为空说明注释要么处于第一行要么处于个已经完结的sql语句之后
                        if (sqls.size() != 0) {
                            // 说明注释处于一个已经完结的sql之后，且该sql已经被加入到sql集合中，此处的注释需要追加到最后一句sql中
                            builder.append(lines, 0, out);
                            int lastIndex = sqls.size() - 1;
                            String lastSql = sqls.get(lastIndex);
                            if (!isSameLine) {
                                lastSql += '\n';
                            }
                            lastSql += builder.append("\n");
                            sqls.set(lastIndex, lastSql);
                            builder.setLength(0);
                        } else {
                            lines[out++] = '\n';
                            builder.append(lines, 0, out - 1);
                        }
                    } else {
                        lines[out++] = '\n';
                        builder.append(lines, 0, out - 1);
                    }
                    out = 0;
                }
                break;
            } else if (inString == '\0' && (inChar == '/' && lines[pos + 1] == '*') && lines[pos + 2] != '+'
                    && ssComment != SSC.HINT) {
                // 处于多行注释中，注意规避了HINT和CONDITIONAL，Oracle模式下没有conditional
                if (preserveMultiComments) {
                    lines[out++] = '/';
                    lines[out++] = '*';
                }
                pos++;
                mlComment = true;
            } else if (mlComment && ssComment == SSC.NONE && inChar == '*' && lines[pos + 1] == '/') {
                // 多行注释结束
                pos++;
                mlComment = false;
                builder.append(lines, 0, out);
                out = 0;
                if (preserveMultiComments) {
                    lines[out++] = '*';
                    lines[out++] = '/';
                    builder.append(lines, 0, out);
                    out = 0;
                    if (sqls.size() != 0 && !inNormalSql) {
                        int lastIndex = sqls.size() - 1;
                        String lastSql = sqls.get(lastIndex) + builder;
                        sqls.set(lastIndex, lastSql);
                        builder.setLength(0);
                    }
                }
                needSpace = true;
            } else {
                if (inString == '\0' && inChar == '/' && lines[pos + 1] == '*') {
                    if (lines[pos + 2] == '+') {
                        // 处于HINT中
                        ssComment = SSC.HINT;
                    }
                } else if (inString == '\0' && ssComment != SSC.NONE && inChar == '*' && lines[pos + 1] == '/') {
                    // HINT或CONDITIONAL结束
                    ssComment = SSC.NONE;
                } else if (inString == '\0' && inChar == '-' && lines[pos + 1] == '-' && lines[pos + 2] == '+') {
                    // 在Oracle模式下Hint有单行Hint和多行Hint之分，这里处理Oracle模式下的单行Hint
                    ssComment = SSC.HINT;
                }
                if (inChar == inString) {
                    // 字符指针出字符串或表达式
                    if (escapeString == '\0') {
                        inString = '\0';
                    } else if (pos >= 1 && matchQEscape(lines[pos - 1])) {
                        inString = '\0';
                        escapeString = '\0';
                    }
                } else if (!mlComment && inString == '\0' && ssComment != SSC.HINT
                        && (inChar == '\'' || inChar == '"' || inChar == '`')) {
                    // 字符指针进入字符串或者表达式
                    inString = inChar;
                    if (pos >= 1 && (lines[pos - 1] == 'q' || lines[pos - 1] == 'Q')) {
                        // oracle 特有语法，Q 转义
                        escapeString = lines[pos + 1];
                    }
                }
                if (!mlComment) {
                    if (needSpace && inChar == ' ') {
                        lines[out++] = ' ';
                    }
                    needSpace = false;
                    // 正常的SQL语句，将其放入line缓冲当中，在合适的实际flush如buffer缓存
                    lines[out++] = inChar;
                    if (inChar != ' ') {
                        inNormalSql = true;
                    }
                } else if (preserveMultiComments) {
                    // 保留多行注释
                    lines[out++] = inChar;
                }
            }
        }
        // 拦截性的处理，如果out指针没有为0，说明lines中还有内容没有被刷入到buffer，在这里进行flush
        if (out != 0 || builder.length() != 0) {
            lines[out++] = '\n';
            builder.append(lines, 0, out);
        }
    }

    private boolean isOnlyWhiteSpace(StringBuilder builder) {
        if (builder == null) {
            return false;
        }
        int length = builder.length();
        for (int i = 0; i < length; i++) {
            if (builder.charAt(i) != ' ') {
                return false;
            }
        }
        return true;
    }

    private int isComment(char[] line, int pos) {
        String tmpStr = new String(line, pos, line.length - pos);
        for (Pattern pattern : PATTERNS) {
            boolean res = pattern.matcher(tmpStr).matches();
            if (!res) {
                continue;
            }
            if (pos >= 1) {
                if (line[pos - 1] == ' ') {
                    return line.length - pos;
                }
                return -1;
            } else {
                return line.length - pos;
            }
        }
        return -1;
    }

    /**
     * 当前SQL是否是以分隔符开头
     */
    private boolean isPrefix(char[] line, int pos, String delim) {
        boolean res = new String(line, pos, line.length - pos).startsWith(delim);
        if (!res || !"/".equals(delim) || line.length <= 1) {
            return res;
        }
        // 匹配到分隔符，分隔符为正斜杠且当前行的大小大于 1，需要注意规避多行注释
        if (pos == 0) {
            return !(line[pos + 1] == '*');
        } else if (line.length - 1 == pos) {
            return !(line[pos - 1] == '*');
        }
        return !(line[pos + 1] == '*' || line[pos - 1] == '*');
    }

    private boolean matchQEscape(char escapeChar) {
        if (this.escapeString == '\0') {
            return false;
        }
        switch (this.escapeString) {
            case '<':
                return escapeChar == '>';
            case '{':
                return escapeChar == '}';
            case '[':
                return escapeChar == ']';
            case '(':
                return escapeChar == ')';
            default:
                return this.escapeString == escapeChar;
        }
    }

    private enum SSC {
        /**
         * 不处于HINT或CONDITIONAL中
         */
        NONE(0),
        /**
         * 当前SQL字符指针处于CONDITIONAL中
         */
        CONDITIONAL(1),
        /**
         * 当前处于HINT中
         */
        HINT(2);

        private final int value;

        SSC(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

}
