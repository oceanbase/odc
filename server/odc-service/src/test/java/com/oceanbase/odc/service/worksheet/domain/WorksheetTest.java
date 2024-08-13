/*
 * Copyright (c) 2024 OceanBase.
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

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.oceanbase.odc.service.worksheet.constants.WorksheetConstant;
import com.oceanbase.odc.service.worksheet.exceptions.ChangeTooMuchException;
import com.oceanbase.odc.service.worksheet.exceptions.NameDuplicatedException;

public class WorksheetTest {
    final static Long projectId = 1L;

    private Worksheet newWorksheet(String path) {
        return Worksheet.of(projectId, new Path(path), "objectId", 1L);
    }

    @Test
    public void testRename_Self() {
        Worksheet worksheet = newWorksheet("/Worksheets/file.sql");
        Path destinationPath = new Path("/Worksheets/file1.sql");
        Set<Worksheet> changedSubFiles = worksheet.rename(destinationPath);

        assertEquals(changedSubFiles, new HashSet<>(Arrays.asList(newWorksheet("/Worksheets/file1.sql"))));
        assertEquals(destinationPath, worksheet.getPath());
    }

    @Test
    public void testRename_Sub() {
        Worksheet worksheet = newWorksheet("/Worksheets/folder1/");
        Worksheet sub1 = newWorksheet("/Worksheets/folder1/file1");
        Worksheet sub2 = newWorksheet("/Worksheets/folder1/file2");
        Worksheet sub3 = newWorksheet("/Worksheets/folder1/folder2/file1");
        Worksheet sub4 = newWorksheet("/Worksheets/folder3/");
        worksheet.setSubWorksheets(new HashSet<>(Arrays.asList(sub1, sub2, sub3, sub4)));
        Path destinationPath = new Path("/Worksheets/folder2/");

        Set<Worksheet> changedSubFiles = worksheet.rename(destinationPath);

        assertEquals(changedSubFiles, new HashSet<>(Arrays.asList(
                newWorksheet("/Worksheets/folder2/"),
                newWorksheet("/Worksheets/folder2/file1"),
                newWorksheet("/Worksheets/folder2/file2"),
                newWorksheet("/Worksheets/folder2/folder2/file1"))));
        assertEquals(destinationPath, worksheet.getPath());
    }

    @Test
    public void testRename_DuplicatePath() {
        Worksheet worksheet = newWorksheet("/Worksheets/folder1/");
        Worksheet sameLevel1 = newWorksheet("/Worksheets/folder2/");
        worksheet.setSameLevelWorksheets(new HashSet<>(Arrays.asList(sameLevel1)));
        Path destinationPath = new Path("/Worksheets/folder2/");
        assertThrows(NameDuplicatedException.class, () -> worksheet.rename(destinationPath));


        Worksheet worksheet2 = newWorksheet("/Worksheets/folder1/file1");
        Worksheet sameLevel2 = newWorksheet("/Worksheets/folder1/file2");
        worksheet2.setSameLevelWorksheets(new HashSet<>(Arrays.asList(sameLevel2)));
        Path destinationPath2 = new Path("/Worksheets/folder1/file2");
        assertThrows(NameDuplicatedException.class, () -> worksheet2.rename(destinationPath2));
    }

    @Test
    public void testRename_InvalidPath() {
        Worksheet worksheet = newWorksheet("/Worksheets/folder1/file2");
        Path destinationPath = new Path("/Worksheets/folder2/file3");
        assertThrows(IllegalArgumentException.class, () -> worksheet.rename(destinationPath));
        Worksheet worksheet2 = newWorksheet("/Worksheets/folder1/file2");
        Path destinationPath2 = new Path("/Worksheets/folder1/folder3/");
        assertThrows(IllegalArgumentException.class, () -> worksheet2.rename(destinationPath2));
        Worksheet worksheet3 = newWorksheet("/Worksheets/folder1/file2");
        Path destinationPath3 = new Path("/Worksheets/folder1/");
        assertThrows(IllegalArgumentException.class, () -> worksheet3.rename(destinationPath3));
    }


    @Test
    public void testRename_TooManyChanges() {
        assertEquals(renameWithSubCount(WorksheetConstant.CHANGE_FILE_NUM_LIMIT - 1).size(),
                WorksheetConstant.CHANGE_FILE_NUM_LIMIT);
        assertThrows(ChangeTooMuchException.class, () -> renameWithSubCount(WorksheetConstant.CHANGE_FILE_NUM_LIMIT));
    }

    private Set<Worksheet> renameWithSubCount(int count) {
        Worksheet worksheet = newWorksheet("/Worksheets/folder1/");
        Set<Worksheet> subs = new HashSet<>();
        for (int i = 0; i < count; i++) {
            Worksheet sub = newWorksheet("/Worksheets/folder1/file" + i);
            subs.add(sub);
        }
        worksheet.setSubWorksheets(subs);
        Path destinationPath = new Path("/Worksheets/folder2/");
        return worksheet.rename(destinationPath);
    }
}
