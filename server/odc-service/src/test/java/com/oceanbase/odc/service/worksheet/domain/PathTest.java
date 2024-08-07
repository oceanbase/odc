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
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.oceanbase.odc.service.worksheet.model.WorksheetLocation;
import com.oceanbase.odc.service.worksheet.model.WorksheetType;

@RunWith(Parameterized.class)
public class PathTest {
    private final String method;

    private final String path1;
    private final String path2;
    private final Integer expectedResult;

    private final String path;
    private final WorksheetLocation expectedLocation;
    private final WorksheetType expectedType;
    private final String expectedName;
    private final Integer expectedLevelNum;
    private final String expectedStandardPath;
    private final List<String> expectedPathItems;


    public PathTest(String method, String path1, String path2, Integer expectedResult,

            String path, WorksheetLocation expectedLocation, WorksheetType expectedType, String expectedName,
            Integer expectedLevelNum, String expectedStandardPath, List<String> expectedPathItems) {
        this.method = method;

        this.path1 = path1;
        this.path2 = path2;
        this.expectedResult = expectedResult;

        this.path = path;
        this.expectedLocation = expectedLocation;
        this.expectedType = expectedType;
        this.expectedName = expectedName;
        this.expectedLevelNum = expectedLevelNum;
        this.expectedStandardPath = expectedStandardPath;
        this.expectedPathItems = expectedPathItems;
    }

