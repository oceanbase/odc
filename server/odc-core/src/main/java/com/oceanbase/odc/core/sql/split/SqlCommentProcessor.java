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
package com.oceanbase.odc.core.sql.split;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import org.springframework.util.CollectionUtils;

import com.oceanbase.odc.common.lang.Holder;
import com.oceanbase.odc.core.shared.constant.DialectType;

import lombok.Getter;

/**
 * 该类用于SQL预处理，去除注释以及进行SQL断句
 *
 * @author yh263208
 * @reference(Oracle) https://docs.oracle.com/cd/B19306_01/server.102/b14200/sql_elements006.htm#i31713
 * @reference(Mysql5.6) https://dev.mysql.com/doc/refman/5.6/en/comments.html
 * @notice 该类为有状态的类，状态是session级别的，因此不建议使用spring自动装配。@Scope也无法解决状态冲突的问题，因此建议直接使用new的方式获取对象
 * @date 2020-07-02
 * @since 公有云2.0.2
 */
public class SqlCommentProcessor {

    private static final String DELIMITER_NAME = "delimiter";
    /**
     * 是否保留格式
     */
    private boolean preserveFormat = false;
    private String delimiter = ";";
    private boolean mlComment = false;
    private char inString = '\0';
    private DialectType dialectType;
    private char escapeString = '\0';
    private boolean inNormalSql = false;
    /**
     * 是否保留单行注释
     */
    @Getter
    private boolean preserveSingleComments = false;
    /**
     * 是否保留多行注释
     */
    @Getter
    private boolean preserveMultiComments = false;

    public SqlCommentProcessor(boolean preserveFormat, String delimiter) {
        this.delimiter = delimiter;
        this.preserveFormat = preserveFormat;
    }

    public SqlCommentProcessor(DialectType dialectType,
            boolean preserveSingleComments,
            boolean preserveMultiComments) {
        this.preserveFormat = true;
        this.dialectType = dialectType;
        this.preserveSingleComments = preserveSingleComments;
        this.preserveMultiComments = preserveMultiComments;
    }

    public SqlCommentProcessor(DialectType dialectType,
            boolean preserveFormat,
            boolean preserveSingleComments,
            boolean preserveMultiComments) {
        this.preserveFormat = preserveFormat;
        this.dialectType = dialectType;
        this.preserveSingleComments = preserveSingleComments;
        this.preserveMultiComments = preserveMultiComments;
    }

    public SqlCommentProcessor(DialectType dialectType, String delimiter) {
        this.preserveFormat = true;
        this.dialectType = dialectType;
        this.delimiter = delimiter;
    }

    public SqlCommentProcessor() {}

    public static List<String> removeSqlComments(String originalSql,
            String delimiter, DialectType dbMode, boolean preserveFormat) {
        SqlCommentProcessor sqlCommentProcessor = new SqlCommentProcessor(preserveFormat, delimiter);
        List<String> sqls = new ArrayList<>();
        String[] lines = originalSql.split("\n");
        StringBuffer buffer = new StringBuffer();
        //for (String item : lines) {
        //    if (Objects.nonNull(dbMode) && dbMode.isMysql()) {
        //        sqlCommentProcessor.addLineMysql(sqls, buffer, item);
        //    } else {
        //        sqlCommentProcessor.addLineOracle(sqls, buffer, item);
        //    }
        //}
        String bufferStr = buffer.toString();
        if (bufferStr.trim().length() != 0) {
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
            sqls.add(bufferStr);
        }
        return sqls;
    }

