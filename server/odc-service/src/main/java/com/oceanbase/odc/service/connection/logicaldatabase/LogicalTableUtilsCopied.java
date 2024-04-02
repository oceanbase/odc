/*
 * Copyright (c) 2024 OceanBase.
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

package com.oceanbase.odc.service.connection.logicaldatabase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.util.Strings;

import com.aliyuncs.utils.StringUtils;
import com.oceanbase.odc.common.lang.Pair;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.exception.NotImplementedException;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.service.connection.logicaldatabase.model.DataNode;
import com.oceanbase.odc.service.connection.logicaldatabase.model.LogicalTable;

/**
 * @Author: Lebie
 * @Date: 2024/3/26 14:17
 * @Description: []
 */
public class LogicalTableUtilsCopied {
    private static final String PATTERN_PLACEHOLDER = "[#]";
    private static final String PATTERN_PLACEHOLDER_REGEX = "\\[#\\]";

    private static final String DOT_DELIMITER = ".";
    private static final String COMMA_DELIMITER = ",";
    private static final Pattern DIGIT_PATTERN = Pattern.compile("\\d+");

    public static List<LogicalTable> generatePatternExpressions(@NotEmpty List<DataNode> dataNodes,
            @NotEmpty List<String> allDatabaseNames) {
        // 找出所有逻辑表，并推导出 逻辑库表名的 databaseNamePattern 和 tableNamePattern
        List<LogicalTable> logicalTables = identifyLogicalTables(dataNodes);

        // 替换掉 pattern 中的 [#] 为具体的数字范围
        logicalTables.stream().forEach(table -> {
            String databaseNamePattern = table.getDatabaseNamePattern();
            String tableNamePattern = table.getTableNamePattern();
            // 既分库，又分表
            if (databaseNamePattern.contains(PATTERN_PLACEHOLDER) && tableNamePattern.contains(PATTERN_PLACEHOLDER)) {
                List<String> range = replacePlaceholdersWithRanges(table);
                table.setFullNameExpression(String.join(COMMA_DELIMITER, range));
            } else if (databaseNamePattern.contains(PATTERN_PLACEHOLDER)) {
                // 只分库，不分表
                List<String> range = replacePlaceholdersWithRanges(databaseNamePattern,
                        table.getActualDataNodes().stream()
                                .map(node -> node.getSchemaName())
                                .collect(Collectors.toList()))
                        .stream().map(r -> r + DOT_DELIMITER + tableNamePattern).collect(Collectors.toList());
                table.setFullNameExpression(String.join(COMMA_DELIMITER, range));
            } else if (tableNamePattern.contains(PATTERN_PLACEHOLDER)) {
                // 只分表，不分库
                List<String> range = replacePlaceholdersWithRanges(tableNamePattern,
                        table.getActualDataNodes().stream()
                                .map(node -> node.getTableName())
                                .collect(Collectors.toList()))
                        .stream().map(r -> databaseNamePattern + DOT_DELIMITER + r).collect(Collectors.toList());
                table.setFullNameExpression(String.join(COMMA_DELIMITER, range));
            } else {
                throw new UnexpectedException(
                        String.format("Unexpected pattern: %s.%s", databaseNamePattern, tableNamePattern));
            }
        });
        return logicalTables;
    }

    private static List<String> replacePlaceholdersWithRanges(LogicalTable logicalTable) {
        String databaseNamePattern = logicalTable.getDatabaseNamePattern();
        String tableNamePattern = logicalTable.getTableNamePattern();
        List<String> databaseNames = logicalTable.getActualDataNodes().stream()
                .map(DataNode::getSchemaName)
                .collect(Collectors.toList());
        List<String> tableNames = logicalTable.getActualDataNodes().stream()
                .map(DataNode::getTableName)
                .collect(Collectors.toList());

        int databaseNamePatternPlaceholderCount = databaseNamePattern.split(PATTERN_PLACEHOLDER_REGEX, 10).length - 1;
        int tableNamePatternPlaceholderCount = tableNamePattern.split(PATTERN_PLACEHOLDER_REGEX, 10).length - 1;

        // 如果分库和分表的级联数都大于等于 2，则直接按照分库来分组，对每个分组生成一个表达式
        if (databaseNamePatternPlaceholderCount >= 2 && tableNamePatternPlaceholderCount >= 2) {
        }

        // 如果每个分库的分表是一致的
        if (hasSameTableNames(logicalTable)) {
            List<String> tableNameExpression = replacePlaceholdersWithRanges(tableNamePattern, tableNames).stream()
                    .map(r -> r.replaceAll("([^\\[\\]]*)(\\[[^\\[\\]]*\\])$", "$1[[$2]]")).collect(Collectors.toList());
            List<String> databaseNameExpression = replacePlaceholdersWithRanges(databaseNamePattern, databaseNames).stream()
                    .map(r -> r.replaceAll("([^\\[\\]]*)(\\[[^\\[\\]]*\\])$", "$1[[$2]]")).collect(Collectors.toList());
            List<String> resultSegs = new ArrayList<>();
            for (String databaseName : databaseNameExpression) {
                for (String tableName : tableNameExpression) {
                    resultSegs.add(databaseName + DOT_DELIMITER + tableName);
                }
            }
            return resultSegs;
        }

        return null;

    }

