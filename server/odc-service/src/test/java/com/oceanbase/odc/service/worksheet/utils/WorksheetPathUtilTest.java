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
package com.oceanbase.odc.service.worksheet.utils;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.oceanbase.odc.service.worksheet.model.WorksheetLocation;
import com.oceanbase.odc.service.worksheet.model.WorksheetType;

@RunWith(Parameterized.class)
public class WorksheetPathUtilTest {

    private final String path;
    private final List<String> expectedItems;
    private final String expectedStandardPath;
    private final String expectedName;
    private final WorksheetType expectedType;
    private final WorksheetLocation expectedLocation;

    public WorksheetPathUtilTest(String path, List<String> expectedItems, String expectedStandardPath,
            String expectedName, WorksheetType expectedType, WorksheetLocation expectedLocation) {
        this.path = path;
        this.expectedItems = expectedItems;
        this.expectedStandardPath = expectedStandardPath;
        this.expectedName = expectedName;
        this.expectedType = expectedType;
        this.expectedLocation = expectedLocation;
    }

    @Parameters
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {"Repos/path2/file.txt", Arrays.asList("Repos", "path2", "file.txt"), "/Repos/path2/file.txt",
                        "file.txt", WorksheetType.FILE, WorksheetLocation.REPOS},
                {"Repos/path2/", Arrays.asList("Repos", "path2", "/"), "/Repos/path2/", "path2",
                        WorksheetType.GIT_REPO, WorksheetLocation.REPOS},
                {"Worksheets/path2", Arrays.asList("Worksheets", "path2"), "/Worksheets/path2", "path2",
                        WorksheetType.FILE, WorksheetLocation.WORKSHEETS},
                {"Worksheets / path2 / file.txt ", Arrays.asList("Worksheets", "path2", "file.txt"),
                        "/Worksheets/path2/file.txt", "file.txt", WorksheetType.FILE, WorksheetLocation.WORKSHEETS},
                {"Worksheets \\ path2 \\  ", Arrays.asList("Worksheets", "path2", "/"), "/Worksheets/path2/", "path2",
                        WorksheetType.DIRECTORY, WorksheetLocation.WORKSHEETS},
                {"Worksheets /  ", Arrays.asList("Worksheets", "/"), "/Worksheets/", "Worksheets",
                        WorksheetType.DIRECTORY, WorksheetLocation.WORKSHEETS},
                {"\\\\Worksheets\\\\ / path2////\\\\ / file.txt ", Arrays.asList("Worksheets", "path2", "file.txt"),
                        "/Worksheets/path2/file.txt", "file.txt", WorksheetType.FILE, WorksheetLocation.WORKSHEETS},
                {" /Repos/git \\ ", Arrays.asList("Repos", "git", "/"), "/Repos/git/", "git",
                        WorksheetType.GIT_REPO, WorksheetLocation.REPOS},
                {" \\Repos/git / folder1/   ", Arrays.asList("Repos", "git", "folder1", "/"), "/Repos/git/folder1/",
                        "folder1",
                        WorksheetType.DIRECTORY, WorksheetLocation.REPOS},
                {" Repos/git \\ file1   ", Arrays.asList("Repos", "git", "file1"), "/Repos/git/file1", "file1",
                        WorksheetType.FILE, WorksheetLocation.REPOS},
                {"/Repos/path2/", Arrays.asList("Repos", "path2", "/"), "/Repos/path2/", "path2",
                        WorksheetType.GIT_REPO, WorksheetLocation.REPOS},
                {"//   ///  ///Repos/  ///  //path2///  /", Arrays.asList("Repos", "path2", "/"), "/Repos/path2/",
                        "path2",
                        WorksheetType.GIT_REPO, WorksheetLocation.REPOS},
                {"//", Arrays.asList("/"), "/", null, null, null},
                {"", null, null, null, null, null},
                {null, null, null, null, null, null},
                {"/folder1/", Arrays.asList("folder1", "/"), "/folder1/", "folder1", null, null},
                {"/Worksheets", Arrays.asList("Worksheets"), "/Worksheets", "Worksheets", null,
                        WorksheetLocation.WORKSHEETS},
                {"/Repos/git", Arrays.asList("Repos", "git"), "/Repos/git", "git", null, WorksheetLocation.REPOS},
                {"/Repos/", Arrays.asList("Repos", "/"), "/Repos/", "Repos", null, WorksheetLocation.REPOS},
                {"path1/path2/", Arrays.asList("path1", "path2", "/"), "/path1/path2/", "path2", null, null},
                {"path1/path2////", Arrays.asList("path1", "path2", "/"), "/path1/path2/", "path2", null, null},
        });
    }

    /**
     * [单元测试]测试splitPathToItems方法
     */
    @Test
    public void testSplitPathToItems() {
        List<String> items = WorksheetPathUtil.splitPathToItems(path);
        if (CollectionUtils.isEmpty(items)) {
            assert CollectionUtils.isEmpty(expectedItems);
        } else {
            assertEquals(expectedItems, items);
        }
    }

    /**
     * [单元测试]测试convertItemsToPath方法
     */
    @Test
    public void testConvertItemsToPath() {
        Optional<String> standardPathOptional = WorksheetPathUtil.convertItemsToPath(expectedItems);
        if (!standardPathOptional.isPresent()) {
            assert expectedStandardPath == null;
        } else {
            assertEquals(expectedStandardPath, standardPathOptional.get());
        }
    }

    /**
     * [单元测试]测试getPathName方法
     */
    @Test
    public void testGetPathName() {
        Optional<String> nameOptional = WorksheetPathUtil.getPathName(expectedItems);
        if (!nameOptional.isPresent()) {
            assert expectedName == null;
        } else {
            assertEquals(expectedName, nameOptional.get());
        }
    }

    /**
     * [单元测试]测试getPathLocation方法
     */
    @Test
    public void testGetPathLocation() {
        Optional<WorksheetLocation> locationOptional = WorksheetPathUtil.getPathLocation(expectedItems);
        if (!locationOptional.isPresent()) {
            assert expectedLocation == null;
        } else {
            assertEquals(expectedLocation, locationOptional.get());
        }
    }

    /**
     * [单元测试]测试getPathType方法
     */
    @Test
    public void testGetPathType() {
        Optional<WorksheetType> typeOptional = WorksheetPathUtil.getPathType(expectedItems);
        if (!typeOptional.isPresent()) {
            assert expectedType == null;
        } else {
            assertEquals(expectedType, typeOptional.get());
        }
    }

    /**
     * [单元测试]测试getObjectStorageBucketName方法
     */
    @Test
    public void testGetObjectStorageBucketName() {
        String bucketName = WorksheetPathUtil.getObjectStorageBucketName(123L);
        assertEquals("PROJECT_FILE_123", bucketName);
    }
}
