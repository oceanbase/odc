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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.util.Strings;

import com.alibaba.fastjson.JSON;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.model.DBObjectWarningDescriptor;
import com.oceanbase.tools.dbbrowser.model.DBTable.DBTableOptions;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DBSchemaAccessorUtil {

    public static final int OB_MAX_IN_SIZE = 2000;

    public static void parseCreateOptions(DBTableOptions tableOptions, String createOptions) {
        if (StringUtils.contains(createOptions, "ROW_FORMAT = ")) {
            tableOptions.setRowFormat(createOptions.split("ROW_FORMAT = ")[1].split(" ")[0]);
        }
        if (StringUtils.contains(createOptions, "COMPRESSION = ")) {
            tableOptions.setCompressionOption(createOptions.split("COMPRESSION = ")[1].split(" ")[0]);
        }
        if (StringUtils.contains(createOptions, "REPLICA_NUM = ")) {
            tableOptions.setReplicaNum(Integer.valueOf(createOptions.split("REPLICA_NUM = ")[1].split(" ")[0]));
        }
        if (StringUtils.contains(createOptions, "BLOCK_SIZE = ")) {
            tableOptions.setBlockSize(Integer.valueOf(createOptions.split("BLOCK_SIZE = ")[1].split(" ")[0]));
        }
        if (StringUtils.contains(createOptions, "USE_BLOOM_FILTER = ")) {
            tableOptions.setUseBloomFilter("TRUE".equals(createOptions.split("USE_BLOOM_FILTER = ")[1].split(" ")[0]));
        }
        if (StringUtils.contains(createOptions, "TABLET_SIZE = ")) {
            tableOptions.setTabletSize(Long.valueOf(createOptions.split("TABLET_SIZE = ")[1].split(" ")[0]));
        }
    }

    public static List<List<String>> parseListRangePartitionDescription(@NotBlank String description) {
        List<List<String>> valuesList = new ArrayList<>();
        try {
            // LIST_RANGE partitioned by multi columns
            if (StringUtils.startsWith(description, "(")) {
                String jsonStr = "[".concat(description).concat("]");
                jsonStr = StringUtils.replace(jsonStr, "(", "[");
                jsonStr = StringUtils.replace(jsonStr, ")", "]");
                String[][] valuesArrays = JSON.parseObject(jsonStr, String[][].class);
                if (Objects.isNull(valuesArrays)) {
                    return valuesList;
                }
                return Arrays.stream(valuesArrays).map(Arrays::asList).collect(Collectors.toList());

            } else {
                String[] values = description.split(",");
                for (String value : values) {
                    valuesList.add(Arrays.asList(value));
                }
            }
        } catch (Exception ex) {
            log.warn("parse list range description from metadata failed, ex=", ex);
        }
        return valuesList;
    }

    public static String parseListRangeValuesList(@NotNull List<List<String>> valuesList) {
        if (CollectionUtils.isEmpty(valuesList)) {
            return StringUtils.EMPTY;
        }
        StringBuilder sb = new StringBuilder();
        List<String> singleList = new ArrayList<>();
        try {
            for (int i = 0; i < valuesList.size(); i++) {
                singleList.add(String.join(",", valuesList.get(i)));
            }
            for (int j = 0; j < singleList.size(); j++) {
                if (!CollectionUtils.isEmpty(valuesList.get(j))) {
                    if (valuesList.get(j).size() > 1) {
                        singleList.set(j, "(".concat(singleList.get(j)).concat(")"));
                    }
                }
            }
            sb.append(String.join(",", singleList));
        } catch (Exception ex) {
            log.warn("parse list range valuesList failed, ex=", ex);
        }
        return sb.toString();
    }

    /**
     * Oracle 字典表 ALL_TAB_COLS 返回的 typeName 是 fullTypeName 即带 precision 和 scale 的，这个工具方法把 fullTypeName
     * 解析成 typeName example1 input: YEAR(4), output: YEAR example2 input INTERVAL YEAR(2) TO MONTH,
     * output: INTERVAL YEAR TO MONTH
     */
    public static String normalizeTypeName(String allTypeName) {
        if (StringUtils.isEmpty(allTypeName)) {
            return "";
        }
        for (;;) {
            int modIndex = allTypeName.indexOf('(');
            if (modIndex == -1) {
                break;
            }
            int modEnd = allTypeName.indexOf(')', modIndex);
            if (modEnd == -1) {
                break;
            }
            allTypeName = allTypeName.substring(0, modIndex) +
                    (modEnd == allTypeName.length() - 1 ? "" : allTypeName.substring(modEnd + 1));
        }
        return allTypeName;
    }

    public static String parsePrecisionAndScale(String allTypeName) {
        if (StringUtils.isEmpty(allTypeName)) {
            return "";
        }
        String value = Strings.EMPTY;
        int modIndex = allTypeName.indexOf('(');
        if (modIndex == -1) {
            return value;
        }
        int modEnd = allTypeName.indexOf(')', modIndex);
        if (modEnd == -1) {
            return value;

        }
        if (modIndex + 1 > modEnd) {
            return value;
        }
        value = allTypeName.substring(modIndex + 1, modEnd);
        return value;
    }

    public static void obtainOptionsByParse(DBTableOptions tableOptions, String ddl) {
        if (StringUtils.contains(ddl, "ROW_FORMAT = ")) {
            tableOptions.setRowFormat(ddl.split("ROW_FORMAT = ")[1].split(" ")[0]);
        }
        if (StringUtils.contains(ddl, "COMPRESSION = ")) {
            tableOptions.setCompressionOption(ddl.split("COMPRESSION = ")[1].split(" ")[0]);
        }
        if (StringUtils.contains(ddl, "REPLICA_NUM = ")) {
            tableOptions.setReplicaNum(Integer.valueOf(ddl.split("REPLICA_NUM = ")[1].split(" ")[0]));
        }
        if (StringUtils.contains(ddl, "BLOCK_SIZE = ")) {
            tableOptions.setBlockSize(Integer.valueOf(ddl.split("BLOCK_SIZE = ")[1].split(" ")[0]));
        }
        if (StringUtils.contains(ddl, "USE_BLOOM_FILTER = ")) {
            tableOptions.setUseBloomFilter("TRUE".equals(ddl.split("USE_BLOOM_FILTER = ")[1].split(" ")[0]));
        }
        if (StringUtils.contains(ddl, "TABLET_SIZE = ")) {
            tableOptions.setTabletSize(Long.valueOf(ddl.split("TABLET_SIZE = ")[1].split(" ")[0]));
        }
        if (StringUtils.contains(ddl, "CHARSET = ")) {
            tableOptions.setCharsetName(ddl.split("CHARSET = ")[1].split(" ")[0]);
        }
    }

    public static List<String> parseEnumValues(String typeName) {
        List<String> values = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        int pos = 0;
        while (true) {
            int startPos = typeName.indexOf('\'', pos);
            if (startPos < 0) {
                break;
            }
            int endPos = -1;
            for (int i = startPos + 1; i < typeName.length(); i++) {
                char c = typeName.charAt(i);
                if (c == '\'') {
                    if (i < typeName.length() - 2 && typeName.charAt(i + 1) == '\'') {
                        // Quote escape
                        value.append(c);
                        i++;
                        continue;
                    }
                    endPos = i;
                    break;
                }
                value.append(c);
            }
            if (endPos < 0) {
                break;
            }
            values.add(value.toString());
            pos = endPos + 1;
            value.setLength(0);
        }
        return values;
    }

    public static <T extends DBObjectWarningDescriptor> void fillWarning(T dbObjectWarningDescriptor, DBObjectType type,
            String reason) {
        Validate.notNull(dbObjectWarningDescriptor, "dbObjectWarningDescriptor");
        dbObjectWarningDescriptor.setWarning(String.format("%s maybe not accurate due to %s", type.name(), reason));
    }

    public static <Y, E> List<E> partitionFind(List<Y> filterValues, int partitionSize,
            @NonNull Function<List<Y>, List<E>> queryMethod) {
        if (CollectionUtils.isEmpty(filterValues)) {
            return queryMethod.apply(filterValues);
        }
        return ListUtils.partition(filterValues, partitionSize).stream().map(queryMethod)
                .flatMap(Collection::stream).collect(Collectors.toList());
    }

}