    private static boolean hasSameTableNames(LogicalTable logicalTable) {
        Map<String, List<String>> schemaName2TableNames = logicalTable.getActualDataNodes().stream()
                .collect(Collectors.groupingBy(DataNode::getSchemaName, TreeMap::new,
                        Collectors.mapping(DataNode::getTableName, Collectors.toList())));
        // 使用SortedSet保证列表中的元素顺序
        Set<SortedSet<String>> tableSets = new HashSet<>();
        for (List<String> tables : schemaName2TableNames.values()) {
            SortedSet<String> sortedTables = new TreeSet<>(tables);
            tableSets.add(sortedTables);
        }
        return tableSets.size() == 1;
    }


    // 我现在在写一个 JAVA 函数 String replacePlaceholdersWithRanges(String pattern, List<String> names)，pattern
    // 是一个模式，names 的所有元素都满足这个模式。
    // 这个函数的作用是，根据某些规则，将模式里的 [#] 替换成具体的数字范围，并返回完整的字符串。比如 pattern = "table_[#]"，names = ["table_1",
    // "table_2", "table_3"]，那么返回 "table_[1-3]"。

    // 你的理解不对，这里并不是简单的替换为最小值和最大值。他需要满足如下规则：1. 如果是连续的数字，则是一个范围，比如 [1-3]；2.
    // 如果不是连续的数字，但这些数字是等差数列，则需要取最小值和最大值以及步长最为替换，比如 table_1, table_3, table_5, 可以表示为 table_[1-5:2],
    // 代表最小值是 1， 最大值是 5， 步长是 2
    // 还有一些额外的条件，如果只有两个数字，则也要表示成步长的形式。如果有两个以上的数字，且不是等差数列，则需要将 names 以 逗号 join 起来，组成字符串返回。
    // 注意，你应该从 names 中提取数字，而是应该只关注 pattern 中的 [#] 在 names 各元素中的位置的数字
    // pattern 中的 [#] 可能出现多次，可能出现在任何位置
    // 请不要假定所有的 [#] 都应被统一目标替换，每个 [#] 占位符都是独立的一段。但是，他们之间也是存在关联的，如果有多个 [#]
    // 的存在，我不觉得他们应该被单独处理，而是结合起来一起看。为了简化问题，我们可以假设，一个 pattern 中最多包含两个 [#]。
    // 比如，pattern = "table_[#]_[#]", names 为 table_2023_01, table_2023_02, table_2024_01, table_2024_02
    // 应该被表示成 table_[2023-2024]_[01-02]，意思是 两个 # 之间的数字是笛卡尔积，而不是独立的两个范围
    // 请你不要忘记上面对话中的假设，也就是只存在一个 [#] 的那些规则，同样要适用，请写出完整的函数
    // 你还需要考虑一条规则，在有两个 [#] 占位符的场景，两组数字间如果满足笛卡尔积的情况，也需要根据单个 [#] 占位符的规则，将每个 [#] 占位符的数字范围合并起来
    // 注意，在有两个 [#] 的场景，你需要主动去检测两组数字是否满足笛卡尔积的情况，如果满足，才能表示成笛卡尔积的形式；如果不满足笛卡尔积的情况，你只需要按照第一个 [#]
    // 的数字来分组，在每个分组里，再对第二个 [#] 进行处理，这里的处理方式就跟只有一个 [#] 占位符的原则一样
    private static List<String> replacePlaceholdersWithRanges(String pattern, List<String> names) {
        String[] parts = pattern.split(PATTERN_PLACEHOLDER_REGEX, -1);
        int len = parts.length;
        Verify.notLessThan(len, 2, "Pattern should contain at least one placeholder: " + pattern);
        Verify.notGreaterThan(len, 3, "Pattern should contain at most two placeholders: " + pattern);

        Pattern patternRegex = Pattern.compile(pattern.replaceAll(PATTERN_PLACEHOLDER_REGEX, "(\\\\d+)"));
        // contains only one placeholder
        if (len == 2) {
            String range = generateRangeOrList(names.stream().map(name -> {
                Matcher matcher = patternRegex.matcher(name);
                if (matcher.matches()) {
                    return matcher.group(1);
                } else {
                    throw new IllegalArgumentException("表名 " + name + " 不符合模式 " + pattern);
                }
            }).collect(Collectors.toSet()));
            return Arrays.asList(parts[0] + range + parts[1]);
        } else if (len == 3) {
            // contains two placeholders
            List<Pair<String, String>> numberPairs = names.stream().map(name -> {
                Matcher matcher = patternRegex.matcher(name);
                if (matcher.matches()) {
                    return new Pair<>(matcher.group(1), matcher.group(2));
                } else {
                    throw new IllegalArgumentException("表名 " + name + " 不符合模式 " + pattern);
                }
            }).collect(Collectors.toList()).stream().sorted(Comparator.comparing(pair -> pair.left))
                    .collect(Collectors.toList());

            Pair<String, String> rangePair = generateRangeOrList(numberPairs);
            // 笛卡尔积的情况
            if (rangePair != null) {
                return Arrays.asList(parts[0] + rangePair.left + parts[1] + rangePair.right + parts[2]);
            } else {
                List<String> result = new ArrayList<>();
                // 不是笛卡尔积的情况，需要按照第一个 [#] 的数字来分组，在每个分组里，再对第二个 [#] 单独处理，然后将所有结果拼接起来
                numberPairs.stream().collect(
                        Collectors.groupingBy(pair -> pair.left, TreeMap::new, Collectors.toCollection(ArrayList::new)))
                        .entrySet().forEach(entry -> {
                            String rightRange = generateRangeOrList(
                                    entry.getValue().stream().map(pair -> pair.right).collect(Collectors.toSet()));
                            result.add(parts[0] + entry.getKey() + parts[1] + rightRange + parts[2]);
                        });
                return result;
            }
        } else {
            throw new UnexpectedException("Unexpected pattern: " + pattern);
        }
    }


