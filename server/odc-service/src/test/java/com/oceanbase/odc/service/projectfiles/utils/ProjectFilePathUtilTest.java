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
package com.oceanbase.odc.service.projectfiles.utils;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.oceanbase.odc.service.projectfiles.model.ProjectFileLocation;
import com.oceanbase.odc.service.projectfiles.model.ProjectFileType;

@RunWith(Parameterized.class)
public class ProjectFilePathUtilTest {

    private final String path;
    private final List<String> expectedItems;
    private final String expectedStandardPath;
    private final String expectedName;
    private final ProjectFileType expectedType;
    private final ProjectFileLocation expectedLocation;

    public ProjectFilePathUtilTest(String path, List<String> expectedItems, String expectedStandardPath,
            String expectedName, ProjectFileType expectedType, ProjectFileLocation expectedLocation) {
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
                        "file.txt", ProjectFileType.FILE, ProjectFileLocation.REPOS},
                {"Repos/path2/", Arrays.asList("Repos", "path2", "/"), "/Repos/path2/", "path2",
                        ProjectFileType.GIT_REPO, ProjectFileLocation.REPOS},
                {"Worksheets/path2", Arrays.asList("Worksheets", "path2"), "/Worksheets/path2", "path2",
                        ProjectFileType.FILE, ProjectFileLocation.WORKSHEETS},
                {"Worksheets / path2 / file.txt ", Arrays.asList("Worksheets", "path2", "file.txt"),
                        "/Worksheets/path2/file.txt", "file.txt", ProjectFileType.FILE, ProjectFileLocation.WORKSHEETS},
                {"Worksheets \\ path2 \\  ", Arrays.asList("Worksheets", "path2", "/"), "/Worksheets/path2/", "path2",
                        ProjectFileType.DIRECTORY, ProjectFileLocation.WORKSHEETS},
                {"Worksheets /  ", Arrays.asList("Worksheets", "/"), "/Worksheets/", "Worksheets",
                        ProjectFileType.DIRECTORY, ProjectFileLocation.WORKSHEETS},
                {"\\\\Worksheets\\\\ / path2////\\\\ / file.txt ", Arrays.asList("Worksheets", "path2", "file.txt"),
                        "/Worksheets/path2/file.txt", "file.txt", ProjectFileType.FILE, ProjectFileLocation.WORKSHEETS},
                {" /Repos/git \\ ", Arrays.asList("Repos", "git", "/"), "/Repos/git/", "git",
                        ProjectFileType.GIT_REPO, ProjectFileLocation.REPOS},
                {" \\Repos/git / folder1/   ", Arrays.asList("Repos", "git", "folder1", "/"), "/Repos/git/folder1/",
                        "folder1",
                        ProjectFileType.DIRECTORY, ProjectFileLocation.REPOS},
                {" Repos/git \\ file1   ", Arrays.asList("Repos", "git", "file1"), "/Repos/git/file1", "file1",
                        ProjectFileType.FILE, ProjectFileLocation.REPOS},
                {"/Repos/path2/", Arrays.asList("Repos", "path2", "/"), "/Repos/path2/", "path2",
                        ProjectFileType.GIT_REPO, ProjectFileLocation.REPOS},
                {"//   ///  ///Repos/  ///  //path2///  /", Arrays.asList("Repos", "path2", "/"), "/Repos/path2/",
                        "path2",
                        ProjectFileType.GIT_REPO, ProjectFileLocation.REPOS},
                {"//", Arrays.asList("/"), "/", null, null, null},
                {"", null, null, null, null, null},
                {null, null, null, null, null, null},
                {"/folder1/", Arrays.asList("folder1", "/"), "/folder1/", "folder1", null, null},
                {"/Worksheets", Arrays.asList("Worksheets"), "/Worksheets", "Worksheets", null,
                        ProjectFileLocation.WORKSHEETS},
                {"/Repos/git", Arrays.asList("Repos", "git"), "/Repos/git", "git", null, ProjectFileLocation.REPOS},
                {"/Repos/", Arrays.asList("Repos", "/"), "/Repos/", "Repos", null, ProjectFileLocation.REPOS},
                {"path1/path2/", Arrays.asList("path1", "path2", "/"), "/path1/path2/", "path2", null, null},
                {"path1/path2////", Arrays.asList("path1", "path2", "/"), "/path1/path2/", "path2", null, null},
        });
    }

    /**
     * [单元测试]测试splitPathToItems方法
     */
    @Test
    public void testSplitPathToItems() {
        List<String> items = ProjectFilePathUtil.splitPathToItems(path);
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
        Optional<String> standardPathOptional = ProjectFilePathUtil.convertItemsToPath(expectedItems);
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
        Optional<String> nameOptional = ProjectFilePathUtil.getPathName(expectedItems);
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
        Optional<ProjectFileLocation> locationOptional = ProjectFilePathUtil.getPathLocation(expectedItems);
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
        Optional<ProjectFileType> typeOptional = ProjectFilePathUtil.getPathType(expectedItems);
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
        String bucketName = ProjectFilePathUtil.getObjectStorageBucketName(123L);
        assertEquals("PROJECT_FILE_123", bucketName);
    }
}