    public synchronized List<OffsetString> split(StringBuffer buffer, String sqlScript) {
        try {
            List<String> sqls = new ArrayList<>();
            List<OffsetString> offsetStrings = new ArrayList<>();

            List<OrderChar> orderChars = new ArrayList<>();
            List<List<OrderChar>> lines1 = new ArrayList<>();

            for (int i = 0; i < sqlScript.length(); i++) {
                orderChars.add(new OrderChar(sqlScript.charAt(i), i));
            }
            // split by \n
            orderChars.stream().reduce(new ArrayList<OrderChar>(), (list, orderChar) -> {
                if (orderChar.getCh() == '\n') {
                    lines1.add(list);
                    return new ArrayList<>();
                } else {
                    list.add(orderChar);
                    return list;
                }
            }, (list1, list2) -> {
                list1.addAll(list2);
                return list1;
            });
            Holder<Integer> bufferOrder = new Holder<>(0);
            for (List<OrderChar> item : lines1) {
                if (Objects.nonNull(this.dialectType) && this.dialectType.isMysql()) {
                    addLineMysql(offsetStrings, buffer, bufferOrder, item);
                }
            }
            //String[] lines = sqlScript.split("\n");
            //for (String item : lines) {
            //    if (Objects.nonNull(this.dialectType) && this.dialectType.isOracle()) {
            //        addLineOracle(sqls, buffer, item);
            //    } else {
            //        throw new IllegalArgumentException("dialect type is illegal");
            //    }
            //}
            return offsetStrings;
        } finally {
            mlComment = false;
            inString = '\0';
            inNormalSql = false;
        }
    }