    private static Pair<String, String> generateRangeOrList(List<Pair<String, String>> numberPairs) {
        // 如果是笛卡尔积，则结果可以表示为 Pair<String, String> 的形式
        // 并且 left 和 right 的表达式都遵循单个表达式的原则
        if (isCartesianProduct(numberPairs)) {
            Set<String> leftSet =
                    numberPairs.stream().map(pair -> pair.left).collect(Collectors.toCollection(TreeSet::new));
            Set<String> rightSet =
                    numberPairs.stream().map(pair -> pair.right).collect(Collectors.toCollection(TreeSet::new));
            return new Pair<>(generateRangeOrList(leftSet), generateRangeOrList(rightSet));
        }
        return null;
    }

    public static boolean isCartesianProduct(List<Pair<String, String>> pairs) {
        Map<String, Set<String>> map = new HashMap<>();

        // 将left值相同的right值收集到一个Set中
        for (Pair<String, String> pair : pairs) {
            String left = pair.left;
            String right = pair.right;

            // 使用HashSet来去重，并保存right值
            if (!map.containsKey(left)) {
                map.put(left, new HashSet<>());
            }
            map.get(left).add(right);
        }

        // 所有left键应该映射到相同尺寸的right值集合
        Set<String> referenceSet = null; // 用第一个Set作为参照
        for (Set<String> rightSet : map.values()) {
            // 设置参照Set
            if (referenceSet == null) {
                referenceSet = rightSet;
                continue;
            }
            // 如果有Set与参照不同，那么它不是笛卡尔积
            if (!CollectionUtils.isEqualCollection(referenceSet, rightSet)) {
                return false;
            }
        }
        return true; // 所有Set都相同，确认是笛卡尔积
    }

