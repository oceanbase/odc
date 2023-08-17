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
package com.oceanbase.odc.service.common.util;

import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Row.MissingCellPolicy;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.oceanbase.odc.core.shared.PreConditions;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2021/11/29 下午1:02
 * @Description: []
 */
@Slf4j
public class FileConvertUtils {

    public static List<Map<String, String>> convertXlxToList(InputStream inputStream) throws IOException {

        XSSFWorkbook xssfWorkbook = new XSSFWorkbook(inputStream);
        Sheet sheet = xssfWorkbook.getSheetAt(0);
        Row firstRow = sheet.getRow(0);

        // Excel内容校验
        List<Map<String, String>> list = new ArrayList<>();
        int rowNum = sheet.getPhysicalNumberOfRows();
        int cellNum = firstRow.getPhysicalNumberOfCells();
        for (int i = 1; i < rowNum; i++) {
            Row row = sheet.getRow(i); // 获取表格中第i行的数据
            Map<String, String> map = new HashMap<>();
            for (int j = 0; j < cellNum; j++) { // 获取表格中第i行第j列的数据
                DataFormatter formatter = new DataFormatter();
                String key = formatter.formatCellValue(firstRow.getCell(j));
                Cell cell = row.getCell(j, MissingCellPolicy.CREATE_NULL_AS_BLANK);
                String value = formatter.formatCellValue(cell);
                map.put(key, value);
            }
            list.add(map);
        }
        return list;
    }


    public static String convertCsvToXls(String csvFilePath, String xlsFilePath,
            List<String> anotherSheetContents)
            throws IOException, IllegalArgumentException {
        PreConditions.notBlank(csvFilePath, "csvFilePath");
        PreConditions.notBlank(xlsFilePath, "xlsFilePath");
        SXSSFSheet sheet;
        xlsFilePath = xlsFilePath.trim();
        try (SXSSFWorkbook workBook = new SXSSFWorkbook();
                FileOutputStream fileOutputStream = new FileOutputStream(xlsFilePath)) {
            /**
             * Read CSV using org.apache.commons.CSVParser
             */
            Reader in = new FileReader(csvFilePath);
            Iterable<CSVRecord> records = CSVFormat.DEFAULT
                    .withSkipHeaderRecord(false)
                    .parse(in);

            /**
             * Write Excel using org.apache.poi
             */
            sheet = workBook.createSheet();
            int rowNum = 0;
            for (CSVRecord csvRecord : records) {
                int column = 0;
                Row currentRow = sheet.createRow(rowNum);
                Iterator<String> it = csvRecord.iterator();
                while (it.hasNext()) {
                    currentRow.createCell(column++).setCellValue(it.next());
                }
                rowNum++;
            }
            if (CollectionUtils.isNotEmpty(anotherSheetContents)) {
                SXSSFSheet anotherSheet = workBook.createSheet();
                rowNum = 0;
                for (String content : anotherSheetContents) {
                    Row currentRow = anotherSheet.createRow(rowNum++);
                    currentRow.createCell(0).setCellValue(content);
                }
            }
            workBook.write(fileOutputStream);
            // Dispose of temporary files, and will render the workbook unusable.
            workBook.dispose();
        }
        return xlsFilePath;
    }
}
