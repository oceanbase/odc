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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

import com.oceanbase.odc.service.connection.logicaldatabase.model.DataNode;
import com.oceanbase.odc.service.connection.logicaldatabase.model.LogicalTable;

/**
 * @Author: Lebie
 * @Date: 2024/3/26 14:17
 * @Description: []
 */
public class LogicalTableUtilsCopied {
    private static final String DELIMITER = ".";
    private static final Pattern DIGIT_PATTERN = Pattern.compile("\\d+");

    public static List<LogicalTable> generatePatternExpressions(@NotEmpty List<DataNode> dataNodes,
            @NotEmpty List<String> allDatabaseNames) {
        // 找出所有逻辑表，并推导出 逻辑库表名的 databaseNamePattern 和 tableNamePattern
        List<LogicalTable> logicalTables = identifyLogicalTables(dataNodes);

        // 替换掉 pattern 中的 [#] 为具体的数字范围
        logicalTables.stream().forEach(table -> table.setFullNameExpression(replacePlaceholdersWithRanges(table, allDatabaseNames)));
        return logicalTables;
    }

    private static String replacePlaceholdersWithRanges(LogicalTable logicalTable, List<String> allDatabaseNames) {
        return null;
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

    private static String replacePlaceholdersWithRanges(String pattern, List<String> tableNames) {
        String placeholderRegex = "\\[#\\]";
        String[] parts = pattern.split(placeholderRegex, -1);

        List<Set<String>> numberSets = new ArrayList<>();
        for (int i = 0; i < parts.length - 1; i++) {
            numberSets.add(new TreeSet<>());
        }

        Pattern patternRegex = Pattern.compile(pattern.replaceAll(placeholderRegex, "(\\\\d+)"));
        for (String tableName : tableNames) {
            Matcher matcher = patternRegex.matcher(tableName);
            if (matcher.matches()) {
                for (int i = 0; i < parts.length - 1; i++) {
                    // 直接保存数字的字符串形式
                    numberSets.get(i).add(matcher.group(i + 1));
                }
            } else {
                throw new IllegalArgumentException("表名 " + tableName + " 不符合模式 " + pattern);
            }
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            result.append(parts[i]);
            if (i < numberSets.size()) {
                result.append(generateRangeOrList(numberSets.get(i), pattern));
            }
        }

        // 如果发现不连续的数字序列，输出枚举的表名
        if (result.toString().contains(",")) {
            return String.join(",", tableNames);
        }

        return result.toString();
    }


    private static String generateRangeOrList(Set<String> numberStrings, String patternPart) {
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
            // 不连续的数字，输出全部表名
            return String.join(",", sortedNumberStrings);
        }
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
                patternBuilder.append("[#]");
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