    private static String generateRangeOrList(Set<String> numberStrings) {
        if (numberStrings.size() == 1) {
            return numberStrings.iterator().next();
        }

        TreeSet<String> sortedNumberStrings = new TreeSet<>(numberStrings);
        // 检查是否为连续序列
        boolean isConsecutive = true;
        String first = sortedNumberStrings.first();
        int step = Integer.MIN_VALUE;
        String prevNumber = first;

        for (String currentNumber : sortedNumberStrings.tailSet(first, false)) {
            int currentStep = Integer.parseInt(currentNumber) - Integer.parseInt(prevNumber);
            if (step == Integer.MIN_VALUE) {
                step = currentStep; // 设置初始步长
            } else if (currentStep != step) {
                isConsecutive = false; // 发现非连续的步长
                break;
            }
            prevNumber = currentNumber; // 更新前一个数字为当前数字
        }

        if (isConsecutive) {
            // 有序且连续的区间
            String start = sortedNumberStrings.first();
            String end = sortedNumberStrings.last();
            if (start.equals(end)) {
                return String.format("[%s]", start);
            } else if (step == 1) {
                return String.format("[%s-%s]", start, end);
            } else {
                // 考虑步长的情况
                return String.format("[%s-%s:%d]", start, end, step);
            }
        } else {
            // 不连续的数字，返回空字符串，这种情况，调用方需要自行处理，比如拼接全部表名
            return String.format("[%s]", String.join(COMMA_DELIMITER, numberStrings));
        }
    }

    private static List<LogicalTable> identifyLogicalTables(@Valid @NotEmpty List<DataNode> dataNodes) {
        // 先将表名分解为由数字序列和非数字序列组成的部分，构建基本模式
        Map<String, List<DataNode>> basePattern2Tables = dataNodes.stream().collect(
                Collectors.groupingBy(dataNode -> dataNode.getTableName().replaceAll(DIGIT_PATTERN.pattern(), "[#]")));
        basePattern2Tables.entrySet().removeIf(entry -> entry.getValue().size() == 1);

        // 最终确定的模式到表的映射
        Map<String, List<DataNode>> finalPatterns = new LinkedHashMap<>();

        basePattern2Tables.forEach((basePattern, nodes) -> {
            // 检查哪些位置的[#]应该被保留，哪些应该替换成实际的数字
            String pattern =
                    getConsistentNumberPattern(nodes.stream().map(DataNode::getTableName).collect(Collectors.toList()));
            finalPatterns.put(pattern, nodes);
        });

        List<LogicalTable> logicalTables = finalPatterns.entrySet().stream().map(entry -> {
            LogicalTable logicalTable = new LogicalTable();
            logicalTable.setTableNamePattern(entry.getKey());
            logicalTable.setActualDataNodes(entry.getValue());
            logicalTable.setDatabaseNamePattern(getConsistentNumberPattern(logicalTable.getActualDataNodes().stream()
                    .map(node -> node.getSchemaName())
                    .collect(Collectors.toList())));
            return logicalTable;
        }).collect(Collectors.toList());
        return logicalTables;
    }

    private static String getConsistentNumberPattern(List<String> names) {
        // 转换为有数字组成的列表
        List<List<String>> nameSections = names.stream().map(name -> {
            Matcher matcher = DIGIT_PATTERN.matcher(name);
            List<String> sections = new ArrayList<>();
            int lastStartIndex = 0;
            while (matcher.find()) {
                sections.add(name.substring(lastStartIndex, matcher.start()));
                sections.add(name.substring(matcher.start(), matcher.end()));
                lastStartIndex = matcher.end();
            }
            if (lastStartIndex < name.length()) {
                sections.add(name.substring(lastStartIndex));
            }
            return sections;
        }).collect(Collectors.toList());

        // 确定每个数字序列是稳定的还是多变的
        List<Boolean> isVariable = new ArrayList<>();
        int sectionCount = nameSections.get(0).size();

        for (int idx = 1; idx < sectionCount; idx += 2) {
            String sampleNumber = nameSections.get(0).get(idx);
            int finalIdx = idx;
            isVariable.add(!nameSections.stream().allMatch(section -> section.get(finalIdx).equals(sampleNumber)));
        }

        // 根据稳定的序列还原出最终的模式
        StringBuilder patternBuilder = new StringBuilder(nameSections.get(0).get(0)); // 开始先添加第一部分的非数字序列

        for (int idx = 1, varIdx = 0; idx < sectionCount; idx += 2, varIdx++) {
            if (isVariable.get(varIdx)) {
                patternBuilder.append(PATTERN_PLACEHOLDER);
            } else {
                patternBuilder.append(nameSections.get(0).get(idx));
            }
            if (idx + 1 < sectionCount) {
                patternBuilder.append(nameSections.get(0).get(idx + 1));
            }
        }
        return patternBuilder.toString();
    }
}
