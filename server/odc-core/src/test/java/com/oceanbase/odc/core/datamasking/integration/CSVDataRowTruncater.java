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
public class CSVDataRowTruncater implements CSVDataProcessor {
    private static final CSVFormat csvFormat = CSVFormat.DEFAULT.withRecordSeparator('\n');
    private Integer rowLimit;

    public CSVDataRowTruncater(int rowLimit) {
        this.rowLimit = rowLimit;
    }

    @Override
    public CSVData process(CSVData csvData) {
        if (Objects.isNull(rowLimit)) {
            return csvData;
        }
        try {
            CSVParser parser = CSVParser.parse(csvData.getContent(), csvFormat);
            StringBuilder stringBuilder = new StringBuilder();
            CSVPrinter csvPrinter = new CSVPrinter(stringBuilder, csvFormat);

            List<String> headers = csvData.getHeaders();
            Iterator<CSVRecord> iterator = parser.iterator();
            if (Objects.nonNull(headers) && headers.size() > 0) {
                // skip the header line
                iterator.next();
                csvPrinter.printRecord(headers);
            }
            int i = 0;
            while (iterator.hasNext() && i < rowLimit) {
                CSVRecord record = iterator.next();
                List<String> x = new ArrayList<>();
                Iterator<String> stringIterator = record.iterator();
                while (stringIterator.hasNext()) {
                    x.add(stringIterator.next());
                }
                csvPrinter.printRecord(x);
                i++;
            }
            csvData.setContent(stringBuilder.toString());
            return csvData;
        } catch (Exception e) {

        }
        return csvData;
    }
}
