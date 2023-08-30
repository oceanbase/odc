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
package com.oceanbase.odc.service.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.oceanbase.odc.service.common.util.FileConvertUtils;

public class FileConvertUtilsTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private static final String xlsFileName = "test-xlsx";

    private static final String csvContent = "a,1";

    private File csvFile;

    @Before
    public void setUp() throws Exception {
        this.csvFile = folder.newFile("result-set.csv");
        FileUtils.writeStringToFile(csvFile, csvContent, StandardCharsets.UTF_8);
    }

    @Test
    public void test_convertXlsRowsToMapList_normalWorkbook() throws IOException {
        try (InputStream stream = Files.newInputStream(Paths.get("src/test/resources/common/workbook_normal.xlsx"))) {
            Assert.assertEquals(4, FileConvertUtils.convertXlsRowsToMapList(stream).size());
        }
    }

    @Test
    public void test_convertXlsRowsToMapList_workbookWithEmptyRows() throws IOException {
        try (InputStream stream =
                Files.newInputStream(Paths.get("src/test/resources/common/workbook_with_empty_rows.xlsx"))) {
            Assert.assertEquals(1, FileConvertUtils.convertXlsRowsToMapList(stream).size());
        }
    }

    @Test
    public void test_convertXlsRowsToMapList_workbookWithEmptyColumns() throws IOException {
        try (InputStream stream =
                Files.newInputStream(Paths.get("src/test/resources/common/workbook_with_empty_columns.xlsx"))) {
            Assert.assertEquals(0, FileConvertUtils.convertXlsRowsToMapList(stream).size());
        }
    }

    @Test
    public void test_convertXlsRowsToMapList_emptyWorkbook() throws IOException {
        try (InputStream stream =
                Files.newInputStream(Paths.get("src/test/resources/common/workbook_empty_content.xlsx"))) {
            Assert.assertEquals(0, FileConvertUtils.convertXlsRowsToMapList(stream).size());
        }
    }

    @Test
    public void testConvertCsvToXls_AnotherSheetContentsIsNull_XlsContentEqualsCsvContent() throws IOException {
        String xlsxFilePath =
                FileConvertUtils.convertCsvToXls(this.csvFile.getAbsolutePath(),
                        folder.getRoot().getAbsolutePath() + "/" + xlsFileName,
                        null);
        try (Workbook workbook = new XSSFWorkbook(new FileInputStream(xlsxFilePath))) {
            String valueInCell = workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue();
            Assert.assertEquals("a", valueInCell);
        }
    }

    @Test
    public void testConvertCsvToXls_AnotherSheetContentsIsNotNull_XlsHasAnotherSheet() throws IOException {
        String xlsxFilePath =
                FileConvertUtils.convertCsvToXls(this.csvFile.getAbsolutePath(),
                        folder.getRoot().getAbsolutePath() + "/" + xlsFileName,
                        Arrays.asList("select * from a;"));
        try (Workbook workbook = new XSSFWorkbook(new FileInputStream(xlsxFilePath))) {
            String valueInAnotherSheet = workbook.getSheetAt(1).getRow(0).getCell(0).getStringCellValue();
            Assert.assertEquals("select * from a;", valueInAnotherSheet);
        }
    }
}
