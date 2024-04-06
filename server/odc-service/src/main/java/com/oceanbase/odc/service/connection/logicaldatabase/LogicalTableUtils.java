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
import java.util.Collection;
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

import com.oceanbase.odc.common.lang.Pair;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.service.connection.logicaldatabase.model.DataNode;
import com.oceanbase.odc.service.connection.logicaldatabase.model.LogicalTable;

/**
 * @Author: Lebie
 * @Date: 2024/3/26 14:17
 * @Description: []
 */
public class LogicalTableUtils {
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

        // 如果每个分库的分表是完全一致的
        if (hasSameTableNames(logicalTable)) {
            List<String> tableNameExpression = replacePlaceholdersWithRanges(tableNamePattern, tableNames).stream()
                    .map(r -> replaceRangeSign(r)).collect(Collectors.toList());
            List<String> databaseNameExpression = replacePlaceholdersWithRanges(databaseNamePattern, databaseNames);
            List<String> resultSegs = new ArrayList<>();
            for (String databaseName : databaseNameExpression) {
                for (String tableName : tableNameExpression) {
                    resultSegs.add(databaseName + DOT_DELIMITER + tableName);
                }
            }
            return resultSegs;
        } else {
            Map<String, List<String>> databaseGroup = logicalTable.getActualDataNodes().stream()
                    .collect(Collectors.groupingBy(DataNode::getSchemaName, () -> new TreeMap<>(createSchemaNameComparator(databaseNamePattern)),
                            Collectors.mapping(DataNode::getTableName, Collectors.toList())));

            if (canBeMerged(databaseGroup, databaseNamePattern, tableNamePattern)) {
                List<String> databaseExpressions =
                        replacePlaceholdersWithRanges(databaseNamePattern, new ArrayList<>(databaseGroup.keySet()));
                Verify.singleton(databaseExpressions, "databaseExpressions.size");

                List<String> tableExpressions = replacePlaceholdersWithRanges(tableNamePattern,
                        databaseGroup.values().stream().flatMap(Collection::stream)
                                .collect(Collectors.toList()));
                Verify.singleton(tableExpressions, "tableExpressions.size");
                return Arrays.asList(databaseExpressions.get(0) + DOT_DELIMITER + tableExpressions.get(0));
            } else {
                // 无法合并的情况，返回每个分组的表达式
                Map<String, List<String>> databaseName2ExpressionSegs = databaseGroup.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey,
                                entry -> replacePlaceholdersWithRanges(tableNamePattern, entry.getValue()),
                                (oldValue, newValue) -> oldValue, () -> new TreeMap<>(createSchemaNameComparator(databaseNamePattern))));
                return databaseName2ExpressionSegs.entrySet().stream()
                        .map(entry -> entry.getValue().stream().map(r -> entry.getKey() + DOT_DELIMITER + r)
                                .collect(Collectors.joining(COMMA_DELIMITER)))
                        .collect(Collectors.toList());
            }
        }
    }

    private static boolean canBeMerged(Map<String, List<String>> databaseGroup, String databaseNamePattern,
            String tableNamePattern) {
        // 可以合并的条件是，分库能被表示成 连续区间或步长的形式 且 各分库的表达式能按照分库的自然顺序组合成 连续区间或步长

        // 每个分库下的表数量不完全一致，无法合并
        if (!isSameValueSize(databaseGroup)) {
            return false;
        }

        List<String> databaseExpressions =
                replacePlaceholdersWithRanges(databaseNamePattern, new ArrayList<>(databaseGroup.keySet()));
        List<String> tableExpressions = replacePlaceholdersWithRanges(tableNamePattern,
                databaseGroup.values().stream().flatMap(Collection::stream).collect(Collectors.toList()));

        // 如果 分库表达式 或者 分表表达式 不能表示为一个表达式的形式，无法合并
        if (databaseExpressions.size() != 1 || tableExpressions.size() != 1) {
            return false;
        }

        // 如果每个序号都是按照自然序均分到每个库的，则可以合并
        return isOrderByEachElement(extractNumbers(databaseGroup, tableNamePattern));
    }


    // 根据提供的正则表达式创建一个Comparator，实现类似 SQL order by 的效果
    private static Comparator<String> createSchemaNameComparator(String pattern) {
        String regex = pattern.replace(PATTERN_PLACEHOLDER_REGEX, "(\\\\d+)");
        Pattern compiledPattern = Pattern.compile(regex);

        return (schema1, schema2) -> {
            Matcher matcher1 = compiledPattern.matcher(schema1);
            Matcher matcher2 = compiledPattern.matcher(schema2);

            if (!matcher1.matches() || !matcher2.matches()) {
                return schema1.compareTo(schema2);
            }

            // 从左到右比较所有找到的数字
            for (int i = 1; i <= Math.min(matcher1.groupCount(), matcher2.groupCount()); i++) {
                int num1 = Integer.parseInt(matcher1.group(i));
                int num2 = Integer.parseInt(matcher2.group(i));
                if (num1 != num2) {
                    return Integer.compare(num1, num2);
                }
            }

            return schema1.compareTo(schema2);
        };
    }

    private static List<List<String>> extractNumbers(Map<String, List<String>> databaseName2Tables,
            String tablePattern) {
        List<List<String>> allNumberLists = new ArrayList<>();
        Pattern patternRegex = Pattern.compile(tablePattern.replaceAll(PATTERN_PLACEHOLDER_REGEX, "(\\\\d+)"));

        for (Map.Entry<String, List<String>> entry : databaseName2Tables.entrySet()) {
            List<String> tableNames = entry.getValue();
            for (String tableName : tableNames) {
                Matcher matcher = patternRegex.matcher(tableName);
                List<String> numbers = new ArrayList<>();
                if (matcher.matches()) {
                    int groupCount = matcher.groupCount();
                    for (int i = 1; i <= groupCount; i++) {
                        numbers.add(matcher.group(i));
                    }
                }
                allNumberLists.add(numbers);
            }
        }
        return allNumberLists;
    }

    public static boolean isOrderByEachElement(List<List<String>> lists) {
        if (lists == null || lists.size() < 2) {
            // 如果列表为空或者只有一个子列表，我们认为它是有序的
            return true;
        }

        for (int i = 1; i < lists.size(); i++) {
            boolean isCurrentListBigger = false;
            for (int j = 0; j < lists.get(i).size(); j++) {
                int prev = Integer.parseInt(lists.get(i - 1).get(j));
                int curr = Integer.parseInt(lists.get(i).get(j));
                if (prev > curr) {
                    return false; // 当前列的元素没有按照顺序排列
                } else if (prev < curr) {
                    isCurrentListBigger = true; // 这意味着不需要比较后续的列
                    break;
                }
                // 如果 prev == curr, 我们需要继续比较下一列
            }
            // 如果在所有列中，所有元素都相等，我们将其视为按顺序排列
            if (!isCurrentListBigger && i == lists.size() - 1) {
                return true;
            }
        }
        return true; // 所有的列表都正确排序
    }

    private static boolean isSameValueSize(Map<String, List<String>> map) {
        // 用于存储列表长度，初始化为-1表示尚未设置
        int listLength = -1;

        for (List<String> list : map.values()) {
            if (listLength == -1) {
                // 设置第一个列表的长度为参照标准
                listLength = list.size();
            } else if (list.size() != listLength) {
                // 如果找到长度不一致的列表，返回false
                return false;
            }
        }

        // 所有列表长度一致
        return true;
    }

    private static String replaceRangeSign(String expression) {
        // 找到最后一个']'的位置
        int lastCloseIndex = expression.lastIndexOf(']');
        // 如果存在，则替换为']]
        if (lastCloseIndex != -1) {
            expression = expression.substring(0, lastCloseIndex) + "]]" + expression.substring(lastCloseIndex + 1);
        }

        // 找到最后一个'['的位置
        int lastOpenIndex = expression.lastIndexOf('[');
        // 如果存在，则替换为'[['
        if (lastOpenIndex != -1) {
            expression = expression.substring(0, lastOpenIndex) + "[[" + expression.substring(lastOpenIndex + 1);
        }

        return expression;
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
                        Collectors.groupingBy(pair -> pair.left, () ->  new TreeMap<>(Comparator.comparingInt(Integer::parseInt)), Collectors.toCollection(ArrayList::new)))
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

        // 创建一个TreeSet集合，使用自定义Comparator按数字大小对字符串进行排序
        TreeSet<String> sortedNumberStrings = new TreeSet<>(Comparator.comparingInt(Integer::parseInt));
        sortedNumberStrings.addAll(numberStrings);
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