    private synchronized void addLineMysql(List<OffsetString> sqls, StringBuffer buffer, Holder<Integer> bufferOrder, List<OrderChar> line) {
        int pos, out;
        boolean needSpace = false;
        // 标识量，用于标识当前是否处于HINT，CONDITIONAL中
        SSC ssComment = SSC.NONE;
        boolean isSameLine = false;
        int lineLength = line.size();
        OrderChar[] lines = line.toArray(new OrderChar[lineLength + 1]);
        if (lines[0].getCh() == 0 && buffer.length() == 0) {
            return;
        }
        lines[lineLength] = new OrderChar((char) 0, lineLength);
        for (pos = out = 0; pos < lineLength; pos++) {
            char inChar = lines[pos].getCh();
            // 去掉每一行SQL语句最开始的空格
            if (inChar == ' ' && out == 0 && buffer.length() == 0 && !preserveFormat) {
                continue;
            }
            int delimiterBegin = 0;
            if (preserveFormat) {
                for (; delimiterBegin < out
                        && (lines[delimiterBegin].getCh() == ' '
                                || lines[delimiterBegin].getCh() == '\t'); delimiterBegin++) {
                }
            }
            if (equalsIgnoreCase((DELIMITER_NAME + " ").toCharArray(), IntStream.range(0, lines.length)
                    .mapToObj(i -> lines[i].getCh())
                    .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                    .toString()
                    .toCharArray(), delimiterBegin, (out - delimiterBegin))) {
                // 检测到"delimiter "字符串，且不在多行注释以及多行字符串中，说明有设定分隔符的语句
                StringBuilder newDelimiter = new StringBuilder();
                for (; pos < lineLength; pos++) {
                    char tempChar = lines[pos].getCh();
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
            // 扫描到转义字符，可能出现指令
            if ((!mlComment && inChar == '\\')) {
                if ((inChar = lines[++pos].getCh()) == 0) {
                    break;
                }
                if (inString != '\0' || inChar == 'N') {
                    lines[out++].setCh('\\');
                    if (inChar == '`' && inString == inChar) {
                        pos--;
                    } else {
                        lines[out++].setCh(inChar);
                    }
                    continue;
                }
                // 非mysql model或没有检索到正确的命令，直接将转义符号及转义字符放入缓冲
                lines[out++].setCh('\\');
                lines[out++].setCh(inChar);
                continue;
            } else if (!mlComment && inString == '\0' && ssComment != SSC.HINT && isPrefix(IntStream.range(0, lines.length)
                .mapToObj(i -> lines[i].getCh())
                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                .toString()
                .toCharArray(), pos, delimiter)) {
                // 不是多行注释，未在字符串中，不是hint且以delimiter开头，通常是扫描到了sql的末尾
                pos += delimiter.length();
                if (out != 0) {
                    if (buffer.length() == 0) {
                        bufferOrder.setValue(lines[0].getOrder());
                    }
                    buffer.append(IntStream.range(0, lines.length)
                        .mapToObj(i -> lines[i].getCh())
                        .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                        .toString()
                        .toCharArray(), 0, out);
                    out = 0;
                }
                // buffer.append(";").append('\n');
                sqls.add(new OffsetString(bufferOrder.getValue(), buffer.toString()));
                pos--;
                buffer.setLength(0);
                isSameLine = true;
                inNormalSql = false;
            } else if (!mlComment && (inString == '\0' && (inChar == '#' || (inChar == '-' && lines[pos + 1].getCh() == '-'
                    && ((lines[pos + 2].getCh() == ' ' || lines[pos + 2].getCh() == '\0')))))) {
                // 处于单行注释中
                if (buffer.length() == 0) {
                    bufferOrder.setValue(lines[0].getOrder());
                }
                buffer.append(IntStream.range(0, lines.length)
                    .mapToObj(i -> lines[i].getCh())
                    .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                    .toString()
                    .toCharArray(), 0, out);
                out = 0;
                if (preserveSingleComments) {
                    // 如果保留单行注释则需要将注释完整地拷贝到缓冲中不能丢弃
                    for (; pos < lineLength; pos++) {
                        lines[out++] = lines[pos];
                    }
                    if (isOnlyWhiteSpace(buffer)) {
                        // 缓冲中全部是空格，或者缓冲为空说明注释要么处于第一行要么处于个已经完结的sql语句之后
                        if (sqls.size() != 0) {
                            if (buffer.length() == 0) {
                                bufferOrder.setValue(lines[0].getOrder());
                            }
                            // 说明注释处于一个已经完结的sql之后，且该sql已经被加入到sql集合中，此处的注释需要追加到最后一句sql中
                            buffer.append(IntStream.range(0, lines.length)
                                .mapToObj(i -> lines[i].getCh())
                                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                                .toString()
                                .toCharArray(), 0, out);
                            int lastIndex = sqls.size() - 1;
                            String lastSql = sqls.get(lastIndex).getStr();
                            if (!isSameLine) {
                                lastSql += '\n';
                            }
                            lastSql += buffer + "\n";
                            sqls.set(lastIndex, new OffsetString(sqls.get(lastIndex).getOffset(), lastSql));
                            buffer.setLength(0);
                        } else {
                            lines[out++].setCh('\n');
                            if (buffer.length() == 0) {
                                bufferOrder.setValue(lines[0].getOrder());
                            }
                            buffer.append(IntStream.range(0, lines.length)
                                .mapToObj(i -> lines[i].getCh())
                                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                                .toString()
                                .toCharArray(), 0, out - 1);
                        }
                    } else {
                        lines[out++].setCh('\n');
                        if (buffer.length() == 0) {
                            bufferOrder.setValue(lines[0].getOrder());
                        }
                        buffer.append(IntStream.range(0, lines.length)
                            .mapToObj(i -> lines[i].getCh())
                            .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                            .toString()
                            .toCharArray(), 0, out - 1);
                    }
                    out = 0;
                }
                break;
            } else if (inString == '\0' && (inChar == '/' && lines[pos + 1].getCh() == '*')
            // 此处注意，Oracle模式下没有Conditional，故这里要做规避。Mysql模式下的Conditional在Oracle模式在要识别为注释去掉
                    && lines[pos + 2].getCh() != '!'
                    && lines[pos + 2].getCh() != '+' && ssComment != SSC.HINT) {
                // 处于多行注释中，注意规避了HINT和CONDITIONAL，Oracle模式下没有conditional
                if (preserveMultiComments) {
                    lines[out++].setCh('/');
                    lines[out++].setCh('*');
                }
                pos++;
                mlComment = true;
            } else if (mlComment && ssComment == SSC.NONE && inChar == '*' && lines[pos + 1].getCh() == '/') {
                // 多行注释结束
                pos++;
                mlComment = false;
                if (buffer.length() == 0) {
                    bufferOrder.setValue(lines[0].getOrder());
                }
                buffer.append(IntStream.range(0, lines.length)
                    .mapToObj(i -> lines[i].getCh())
                    .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                    .toString()
                    .toCharArray(), 0, out);
                out = 0;
                if (preserveMultiComments) {
                    lines[out++].setCh('*');
                    lines[out++].setCh('/');
                    if (buffer.length() == 0) {
                        bufferOrder.setValue(lines[0].getOrder());
                    }
                    buffer.append(IntStream.range(0, lines.length)
                        .mapToObj(i -> lines[i].getCh())
                        .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                        .toString()
                        .toCharArray(), 0, out);
                    out = 0;
                    if (sqls.size() != 0 && !inNormalSql) {
                        int lastIndex = sqls.size() - 1;
                        String lastSql = sqls.get(lastIndex) + buffer.toString();
                        sqls.set(lastIndex, new OffsetString(sqls.get(lastIndex).getOffset(), lastSql));
                        buffer.setLength(0);
                    }
                }
                needSpace = true;
            } else {
                if (inString == '\0' && inChar == '/' && lines[pos + 1].getCh() == '*') {
                    if (lines[pos + 2].getCh() == '!') {
                        // 处于CONDITIONAL中
                        ssComment = SSC.CONDITIONAL;
                    } else if (lines[pos + 2].getCh() == '+') {
                        // 处于HINT中
                        ssComment = SSC.HINT;
                    }
                } else if (inString == '\0' && ssComment != SSC.NONE && inChar == '*' && lines[pos + 1].getCh() == '/') {
                    // HINT或CONDITIONAL结束
                    ssComment = SSC.NONE;
                }
                if (inChar == inString) {
                    // 字符指针出字符串或表达式
                    inString = '\0';
                } else if (!mlComment && inString == '\0' && ssComment != SSC.HINT
                        && (inChar == '\'' || inChar == '"' || inChar == '`')) {
                    // 字符指针进入字符串或者表达式
                    inString = inChar;
                }
                if (!mlComment) {
                    if (needSpace && inChar == ' ') {
                        lines[out++].setCh(' ');
                    }
                    needSpace = false;
                    // 正常的SQL语句，将其放入line缓冲当中，在合适的实际flush如buffer缓存
                    lines[out++].setCh(inChar);
                    if (inChar != ' ') {
                        inNormalSql = true;
                    }
                } else if (preserveMultiComments) {
                    // 保留多行注释
                    lines[out++].setCh(inChar);
                }
            }
        }
        // 拦截性的处理，如果out指针没有为0，说明lines中还有内容没有被刷入到buffer，在这里进行flush
        if (out != 0 || buffer.length() != 0) {
            lines[out++].setCh('\n');
            if (buffer.length() == 0) {
                bufferOrder.setValue(lines[0].getOrder());
            }
            buffer.append(IntStream.range(0, lines.length)
                .mapToObj(i -> lines[i].getCh())
                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                .toString()
                .toCharArray(), 0, out);
        }
    }

    private boolean isOnlyWhiteSpace(StringBuffer buffer) {
        if (buffer == null) {
            return false;
        }
        int length = buffer.length();
        for (int i = 0; i < length; i++) {
            if (buffer.charAt(i) != ' ') {
                return false;
            }
        }
        return true;
    }

    private synchronized void addLineOracle(List<String> sqls, StringBuffer buffer, String line) {
        int pos, out;
        boolean needSpace = false;
        // 标识量，用于标识当前是否处于HINT，CONDITIONAL中
        SSC ssComment = SSC.NONE;

        boolean isSameLine = false;
        int lineLength = line.length();
        char[] lines = Arrays.copyOf(line.toCharArray(), lineLength + 1);
        if (lines[0] == 0 && buffer.length() == 0) {
            return;
        }

        lines[lineLength] = 0;
        for (pos = out = 0; pos < lineLength; pos++) {
            char inChar = lines[pos];
            // 去掉每一行SQL语句最开始的空格
            if (inChar == ' ' && out == 0 && buffer.length() == 0 && !preserveFormat) {
                continue;
            }
            int delimiterBegin = 0;
            if (preserveFormat) {
                for (; delimiterBegin < out
                        && (lines[delimiterBegin] == ' ' || lines[delimiterBegin] == '\t'); delimiterBegin++) {
                }
            }
            if (equalsIgnoreCase((DELIMITER_NAME + " ").toCharArray(), lines, delimiterBegin, (out - delimiterBegin))) {
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
                    buffer.append(lines, 0, out);
                    out = 0;
                }
                // buffer.append(";").append('\n');
                sqls.add(buffer.toString());
                pos--;
                buffer.setLength(0);
                isSameLine = true;
                inNormalSql = false;
            } else if (!mlComment && (inString == '\0' && (inChar == '-' && lines[pos + 1] == '-'
                    && (lines[pos + 2] != '+' || (lines[pos + 2] == ' '
                            || lines[pos + 2] == '\0'))))) {
                // 处于单行注释中，注意规避单行HINT
                buffer.append(lines, 0, out);
                out = 0;
                if (preserveSingleComments) {
                    // 如果保留单行注释则需要将注释完整地拷贝到缓冲中不能丢弃
                    for (; pos < lineLength; pos++) {
                        lines[out++] = lines[pos];
                    }
                    if (isOnlyWhiteSpace(buffer)) {
                        // 缓冲中全部是空格，或者缓冲为空说明注释要么处于第一行要么处于个已经完结的sql语句之后
                        if (sqls.size() != 0) {
                            // 说明注释处于一个已经完结的sql之后，且该sql已经被加入到sql集合中，此处的注释需要追加到最后一句sql中
                            buffer.append(lines, 0, out);
                            int lastIndex = sqls.size() - 1;
                            String lastSql = sqls.get(lastIndex);
                            if (!isSameLine) {
                                lastSql += '\n';
                            }
                            lastSql += buffer.toString() + "\n";
                            sqls.set(lastIndex, lastSql);
                            buffer.setLength(0);
                        } else {
                            lines[out++] = '\n';
                            buffer.append(lines, 0, out - 1);
                        }
                    } else {
                        lines[out++] = '\n';
                        buffer.append(lines, 0, out - 1);
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
                buffer.append(lines, 0, out);
                out = 0;
                if (preserveMultiComments) {
                    lines[out++] = '*';
                    lines[out++] = '/';
                    buffer.append(lines, 0, out);
                    out = 0;
                    if (sqls.size() != 0 && !inNormalSql) {
                        int lastIndex = sqls.size() - 1;
                        String lastSql = sqls.get(lastIndex) + buffer.toString();
                        sqls.set(lastIndex, lastSql);
                        buffer.setLength(0);
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
        if (out != 0 || buffer.length() != 0) {
            lines[out++] = '\n';
            buffer.append(lines, 0, out);
        }
    }

    private boolean equalsIgnoreCase(char[] src, char[] dest, int begin, int count) {
        if (src == null && dest == null) {
            return true;
        } else if (src != null && dest != null) {
            if (src.length != count) {
                return false;
            }
            for (int i = 0; i < count; i++) {
                char c1 = src[i];
                char c2 = dest[begin + i];
                if (c1 == c2) {
                    continue;
                }
                char u1 = Character.toUpperCase(c1);
                char u2 = Character.toUpperCase(c2);
                if (u1 == u2) {
                    continue;
                }
                if (Character.toLowerCase(u1) == Character.toLowerCase(u2)) {
                    continue;
                }
                return false;
            }
            return true;
        }
        return false;
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

    public String getDelimiter() {
        return delimiter;
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
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
