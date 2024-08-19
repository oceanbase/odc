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

import static com.oceanbase.odc.service.worksheet.constants.WorksheetConstant.NAME_LENGTH_LIMIT;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.oceanbase.odc.service.worksheet.constants.WorksheetConstant;
import com.oceanbase.odc.service.worksheet.exceptions.ChangeTooMuchException;
import com.oceanbase.odc.service.worksheet.exceptions.EditVersionConflictException;
import com.oceanbase.odc.service.worksheet.exceptions.ExceedSameLevelNumLimitException;
import com.oceanbase.odc.service.worksheet.exceptions.NameDuplicatedException;
import com.oceanbase.odc.service.worksheet.exceptions.NameTooLongException;

public class WorksheetTest {
    final static Long projectId = 1L;
    long id = 0L;
    Long defaultVersion = 1L;

    private Worksheet newWorksheet(String path) {
        return newWorksheet(projectId, new Path(path), "objectId", 1L);
    }

    public Worksheet newWorksheet(Long projectId, Path path, String objectId, Long creatorId) {
        return new Worksheet(id++, new Date(), new Date(), projectId, path, creatorId,
                defaultVersion, objectId, null, null);
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
        Worksheet sameWorksheet1 = newWorksheet("/Worksheets/folder2/");
        worksheet.setSameParentAtPrevLevelWorksheets(new HashSet<>(Arrays.asList(sameWorksheet1)));
        Path destinationPath = new Path("/Worksheets/folder2/");
        assertThrows(NameDuplicatedException.class, () -> worksheet.rename(destinationPath));


        Worksheet worksheet2 = newWorksheet("/Worksheets/folder1/file1");
        Worksheet sameWorksheet2 = newWorksheet("/Worksheets/folder1/file2");
        worksheet2.setSameParentAtPrevLevelWorksheets(new HashSet<>(Arrays.asList(sameWorksheet2)));
        Path destinationPath2 = new Path("/Worksheets/folder1/file2");
        assertThrows(NameDuplicatedException.class, () -> worksheet2.rename(destinationPath2));

        Worksheet worksheet3 = newWorksheet("/Worksheets/folder1/");
        Worksheet sameWorksheet3 = newWorksheet("/Worksheets/folder2/file2");
        worksheet3.setSameParentAtPrevLevelWorksheets(new HashSet<>(Arrays.asList(sameWorksheet3)));
        Path destinationPath3 = new Path("/Worksheets/folder2/");
        assertThrows(NameDuplicatedException.class, () -> worksheet3.rename(destinationPath3));

        Worksheet worksheet4 = newWorksheet("/Worksheets/folder1/");
        Worksheet sameWorksheet4 = newWorksheet("/Worksheets/folder2");
        worksheet4.setSameParentAtPrevLevelWorksheets(new HashSet<>(Arrays.asList(sameWorksheet4)));
        Path destinationPath4 = new Path("/Worksheets/folder2/");
        assertThrows(NameDuplicatedException.class, () -> worksheet3.rename(destinationPath4));
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

    @Test
    public void testBatchCreate_Single() {
        Worksheet worksheet = newWorksheet(projectId,
                Path.ofDirectory("Worksheets", "folder1"), id++ + "", id++);
        Map<Path, String> createPathToObjectIdMap = new HashMap<>();
        Path createPath = Path.ofFile("Worksheets", "folder1", "file2.sql");
        createPathToObjectIdMap.put(createPath, "objectId2");

        Set<Worksheet> result = worksheet.batchCreate(createPathToObjectIdMap, 1001L);

        assertEquals(1, result.size());
        Worksheet createWorksheet =
                result.stream().findFirst().orElse(null);
        assertNotNull(createWorksheet);
        assertEquals(createPath, createWorksheet.getPath());
        assertEquals(projectId, createWorksheet.getProjectId());
        assertEquals(1001L, createWorksheet.getCreatorId().longValue());
        assertEquals("objectId2", createWorksheet.getObjectId());
    }

    @Test
    public void testBatchCreate_Multiple() {
        Worksheet worksheet =
                newWorksheet(projectId,
                        Path.ofDirectory("Worksheets", "folder1"), id++ + "", id++);
        Set<Worksheet> subWorksheets = new HashSet<>();
        subWorksheets.add(newWorksheet(projectId,
                Path.ofFile("Worksheets", "folder1", "folder5", "file1"), id++ + "", id++));
        subWorksheets.add(newWorksheet(projectId,
                Path.ofFile("Worksheets", "folder1", "file1"), id++ + "", id++));
        subWorksheets.add(newWorksheet(projectId,
                Path.ofFile("Worksheets", "folder1", "file4.sql"), id++ + "", id++));
        worksheet.setSubWorksheets(subWorksheets);

        Map<Path, String> createPathToObjectIdMap = new HashMap<>();
        createPathToObjectIdMap.put(Path.ofFile("Worksheets", "folder1", "file2.sql"), "objectId2");
        createPathToObjectIdMap.put(Path.ofDirectory("Worksheets", "folder1", "folder4"), "objectId3");
        // sub worksheet exist,but can to be created
        createPathToObjectIdMap.put(Path.ofDirectory("Worksheets", "folder1", "folder5"), "objectId4");

        Set<Worksheet> result = worksheet.batchCreate(createPathToObjectIdMap, 1001L);
        assertEquals(3, result.size());
        for (Worksheet rw : result) {
            String objectId =
                    createPathToObjectIdMap.get(rw.getPath());
            assertNotNull(objectId);
            assertEquals(projectId, rw.getProjectId());
            assertEquals(1001L, rw.getCreatorId().longValue());
            assertEquals(objectId, rw.getObjectId());
        }
    }

    @Test
    public void testBatchCreate_ExceedLimit() {
        Worksheet worksheet =
                newWorksheet(projectId,
                        Path.ofDirectory("Worksheets", "folder1"), id++ + "", id++);
        Set<Worksheet> subWorksheets = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            subWorksheets.add(newWorksheet(projectId,
                    Path.ofDirectory("Worksheets", "folder1", "file" + i + ".sql"), id++ + "", id++));
        }
        worksheet.setSubWorksheets(subWorksheets);
        Map<Path, String> createPathToObjectIdMap = new HashMap<>();
        for (int i = 10; i < WorksheetConstant.SAME_LEVEL_NUM_LIMIT; i++) {
            createPathToObjectIdMap.put(Path.ofFile("Worksheets", "folder1", "file" + i + ".sql"), "objectId" + i);
        }
        Set<Worksheet> result = worksheet.batchCreate(createPathToObjectIdMap, 1001L);
        assertEquals(WorksheetConstant.SAME_LEVEL_NUM_LIMIT - 10, result.size());
        for (Worksheet rw : result) {
            String objectId =
                    createPathToObjectIdMap.get(rw.getPath());
            assertNotNull(objectId);
            assertEquals(projectId, rw.getProjectId());
            assertEquals(1001L, rw.getCreatorId().longValue());
            assertEquals(objectId, rw.getObjectId());
        }
        createPathToObjectIdMap.put(
                Path.ofFile("Worksheets", "folder1", "file" + WorksheetConstant.SAME_LEVEL_NUM_LIMIT + ".sql"),
                "objectId" + WorksheetConstant.SAME_LEVEL_NUM_LIMIT);
        assertThrows(ExceedSameLevelNumLimitException.class,
                () -> worksheet.batchCreate(createPathToObjectIdMap, 1001L));
    }

