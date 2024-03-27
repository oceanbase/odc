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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.validation.constraints.NotNull;

/**
 * @Author: Lebie
 * @Date: 2024/3/26 14:17
 * @Description: []
 */
public class LogicalTableUtils {
    private static final Pattern DIGIT_PATTERN = Pattern.compile("\\d+");

    public static Map<String, List<String>> identifyLogicalTables(@NotNull List<String> tableNames) {
        // 先将表名分解为由数字序列和非数字序列组成的部分，构建基本模式
        Map<String, List<String>> basePatternToTables = tableNames.stream().collect(
            Collectors.groupingBy(tableName -> tableName.replaceAll(DIGIT_PATTERN.pattern(), "[#]"))
        );
        basePatternToTables.entrySet().removeIf(entry -> entry.getValue().size() == 1);

        // 最终确定的模式到表的映射
        Map<String, List<String>> finalPatterns = new LinkedHashMap<>();

        basePatternToTables.forEach((basePattern, names) -> {
            // 检查哪些位置的[#]应该被保留，哪些应该替换成实际的数字
            String pattern = getConsistentNumberPattern(names);
            finalPatterns.put(pattern, names);
        });

        return finalPatterns;
    }

    public static Map<String, String> generatePatternExpressions(Map<String, List<String>> patternToPhysicalTablesMap) {
        Map<String, String> patternToExpressionMap = new LinkedHashMap<>();

        for (String pattern : patternToPhysicalTablesMap.keySet()) {
            List<String> tableNames = patternToPhysicalTablesMap.get(pattern);
            String expression = replacePlaceholdersWithRanges(pattern, tableNames);
            patternToExpressionMap.put(pattern, expression);
        }

        return patternToExpressionMap;
    }

    public static String replacePlaceholdersWithRanges(String pattern, List<String> tableNames) {
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
        String last = first;
        int step = Integer.MIN_VALUE;
        String prevNumber = first;

        for (String currentNumber : sortedNumberStrings.tailSet(first, false)) {
            last = currentNumber; // 更新末尾数字
            int currentStep = Integer.parseInt(currentNumber) - Integer.parseInt(prevNumber);
            if(step == Integer.MIN_VALUE) {
                step = currentStep; // 设置初始步长
            } else if(currentStep != step) {
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
        StringBuilder patternBuilder = new StringBuilder(nameSections.get(0).get(0));  // 开始先添加第一部分的非数字序列

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


    public static void main(String[] args) {
        List<String> tableNames = Arrays.asList(
            "pp_000","pp_001","pp_002","pp_049",
            "pyn_1_2","pyn_1_3","pyn_2_2",
            "123_a","111_a","rr_202402_t", "rr_202401_t",
            "test_1_2", "test_2_2", "test_3_2","lebie_11_ta2", "lebie_2_ta", "lebie_3_ta", "product3_1", "product3_2", "product3_12","test_1","test_2",
            "inventory4_1", "inventory4_2", "inventory5_1", "inventory5_2",
            "user5_data", "user6_data", "data_2021", "data_2022", "test", "test", "test1", "test12", "1_abc", "2_abc"
        );

        //List<String> tableNames = Arrays.asList("product3_1", "product3_2", "product3_12");

        //List<String> tableNames = Arrays.asList("data_2021", "data_2022");

        Map<String, List<String>> logicalTables = identifyLogicalTables(tableNames);
        logicalTables.forEach((pattern, names) -> System.out.println(pattern + " -> " + names));
        System.out.println("--------------------------------------------------");
        Map<String, String> patternToExpressions = generatePatternExpressions(logicalTables);
        patternToExpressions.forEach((pattern, expression) -> System.out.println(pattern + " -> " + expression));
    }
}
