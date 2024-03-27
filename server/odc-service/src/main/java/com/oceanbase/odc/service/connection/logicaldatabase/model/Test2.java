package com.oceanbase.odc.service.connection.logicaldatabase.model;

import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Test2 {

    private static final Pattern DIGIT_PATTERN = Pattern.compile("\\d+");

    // 主逻辑方法
    public static String identifyLogicalTablePattern(List<String> tableNames) {
        // 用#[#]替换表名中的数字并进行分组
        String basePattern = DIGIT_PATTERN.matcher(tableNames.get(0)).replaceAll("[#]");
        List<List<Integer>> indexNumbersList = getIndexNumbers(tableNames, basePattern);

        // 生成各个占位符的范围表达式
        List<String> replacements = indexNumbersList.stream()
            .map(Test2::generateRangeReplacement)
            .collect(Collectors.toList());

        // 替换占位符
        Matcher m = Pattern.compile("\\[\\#\\]").matcher(basePattern);
        StringBuffer sb = new StringBuffer();
        int i = 0;
        while (m.find()) {
            m.appendReplacement(sb, replacements.get(i++));
        }
        m.appendTail(sb);

        return sb.toString();
    }

    // 提取模式中#[#]占位符的索引数字
    private static List<List<Integer>> getIndexNumbers(List<String> tableNames, String basePattern) {
        List<List<Integer>> allIndexNumbers = new ArrayList<>();

        // 初始化存储每个占位符数字的列表
        int countPlaceholders = (int) basePattern.chars().filter(ch -> ch == '#').count();
        for (int i = 0; i < countPlaceholders; i++) {
            allIndexNumbers.add(new ArrayList<>());
        }

        // 提取所有数字并根据占位符位置分组存储
        for (String tableName : tableNames) {
            Matcher matcher = DIGIT_PATTERN.matcher(tableName);
            int[] placeholderPositions = IntStream.range(0, basePattern.length())
                .filter(idx -> basePattern.charAt(idx) == '#')
                .toArray();
            int placeholderIndex = 0;
            while (matcher.find()) {
                if (placeholderIndex < placeholderPositions.length && matcher.start() == placeholderPositions[placeholderIndex]) {
                    allIndexNumbers.get(placeholderIndex).add(Integer.parseInt(matcher.group()));
                    placeholderIndex++;
                }
            }
        }

        // 对每个占位符的数字排序并去重
        allIndexNumbers.forEach(numbers -> {
            Collections.sort(numbers);
            List<Integer> uniqueNumbers = numbers.stream().distinct().collect(Collectors.toList());
            numbers.clear();
            numbers.addAll(uniqueNumbers);
        });

        return allIndexNumbers;
    }

    // 根据索引数字生成范围表达式
    private static String generateRangeReplacement(List<Integer> indexNumbers) {
        if (indexNumbers.isEmpty()) {
            return "[#]";
        }
        if (indexNumbers.size() == 1) {
            return indexNumbers.get(0).toString();
        }

        // 检查数字是否连续或者是否有固定步长
        boolean isConsecutive = IntStream.range(1, indexNumbers.size())
            .allMatch(i -> indexNumbers.get(i) - indexNumbers.get(i - 1) == 1);
        if (isConsecutive) {
            return String.format("[%d-%d]", indexNumbers.get(0), indexNumbers.get(indexNumbers.size() - 1));
        }

        boolean isUniformStep = IntStream.range(2, indexNumbers.size())
            .allMatch(i -> indexNumbers.get(i) - indexNumbers.get(i - 1) ==
                           indexNumbers.get(1) - indexNumbers.get(0));
        if (isUniformStep) {
            int step = indexNumbers.get(1) - indexNumbers.get(0);
            return String.format("[%d-%d:%d]", indexNumbers.get(0),
                indexNumbers.get(indexNumbers.size() - 1), step);
        }

        // 不连续也不是固定步长时，直接枚举所有数字
        return indexNumbers.stream()
            .map(Object::toString)
            .collect(Collectors.joining(","));
    }

    public static void main(String[] args) {
        List<String> tableNames = Arrays.asList(
            "y_2023_m_01_t", "y_2023_m_02_t", "y_2024_m_01_t", "y_2024_m_02_t"  // 应返回y_[2023-2024]_m_[01-02]_t
        );

        String logicalTablePattern = identifyLogicalTablePattern(tableNames);
        System.out.println("Pattern: " + logicalTablePattern);
    }
}
