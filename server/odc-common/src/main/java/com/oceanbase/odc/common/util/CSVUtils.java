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

import java.io.IOException;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.Validate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.oceanbase.odc.common.json.JacksonFactory;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2021/9/1 下午9:37
 * @Description: []
 */
@Slf4j
public class CSVUtils {
    private static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    private static final CSVFormat csvFormat = CSVFormat.DEFAULT.withRecordSeparator('\n'); // 每条记录间隔符
    private static final ObjectMapper objectMapper =
            JacksonFactory.unsafeJsonMapper().disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                    .setDateFormat(new SimpleDateFormat(DEFAULT_DATE_FORMAT));

    public static String buildCSVFormatData(List<String> header, List<List<Object>> records) throws IOException {
        Validate.notEmpty(records, "Records must not be empty when building CSV string");
        StringBuilder stringBuilder = new StringBuilder();
        try (CSVPrinter csvPrinter = new CSVPrinter(stringBuilder, csvFormat)) {
            csvPrinter.printRecord(header);
            csvPrinter.printRecords(records);
            csvPrinter.flush();
        } catch (Exception e) {
            log.warn("build CSV format string failed, errorMessage={}", e.getMessage());
            throw new IllegalStateException("build CSV format string failed");
        }
        return stringBuilder.toString();
    }

    public static <T> String buildCSVFormatData(Iterable<T> records, Class<T> clazz) {
        StringBuilder stringBuilder = new StringBuilder();
        Field[] declaredFields = clazz.getDeclaredFields();
        // 忽略 nested class 的特殊字段
        List<String> headers = Arrays.stream(declaredFields)
                .filter(f -> !StringUtils.startsWith(f.getName(), "this$"))
                .map(Field::getName).collect(Collectors.toList());
        try (CSVPrinter csvPrinter = new CSVPrinter(stringBuilder, csvFormat)) {
            csvPrinter.printRecord(headers);
            for (T record : records) {
                String json = objectMapper.writeValueAsString(record);
                JsonNode jsonNode = objectMapper.readTree(json);
                if (!(jsonNode instanceof ObjectNode)) {
                    continue;
                }
                ObjectNode objectNode = (ObjectNode) jsonNode;
                Iterator<JsonNode> elements = objectNode.elements();
                List<Object> row = new ArrayList<>(headers.size());
                elements.forEachRemaining(t -> {
                    row.add(t.asText());
                });
                csvPrinter.printRecord(row);
            }
            csvPrinter.flush();
        } catch (Exception e) {
            log.warn("build CSV format string failed, errorMessage={}", e.getMessage());
            throw new IllegalStateException("build CSV format string failed");
        }
        return stringBuilder.toString();
    }
}

