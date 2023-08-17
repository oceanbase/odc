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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.junit.Test;

import com.oceanbase.odc.core.datamasking.config.FieldConfig;
import com.oceanbase.odc.core.datamasking.config.MaskConfig;
import com.oceanbase.odc.core.datamasking.masker.AbstractDataMasker;
import com.oceanbase.odc.core.datamasking.masker.DataMaskerFactory;
import com.oceanbase.odc.core.datamasking.masker.ValueMeta;

/**
 * @author wenniu.ly
 * @date 2022/8/24
 */
public class IntegrationTest {

    @Test
    public void test_integration() throws Exception {
        CSVDataProcessors processors = new CSVDataProcessors();
        processors.addProcessor(new CSVDataColumnTruncater(Arrays.asList("c", "d")));
        processors.addProcessor(new CSVDataRowTruncater(1));
        processors.registerFunction(2, s -> {
            FieldConfig fieldConfig = FieldConfig.builder().fieldName("a").algorithmType("null").build();
            MaskConfig maskConfig = new MaskConfig();
            maskConfig.addFieldConfig(fieldConfig);

            DataMaskerFactory factory = new DataMaskerFactory();
            AbstractDataMasker abstractDataMasker = factory.createDataMasker("single_value", maskConfig);
            ValueMeta valueMeta = new ValueMeta("string", "a");
            return abstractDataMasker.mask(s, valueMeta);
        });

        List<String> header = Arrays.asList("a", "b", "c", "d");
        List<List<Object>> records = new ArrayList<>();
        records.add(Arrays.asList("abc", "123", "-=)", "uuu"));
        records.add(Arrays.asList("def", "456", "*&^", "ttt"));
        CSVData data = new CSVData();
        data.setContent(buildCSVFormatData(header, records));
        data.setHeaders(header);
        CSVData newData = processors.process(data);
        System.out.println(newData.getContent());
    }

    private String buildCSVFormatData(List<String> header, List<List<Object>> records) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        CSVFormat csvFormat = CSVFormat.DEFAULT.withRecordSeparator('\n');
        CSVPrinter csvPrinter = new CSVPrinter(stringBuilder, csvFormat);
        csvPrinter.printRecord(header);
        csvPrinter.printRecords(records);
        csvPrinter.flush();

        return stringBuilder.toString();
    }
}
