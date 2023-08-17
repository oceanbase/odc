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
package com.oceanbase.odc.core.datamasking.integration;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

/**
 * @author wenniu.ly
 * @date 2022/8/24
 */
public class CSVDataColumnTruncater implements CSVDataProcessor {
    private static final CSVFormat csvFormat = CSVFormat.DEFAULT.withRecordSeparator('\n');
    private List<String> removedColumns;

    public CSVDataColumnTruncater(List<String> removedColumns) {
        this.removedColumns = removedColumns;
    }

    @Override
    public CSVData process(CSVData csvData) {
        if (Objects.isNull(removedColumns) || removedColumns.size() == 0) {
            return csvData;
        }
        try {
            CSVParser parser = CSVParser.parse(csvData.getContent(), csvFormat);
            StringBuilder stringBuilder = new StringBuilder();
            CSVPrinter csvPrinter = new CSVPrinter(stringBuilder, csvFormat);

            List<String> headers = csvData.getHeaders();
            Iterator<CSVRecord> iterator = parser.iterator();
            if (Objects.nonNull(headers) && headers.size() > 0) {
                List<Integer> indexes = findRemovedColumnIndexes(headers);
                List<String> newHeader = getNewHeaders(headers, indexes);
                csvPrinter.printRecord(newHeader);
                // skip the header line
                iterator.next();

                while (iterator.hasNext()) {
                    CSVRecord record = iterator.next();
                    int i = 0;
                    List<String> newRecord = new ArrayList<>();
                    Iterator<String> recordIterator = record.iterator();
                    while (recordIterator.hasNext()) {
                        String value = recordIterator.next();
                        if (!indexes.contains(i)) {
                            newRecord.add(value);
                        }
                        i++;
                    }
                    csvPrinter.printRecord(newRecord);
                }
                csvData.setContent(stringBuilder.toString());
                csvData.setHeaders(newHeader);
                return csvData;
            }
        } catch (Exception e) {

        }
        return csvData;
    }

    private List<Integer> findRemovedColumnIndexes(List<String> headers) {
        List<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < headers.size(); i++) {
            String columnName = headers.get(i);
            if (removedColumns.contains(columnName)) {
                indexes.add(i);
            }
        }
        return indexes;
    }

    private List<String> getNewHeaders(List<String> headers, List<Integer> indexes) {
        List<String> newHeaders = new ArrayList<>();
        for (int i = 0; i < headers.size(); i++) {
            if (!indexes.contains(i)) {
                newHeaders.add(headers.get(i));
            }
        }
        return newHeaders;
    }

}