    @Test
    public void testBatchCreate_DuplicateName() {
        Worksheet worksheet =
                newWorksheet(projectId,
                        Path.ofDirectory("Worksheets", "folder1"), id++ + "", id++);
        Set<Worksheet> subWorksheets = new HashSet<>();
        subWorksheets.add(newWorksheet(projectId,
                Path.ofFile("Worksheets", "folder1", "folder3", "file1"), id++ + "", id++));
        subWorksheets.add(newWorksheet(projectId,
                Path.ofDirectory("Worksheets", "folder1", "folder2"), id++ + "", id++));
        subWorksheets.add(newWorksheet(projectId,
                Path.ofFile("Worksheets", "folder1", "file4.sql"), id++ + "", id++));
        worksheet.setSubWorksheets(subWorksheets);

        Map<Path, String> createPathToObjectIdMap = new HashMap<>();
        createPathToObjectIdMap.put(Path.ofDirectory("Worksheets", "folder1", "folder2"), id++ + "");
        assertThrows(NameDuplicatedException.class,
                () -> worksheet.batchCreate(createPathToObjectIdMap, 1001L));

        Map<Path, String> createPathToObjectIdMap2 = new HashMap<>();
        createPathToObjectIdMap2.put(Path.ofFile("Worksheets", "folder1", "file4.sql"), id++ + "");
        assertThrows(NameDuplicatedException.class,
                () -> worksheet.batchCreate(createPathToObjectIdMap2, 1001L));

        Map<Path, String> createPathToObjectIdMap3 = new HashMap<>();
        createPathToObjectIdMap3.put(Path.ofFile("Worksheets", "folder1", "folder3"), id++ + "");
        assertThrows(NameDuplicatedException.class,
                () -> worksheet.batchCreate(createPathToObjectIdMap3, 1001L));
    }

