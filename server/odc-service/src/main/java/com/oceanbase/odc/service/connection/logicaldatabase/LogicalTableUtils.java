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
import com.oceanbase.odc.common.util.StringUtils;
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
    private static final String DIGIT_REGEX_CAPTURING_GROUP_REPLACEMENT = "(\\\\d+)";
    private static final String DOT_DELIMITER = ".";
    private static final String COMMA_DELIMITER = ",";
    private static final String LEFT_SQUARE_BRACKET = "[";
    private static final String RIGHT_SQUARE_BRACKET = "]";
    private static final String DOUBLE_LEFT_SQUARE_BRACKET = "[[";
    private static final String DOUBLE_RIGHT_SQUARE_BRACKET = "]]";
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
                                        .stream().map(r -> r + DOT_DELIMITER + tableNamePattern)
                                        .collect(Collectors.toList());
                table.setFullNameExpression(String.join(COMMA_DELIMITER, range));
            } else if (tableNamePattern.contains(PATTERN_PLACEHOLDER)) {
                // 只分表，不分库
                List<String> range = replacePlaceholdersWithRanges(tableNamePattern,
                        table.getActualDataNodes().stream()
                                .map(node -> node.getTableName())
                                .collect(Collectors.toList()))
                                        .stream().map(r -> databaseNamePattern + DOT_DELIMITER + r)
                                        .collect(Collectors.toList());
                table.setFullNameExpression(String.join(COMMA_DELIMITER, range));
            } else {
                throw new UnexpectedException(
                        String.format("Unexpected pattern: %s.%s", databaseNamePattern, tableNamePattern));
            }
        });
        return logicalTables;
    }

    private static List<LogicalTable> identifyLogicalTables(@Valid @NotEmpty List<DataNode> dataNodes) {
        // replace all table names with a pattern that replaces digits with [#]
        Map<String, List<DataNode>> basePattern2Tables = dataNodes.stream().collect(
                Collectors.groupingBy(
                        dataNode -> dataNode.getTableName().replaceAll(DIGIT_PATTERN.pattern(), PATTERN_PLACEHOLDER)));
        basePattern2Tables.entrySet().removeIf(entry -> entry.getValue().size() == 1);


        Map<String, List<DataNode>> finalPatterns = new LinkedHashMap<>();

        basePattern2Tables.forEach((basePattern, nodes) -> {
            // check which [#] should be kept and which should be replaced with actual numbers
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


    private static List<String> replacePlaceholdersWithRanges(LogicalTable logicalTable) {
        String databaseNamePattern = logicalTable.getDatabaseNamePattern();
        String tableNamePattern = logicalTable.getTableNamePattern();
        List<String> databaseNames = logicalTable.getActualDataNodes().stream()
                .map(DataNode::getSchemaName)
                .collect(Collectors.toList());
        List<String> tableNames = logicalTable.getActualDataNodes().stream()
                .map(DataNode::getTableName)
                .collect(Collectors.toList());

        // if every database has the same table names, we should merge them into one expression with double
        // brackets
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
                    .collect(Collectors.groupingBy(DataNode::getSchemaName,
                            () -> new TreeMap<>(createSchemaNameComparator(databaseNamePattern)),
                            Collectors.mapping(DataNode::getTableName, Collectors.toList())));

            // if could be merged, we should merge database pattern and table pattern into one expression
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
                // if could not be merged, we should return every expression group by database name
                Map<String, List<String>> databaseName2ExpressionSegs = databaseGroup.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey,
                                entry -> replacePlaceholdersWithRanges(tableNamePattern, entry.getValue()),
                                (oldValue, newValue) -> oldValue,
                                () -> new TreeMap<>(createSchemaNameComparator(databaseNamePattern))));
                return databaseName2ExpressionSegs.entrySet().stream()
                        .map(entry -> entry.getValue().stream().map(r -> entry.getKey() + DOT_DELIMITER + r)
                                .collect(Collectors.joining(COMMA_DELIMITER)))
                        .collect(Collectors.toList());
            }
        }
    }

    private static boolean canBeMerged(Map<String, List<String>> databaseGroup, String databaseNamePattern,
            String tableNamePattern) {
        // if the size of values in each group is not the same, we can not merge them
        if (!hasSameValueSize(databaseGroup)) {
            return false;
        }

        List<String> databaseExpressions =
                replacePlaceholdersWithRanges(databaseNamePattern, new ArrayList<>(databaseGroup.keySet()));
        List<String> tableExpressions = replacePlaceholdersWithRanges(tableNamePattern,
                databaseGroup.values().stream().flatMap(Collection::stream).collect(Collectors.toList()));

        // if the size of databaseExpressions and tableExpressions is not 1, we can not merge them
        if (databaseExpressions.size() != 1 || tableExpressions.size() != 1) {
            return false;
        }

        // if the table name is order by each element, we can merge them
        return isOrderByEachElement(extractNumbers(databaseGroup, tableNamePattern));
    }

    private static Comparator<String> createSchemaNameComparator(String pattern) {
        // this is a comparator that can compare two number strings
        // the logic is the same with Order By multiple columns in SQL
        String regex = pattern.replaceAll(PATTERN_PLACEHOLDER_REGEX, DIGIT_REGEX_CAPTURING_GROUP_REPLACEMENT);
        Pattern compiledPattern = Pattern.compile(regex);

        return (first, second) -> {
            Matcher matcher1 = compiledPattern.matcher(first);
            Matcher matcher2 = compiledPattern.matcher(second);

            if (!matcher1.matches() || !matcher2.matches()) {
                return first.compareTo(second);
            }

            for (int i = 1; i <= Math.min(matcher1.groupCount(), matcher2.groupCount()); i++) {
                int num1 = Integer.parseInt(matcher1.group(i));
                int num2 = Integer.parseInt(matcher2.group(i));
                if (num1 != num2) {
                    return Integer.compare(num1, num2);
                }
            }

            return first.compareTo(second);
        };
    }

    private static List<List<String>> extractNumbers(Map<String, List<String>> databaseName2Tables,
            String tablePattern) {
        List<List<String>> allNumberLists = new ArrayList<>();
        Pattern patternRegex = Pattern
                .compile(tablePattern.replaceAll(PATTERN_PLACEHOLDER_REGEX, DIGIT_REGEX_CAPTURING_GROUP_REPLACEMENT));

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
            return true;
        }

        // compare each element in the list
        // the logic is quite similar to the logic Order By multiple columns in SQL
        for (int i = 1; i < lists.size(); i++) {
            boolean isCurrentListBigger = false;
            for (int j = 0; j < lists.get(i).size(); j++) {
                int prev = Integer.parseInt(lists.get(i - 1).get(j));
                int curr = Integer.parseInt(lists.get(i).get(j));
                if (prev > curr) {
                    return false;
                } else if (prev < curr) {
                    isCurrentListBigger = true;
                    break;
                }
            }
            if (!isCurrentListBigger && i == lists.size() - 1) {
                return true;
            }
        }
        return true;
    }

    private static boolean hasSameValueSize(Map<String, List<String>> map) {
        // check if the map values have the same size
        int listLength = -1;
        for (List<String> list : map.values()) {
            if (listLength == -1) {
                listLength = list.size();
            } else if (list.size() != listLength) {
                return false;
            }
        }
        return true;
    }

    private static String replaceRangeSign(String expression) {
        // find the last ']'
        int lastCloseIndex = expression.lastIndexOf(RIGHT_SQUARE_BRACKET);
        // if exists, replace it with ']]'
        if (lastCloseIndex != -1) {
            expression = expression.substring(0, lastCloseIndex) + DOUBLE_RIGHT_SQUARE_BRACKET
                    + expression.substring(lastCloseIndex + 1);
        }

        // find the last '['
        int lastOpenIndex = expression.lastIndexOf(LEFT_SQUARE_BRACKET);
        // if exists, replace it with '[['
        if (lastOpenIndex != -1) {
            expression = expression.substring(0, lastOpenIndex) + DOUBLE_LEFT_SQUARE_BRACKET
                    + expression.substring(lastOpenIndex + 1);
        }

        return expression;
    }

    private static boolean hasSameTableNames(LogicalTable logicalTable) {
        Map<String, List<String>> schemaName2TableNames = logicalTable.getActualDataNodes().stream()
                .collect(Collectors.groupingBy(DataNode::getSchemaName, TreeMap::new,
                        Collectors.mapping(DataNode::getTableName, Collectors.toList())));
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

        Pattern patternRegex =
                Pattern.compile(pattern.replaceAll(PATTERN_PLACEHOLDER_REGEX, DIGIT_REGEX_CAPTURING_GROUP_REPLACEMENT));
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
            // it is a Cartesian product
            if (rangePair != null) {
                return Arrays.asList(parts[0] + rangePair.left + parts[1] + rangePair.right + parts[2]);
            } else {
                // it is not a Cartesian product, we should group by the first [#] number and process the second [#]
                // separately
                List<String> result = new ArrayList<>();
                numberPairs.stream().collect(
                        Collectors.groupingBy(pair -> pair.left,
                                () -> new TreeMap<>(Comparator.comparingInt(Integer::parseInt)),
                                Collectors.toCollection(ArrayList::new)))
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
        // if it is a Cartesian product, we generate range for each part
        if (isCartesianProduct(numberPairs)) {
            Set<String> leftSet =
                    numberPairs.stream().map(pair -> pair.left).collect(Collectors.toCollection(TreeSet::new));
            Set<String> rightSet =
                    numberPairs.stream().map(pair -> pair.right).collect(Collectors.toCollection(TreeSet::new));
            return new Pair<>(generateRangeOrList(leftSet), generateRangeOrList(rightSet));
        }
        // if it is not a Cartesian product, we return null
        return null;
    }

    public static boolean isCartesianProduct(List<Pair<String, String>> pairs) {
        Map<String, Set<String>> map = new HashMap<>();

        for (Pair<String, String> pair : pairs) {
            String left = pair.left;
            String right = pair.right;

            if (!map.containsKey(left)) {
                map.put(left, new HashSet<>());
            }
            map.get(left).add(right);
        }

        Set<String> referenceSet = null;
        for (Set<String> rightSet : map.values()) {
            if (referenceSet == null) {
                referenceSet = rightSet;
                continue;
            }
            if (!CollectionUtils.isEqualCollection(referenceSet, rightSet)) {
                return false;
            }
        }
        return true;
    }

    private static String generateRangeOrList(Set<String> numberStrings) {
        if (numberStrings.size() == 1) {
            return numberStrings.iterator().next();
        }

        TreeSet<String> sortedNumberStrings = new TreeSet<>(Comparator.comparingInt(Integer::parseInt));
        sortedNumberStrings.addAll(numberStrings);

        boolean isConsecutive = true;
        String first = sortedNumberStrings.first();
        int step = Integer.MIN_VALUE;
        String prevNumber = first;

        for (String currentNumber : sortedNumberStrings.tailSet(first, false)) {
            int currentStep = Integer.parseInt(currentNumber) - Integer.parseInt(prevNumber);
            if (step == Integer.MIN_VALUE) {
                step = currentStep;
            } else if (currentStep != step) {
                // step is not consistent
                isConsecutive = false;
                break;
            }
            prevNumber = currentNumber;
        }

        if (isConsecutive) {
            String start = sortedNumberStrings.first();
            String end = sortedNumberStrings.last();
            if (StringUtils.equals(start, end)) {
                return String.format("[%s]", start);
            } else if (step == 1) {
                // if step is 1, we could use continuous range
                return String.format("[%s-%s]", start, end);
            } else {
                // if step is not 1, we need to show the step
                return String.format("[%s-%s:%d]", start, end, step);
            }
        } else {
            // if not consecutive, we just list them all
            return String.format("[%s]", String.join(COMMA_DELIMITER, numberStrings));
        }
    }

    private static String getConsistentNumberPattern(List<String> names) {
        // convert to a list of sections, each section is either a number or a non-number string
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

        // check if the sections are consistent numbers
        List<Boolean> isVariable = new ArrayList<>();
        int sectionCount = nameSections.get(0).size();

        for (int idx = 1; idx < sectionCount; idx += 2) {
            String sampleNumber = nameSections.get(0).get(idx);
            int finalIdx = idx;
            isVariable.add(!nameSections.stream().allMatch(section -> section.get(finalIdx).equals(sampleNumber)));
        }

        // generate the pattern according to the sections
        StringBuilder patternBuilder = new StringBuilder(nameSections.get(0).get(0));

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
