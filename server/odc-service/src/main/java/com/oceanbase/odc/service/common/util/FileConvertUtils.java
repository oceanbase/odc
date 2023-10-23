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
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Row.MissingCellPolicy;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.PreConditions;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2021/11/29 下午1:02
 * @Description: []
 */
@Slf4j
public class FileConvertUtils {

    private final static int DEFAULT_SHEET_INDEX = 0;
    private final static int DEFAULT_HEADER_ROW_NUM = 0;

    public static List<Map<String, String>> convertXlsRowsToMapList(InputStream inputStream) throws IOException {
        List<Map<String, String>> returnValue = new ArrayList<>();
        Sheet sheet = new XSSFWorkbook(inputStream).getSheetAt(DEFAULT_SHEET_INDEX);
        int firstRowNum = sheet.getFirstRowNum();
        int lastRowNum = sheet.getLastRowNum();
        if (firstRowNum < DEFAULT_HEADER_ROW_NUM || lastRowNum < DEFAULT_HEADER_ROW_NUM + 1) {
            // Empty sheet
            return returnValue;
        }
        Row header = sheet.getRow(firstRowNum);
        int columnNum = getNotBlankColumnNum(header);
        for (int i = firstRowNum + 1; i <= lastRowNum; i++) {
            Map<String, String> map = new HashMap<>();
            Row row = sheet.getRow(i);
            if (isEmptyRow(row)) {
                // Skip empty row
                continue;
            }
            for (int j = 0; j < columnNum; j++) {
                DataFormatter formatter = new DataFormatter();
                String key = formatter.formatCellValue(header.getCell(j));
                Cell cell = row.getCell(j, MissingCellPolicy.CREATE_NULL_AS_BLANK);
                String value = formatter.formatCellValue(cell);
                map.put(key, value);
            }
            returnValue.add(map);
        }
        return returnValue;
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
                    .withEscape('\\')
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

    private static int getNotBlankColumnNum(Row row) {
        int columnNum = 0;
        for (Cell cell : row) {
            if (!isEmptyCell(cell)) {
                columnNum++;
            }
        }
        return columnNum;
    }

    private static boolean isEmptyRow(Row row) {
        if (row == null || row.getPhysicalNumberOfCells() == 0 || row.getLastCellNum() <= 0) {
            return true;
        }
        for (Cell cell : row) {
            if (!isEmptyCell(cell)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isEmptyCell(Cell cell) {
        return cell == null || StringUtils.isBlank(getCellValue(cell));
    }

    private static String getCellValue(Cell cell) {
        if (cell != null) {
            switch (cell.getCellType()) {
                case STRING:
                    return cell.getStringCellValue();
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        return cell.getDateCellValue().toString();
                    } else {
                        return Double.toString(cell.getNumericCellValue());
                    }
                case BOOLEAN:
                    return Boolean.toString(cell.getBooleanCellValue());
                case FORMULA:
                    return cell.getCellFormula();
            }
        }
        return StringUtils.EMPTY;
    }

}