    @Test
    public void testBatchCreateNameTooLong() {
        Worksheet worksheet =
                newWorksheet(projectId,
                        Path.ofDirectory("Worksheets", "folder1"), id++ + "", id++);
        StringBuilder exceedNameBuilder = new StringBuilder();
        for (int i = 0; i < NAME_LENGTH_LIMIT; i++) {
            exceedNameBuilder.append("a");
        }
        Map<Path, String> createPathToObjectIdMap = new HashMap<>();
        createPathToObjectIdMap.put(Path.ofFile("Worksheets", "folder1", exceedNameBuilder.toString()),
                "objectId2");
        Set<Worksheet> worksheets = worksheet.batchCreate(createPathToObjectIdMap, 1001L);
        assert worksheets.size() == 1;

        exceedNameBuilder.append("a");
        createPathToObjectIdMap.put(Path.ofFile("Worksheets", "folder1", exceedNameBuilder.toString()),
                "objectId2");
        assertThrows(NameTooLongException.class,
                () -> worksheet.batchCreate(createPathToObjectIdMap, 1001L));
    }


    @Test
    public void testEdit_Normal() {
        Worksheet worksheet = newWorksheet("/Worksheets/folder1/file1.sql");

        // no change
        String originalObjectId = worksheet.getObjectId();
        Set<Worksheet> editedWorksheets =
                worksheet.edit(worksheet.getPath(), originalObjectId, worksheet.getVersion());
        assertEquals(0, editedWorksheets.size());
        assertEquals(originalObjectId, worksheet.getObjectId());
        assertEquals(null, worksheet.getReadVersion());
        assertEquals(defaultVersion, worksheet.getVersion());
        assertFalse(worksheet.isChanged());

        // only content,not change name
        String newObjectId = "newObjectId" + id++;
        editedWorksheets =
                worksheet.edit(worksheet.getPath(), newObjectId, worksheet.getVersion());
        assertEquals(1, editedWorksheets.size());
        assertEquals(newObjectId, worksheet.getObjectId());
        assertEquals(defaultVersion, worksheet.getReadVersion());
        assertEquals(new Long(defaultVersion + 1L), worksheet.getVersion());
        assertTrue(worksheet.isChanged());

        // change content,and name
        newObjectId = "newObjectId" + id++;
        Path newPath = new Path("/Worksheets/folder1/file2.sql");
        editedWorksheets =
                worksheet.edit(newPath, newObjectId, worksheet.getVersion());
        assertEquals(1, editedWorksheets.size());
        assertEquals(newPath, worksheet.getPath());
        assertEquals(newObjectId, worksheet.getObjectId());
        assertEquals(new Long(defaultVersion + 1L), worksheet.getReadVersion());
        assertEquals(new Long(defaultVersion + 2L), worksheet.getVersion());
        assertTrue(worksheet.isChanged());
    }

    @Test
    public void testEdit_VersionConflict() {
        Worksheet worksheet = newWorksheet("/Worksheets/file.sql");
        worksheet.setVersion(2L);
        Path destinationPath = new Path("/Worksheets/file.sql");
        String objectId = "newObjectId";
        Long readVersion = 1L;
        assertThrows(EditVersionConflictException.class, () -> worksheet.edit(destinationPath, objectId, readVersion));
    }
}