    @Parameters
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {
                // 测试场景：类型相同，名称相同
                {"testGetPathSameLevelComparator", "/Worksheets/folder1/", "/Worksheets/folder1/", 0, null, null, null,
                        null, null, null, null},
                {"testGetPathSameLevelComparator", "/Repos/git1/file2", "/Repos/git1/file2", 0, null, null, null, null,
                        null, null, null},
                // 测试场景：类型相同，名称不同
                {"testGetPathSameLevelComparator", "/Worksheets/folder1/", "/Worksheets/folder2/", -1, null, null, null,
                        null, null, null, null},
                {"testGetPathSameLevelComparator", "/Worksheets/file1", "/Worksheets/file2", -1, null, null, null, null,
                        null, null, null},
                {"testGetPathSameLevelComparator", "/Repos/git1/file2", "/Repos/git1/file1", 1, null, null, null, null,
                        null, null, null},
                {"testGetPathSameLevelComparator", "/Repos/git1/", "/Repos/git2/", -1, null, null, null, null, null,
                        null, null},
                // 测试场景：类型不同
                // File<Directory
                {"testGetPathSameLevelComparator", "/Worksheets/folder1", "/Worksheets/folder1/", -1, null, null, null,
                        null, null, null, null},
                // Git_Repo>Directory
                {"testGetPathSameLevelComparator", "/Repos/git1/", "/Worksheets/git1/", 1, null, null, null, null, null,
                        null, null},
                // Git_Repo>File
                {"testGetPathSameLevelComparator", "/Repos/git1/", "/Worksheets/file1", 1, null, null, null, null, null,
                        null, null},


                {"testConstructor", null, null, null, "Worksheets / path2 / file.txt ", WorksheetLocation.WORKSHEETS,
                        WorksheetType.FILE, "file.txt", 3,
                        "/Worksheets/path2/file.txt", Arrays.asList("Worksheets", "path2")},
                {"testConstructor", null, null, null, "Worksheets \\ path2 \\  ", WorksheetLocation.WORKSHEETS,
                        WorksheetType.DIRECTORY, "path2", 2,
                        "/Worksheets/path2/", Arrays.asList("Worksheets")},
                {"testConstructor", null, null, null, "Worksheets /  ", WorksheetLocation.WORKSHEETS,
                        WorksheetType.DIRECTORY, "Worksheets", 1,
                        "/Worksheets/", Arrays.asList()},
                {"testConstructor", null, null, null, "Worksheets / path2 / file.txt ", WorksheetLocation.WORKSHEETS,
                        WorksheetType.FILE, "file.txt", 3,
                        "/Worksheets/path2/file.txt", Arrays.asList("Worksheets", "path2")},
                {"testConstructor", null, null, null, " /Repos/git \\ ", WorksheetLocation.REPOS,
                        WorksheetType.GIT_REPO, "git", 2, "/Repos/git/", Arrays.asList("Repos")},
                {"testConstructor", null, null, null, " \\Repos/git / folder1/   ", WorksheetLocation.REPOS,
                        WorksheetType.DIRECTORY, "folder1", 3,
                        "/Repos/git/folder1/", Arrays.asList("Repos", "git")},
                {"testConstructor", null, null, null, " Repos/git \\ file1   ", WorksheetLocation.REPOS,
                        WorksheetType.FILE, "file1", 3,
                        "/Repos/git/file1", Arrays.asList("Repos", "git")},
                {"testConstructor", null, null, null, "/Repos/path2/", WorksheetLocation.REPOS,
                        WorksheetType.GIT_REPO, "path2", 2, "/Repos/path2/", Arrays.asList("Repos")},
                {"testConstructor", null, null, null, "//   ///  ///Repos/  ///  //path2///  /",
                        WorksheetLocation.REPOS, WorksheetType.GIT_REPO, "path2", 2,
                        "/Repos/path2/", Arrays.asList("Repos")},
                {"testConstructor", null, null, null, "//", null, null, null, null, null, null},
                {"testConstructor", null, null, null, "", null, null, null, null, null, null},
                {"testConstructor", null, null, null, null, null, null, null, null, null, null},
                {"testConstructor", null, null, null, "/folder1/", null, null, null, null, null, null},
                {"testConstructor", null, null, null, "/Worksheets", null, null, null, null, null, null},
                {"testConstructor", null, null, null, "/Repos/git", null, null, null, null, null, null},
                {"testConstructor", null, null, null, "/Repos/", null, null, null, null, null, null},

                {"testGetParentPath", null, null, null, "Worksheets / path2 / file.txt ",
                        WorksheetLocation.WORKSHEETS,
                        WorksheetType.DIRECTORY, "path2", 2,
                        null, Arrays.asList("Worksheets")},
                {"testGetParentPath", null, null, null, "Worksheets \\ path2 \\  ", WorksheetLocation.WORKSHEETS,
                        WorksheetType.DIRECTORY, "Worksheets", 1,
                        null, Arrays.asList()},
                {"testGetParentPath", null, null, null, "Worksheets /  ", null, null, null, null, null, null},
                {"testGetParentPath", null, null, null, " /Repos/git \\ ", null, null, null, null, null, null},
                {"testGetParentPath", null, null, null, " \\Repos/git / folder1/   ", WorksheetLocation.REPOS,
                        WorksheetType.GIT_REPO, "git", 2,
                        null, Arrays.asList("Repos")},
                {"testGetParentPath", null, null, null, " \\Repos/git / folder1/file1   ", WorksheetLocation.REPOS,
                        WorksheetType.DIRECTORY, "folder1", 3,
                        null, Arrays.asList("Repos", "git")},
        });
    }

    /**
     * [单元测试]测试getPathSameLevelComparator方法
     */
    @Test
    public void testGetPathSameLevelComparator() {
        if (!StringUtils.equals(method, "testGetPathSameLevelComparator")) {
            return;
        }
        Comparator<Path> comparator = Path.getPathSameLevelComparator();
        Path o1 = new Path(path1);
        Path o2 = new Path(path2);
        Integer result = comparator.compare(o1, o2);
        assertEquals(expectedResult, result);
    }



    /**
     * [单元测试]测试构造函数
     */
    @Test
    public void testConstructor() {
        if (!StringUtils.equals(method, "testConstructor")) {
            return;
        }
        try {

            Path pathObject = new Path(path);
            assertNotNull(pathObject);
            assertEquals(expectedLocation, pathObject.getLocation());
            assertEquals(expectedType, pathObject.getType());
            assertEquals(expectedName, pathObject.getName());
            assertEquals(expectedLevelNum, pathObject.getLevelNum());
            assertEquals(expectedPathItems, pathObject.getParentPathItems());
        } catch (IllegalArgumentException e) {
            assert expectedLocation == null;
        }
    }

    @Test
    public void testGetParentPath() {
        if (!StringUtils.equals(method, "testGetParentPath")) {
            return;
        }
        Optional<Path> parentPathOptional = new Path(path).getParentPath();
        if (parentPathOptional.isPresent()) {
            Path pathObject = parentPathOptional.get();
            assertEquals(expectedLocation, pathObject.getLocation());
            assertEquals(expectedType, pathObject.getType());
            assertEquals(expectedName, pathObject.getName());
            assertEquals(expectedLevelNum, pathObject.getLevelNum());
            assertEquals(expectedPathItems, pathObject.getParentPathItems());
        } else {
            assert expectedLocation == null;
        }
    }

    @Test
    public void testGetStandardPath() {
        if (!StringUtils.equals(method, "testConstructor")) {
            return;
        }
        try {
            Path pathObject = new Path(path);
            assertNotNull(pathObject);
            assertEquals(expectedStandardPath, pathObject.getStandardPath());
        } catch (IllegalArgumentException e) {
            assert expectedLocation == null;
        }
    }
}
