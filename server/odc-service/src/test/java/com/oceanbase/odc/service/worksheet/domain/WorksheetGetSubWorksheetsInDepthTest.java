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
package com.oceanbase.odc.service.worksheet.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.oceanbase.odc.core.shared.exception.BadArgumentException;

/**
 * @author keyang
 * @date 2024/08/15
 * @since 4.3.2
 */
public class WorksheetGetSubWorksheetsInDepthTest {
    final static Long projectId = 1L;
    long id = 0L;

    private Worksheet newWorksheet(String path) {
        return newWorksheet(projectId, new Path(path), id++ + "", 1L);
    }

    public Worksheet newWorksheet(Long projectId, Path path, String objectId, Long creatorId) {
        return new Worksheet(id++, new Date(), new Date(), projectId, path, creatorId,
                null, objectId, null, null);
    }



    Worksheet currentWorksheet;

    @Before
    public void setUp() {
        Path rootPath = new Path("/Worksheets/");
        currentWorksheet = newWorksheet("/Worksheets/");

        Set<Worksheet> subWorksheets = new HashSet<>(Arrays.asList(
                newWorksheet("/Worksheets/folder1/"),
                newWorksheet("/Worksheets/folder1/file2.sql"),
                newWorksheet("/Worksheets/folder1/folder4/"),
                newWorksheet("/Worksheets/folder1/folder4/file5.sql"),
                newWorksheet("/Worksheets/folder3/file3.sql"),
                newWorksheet("/Worksheets/file1.sql")));

        currentWorksheet.setSubWorksheets(subWorksheets);
    }

    @Test
    public void testGetSubWorksheetsInDepth_HappyPath() {

        List<Worksheet> expectedWorksheets1 = Arrays.asList(
                newWorksheet("/Worksheets/folder1/"),
                newWorksheet("/Worksheets/folder1/folder4/"),
                newWorksheet("/Worksheets/folder1/folder4/file5.sql"),
                newWorksheet("/Worksheets/folder1/file2.sql"),
                newWorksheet("/Worksheets/folder3/file3.sql"),
                newWorksheet("/Worksheets/file1.sql"));
        assertEquals(expectedWorksheets1, currentWorksheet.getSubWorksheetsInDepth(0, false));

        List<Worksheet> expectedWorksheets2 = Arrays.asList(
                newWorksheet("/Worksheets/folder1/"),
                newWorksheet("/Worksheets/file1.sql"));
        assertEquals(expectedWorksheets2, currentWorksheet.getSubWorksheetsInDepth(1, false));

        List<Worksheet> expectedWorksheets3 = Arrays.asList(
                newWorksheet("/Worksheets/folder1/"),
                newWorksheet("/Worksheets/folder1/folder4/"),
                newWorksheet("/Worksheets/folder1/file2.sql"),
                newWorksheet("/Worksheets/folder3/file3.sql"),
                newWorksheet("/Worksheets/file1.sql"));
        assertEquals(expectedWorksheets3, currentWorksheet.getSubWorksheetsInDepth(2, false));

        assertEquals(expectedWorksheets1, currentWorksheet.getSubWorksheetsInDepth(3, false));
        assertEquals(expectedWorksheets1, currentWorksheet.getSubWorksheetsInDepth(4, false));
        assertEquals(expectedWorksheets1, currentWorksheet.getSubWorksheetsInDepth(Integer.MAX_VALUE, false));
    }

    @Test
    public void getSubWorksheetsInDepth_NeedToExtractNotExistParent() {
        List<Worksheet> expectedWorksheets1 = Arrays.asList(
                newWorksheet("/Worksheets/folder1/"),
                newWorksheet("/Worksheets/folder1/folder4/"),
                newWorksheet("/Worksheets/folder1/folder4/file5.sql"),
                newWorksheet("/Worksheets/folder1/file2.sql"),
                newWorksheet("/Worksheets/folder3/"),
                newWorksheet("/Worksheets/folder3/file3.sql"),
                newWorksheet("/Worksheets/file1.sql"));
        assertEquals(expectedWorksheets1, currentWorksheet.getSubWorksheetsInDepth(0, true));

        List<Worksheet> expectedWorksheets2 = Arrays.asList(
                newWorksheet("/Worksheets/folder1/"),
                newWorksheet("/Worksheets/folder3/"),
                newWorksheet("/Worksheets/file1.sql"));
        assertEquals(expectedWorksheets2, currentWorksheet.getSubWorksheetsInDepth(1, true));

        List<Worksheet> expectedWorksheets3 = Arrays.asList(
                newWorksheet("/Worksheets/folder1/"),
                newWorksheet("/Worksheets/folder1/folder4/"),
                newWorksheet("/Worksheets/folder1/file2.sql"),
                newWorksheet("/Worksheets/folder3/"),
                newWorksheet("/Worksheets/folder3/file3.sql"),
                newWorksheet("/Worksheets/file1.sql"));
        assertEquals(expectedWorksheets3, currentWorksheet.getSubWorksheetsInDepth(2, true));

        assertEquals(expectedWorksheets1, currentWorksheet.getSubWorksheetsInDepth(3, true));
        assertEquals(expectedWorksheets1, currentWorksheet.getSubWorksheetsInDepth(4, true));
        assertEquals(expectedWorksheets1, currentWorksheet.getSubWorksheetsInDepth(Integer.MAX_VALUE, true));
    }

    @Test
    public void getSubWorksheetsInDepth_NoSubWorksheets() {
        Worksheet emptyWorksheet = newWorksheet("/Worksheets/");
        assertEquals(0, emptyWorksheet.getSubWorksheetsInDepth(0, false).size());
    }

    @Test
    public void getSubWorksheetsInDepth_NegativeDepth() {
        assertThrows(BadArgumentException.class, () -> {
            currentWorksheet.getSubWorksheetsInDepth(-1, false);
        });
    }

    @Test
    public void getSubWorksheetsInDepth_EmptyArgumentDepth() {
        assertThrows(BadArgumentException.class, () -> {
            currentWorksheet.getSubWorksheetsInDepth(null, false);
        });
    }


}
