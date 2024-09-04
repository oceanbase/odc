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

import static com.oceanbase.odc.service.worksheet.constants.WorksheetConstants.ROOT_PATH_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.oceanbase.odc.service.worksheet.model.WorksheetLocation;
import com.oceanbase.odc.service.worksheet.model.WorksheetType;

@RunWith(Parameterized.class)
public class PathConstructorTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private final String path;
    private final WorksheetLocation expectedLocation;
    private final WorksheetType expectedType;
    private final String expectedName;
    private final Integer expectedLevelNum;
    private final String expectedStandardPath;
    private final List<String> expectedPathItems;


    public PathConstructorTest(String path, WorksheetLocation expectedLocation, WorksheetType expectedType,
            String expectedName,
            Integer expectedLevelNum, String expectedStandardPath, List<String> expectedPathItems) {
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
                {"Worksheets / path2 / file.txt ", WorksheetLocation.WORKSHEETS,
                        WorksheetType.FILE, "file.txt", 3,
                        "/Worksheets/path2/file.txt", Arrays.asList("Worksheets", "path2")},
                {"Worksheets \\ path2 \\  ", WorksheetLocation.WORKSHEETS,
                        WorksheetType.DIRECTORY, "path2", 2,
                        "/Worksheets/path2/", Arrays.asList("Worksheets")},
                {"Worksheets /  ", WorksheetLocation.WORKSHEETS,
                        WorksheetType.WORKSHEETS, "Worksheets", 1,
                        "/Worksheets/", Arrays.asList()},
                {"Worksheets / path2 / file.txt ", WorksheetLocation.WORKSHEETS,
                        WorksheetType.FILE, "file.txt", 3,
                        "/Worksheets/path2/file.txt", Arrays.asList("Worksheets", "path2")},
                {" /Repos/git \\ ", WorksheetLocation.REPOS,
                        WorksheetType.GIT_REPO, "git", 2, "/Repos/git/", Arrays.asList("Repos")},
                {" \\Repos/git / folder1/   ", WorksheetLocation.REPOS,
                        WorksheetType.DIRECTORY, "folder1", 3,
                        "/Repos/git/folder1/", Arrays.asList("Repos", "git")},
                {" Repos/git \\ file1   ", WorksheetLocation.REPOS,
                        WorksheetType.FILE, "file1", 3,
                        "/Repos/git/file1", Arrays.asList("Repos", "git")},
                {"/Repos/path2/", WorksheetLocation.REPOS,
                        WorksheetType.GIT_REPO, "path2", 2, "/Repos/path2/", Arrays.asList("Repos")},
                {"//   ///  ///Repos/  ///  //path2///  /",
                        WorksheetLocation.REPOS, WorksheetType.GIT_REPO, "path2", 2,
                        "/Repos/path2/", Arrays.asList("Repos")},
                {"//", WorksheetLocation.ROOT, WorksheetType.ROOT,
                        ROOT_PATH_NAME, 0, "/", Arrays.asList()},
                {"", null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {"/folder1/", null, null, null, null, null, null},
                {"/Worksheets", null, null, null, null, null, null},
                {"/Repos/git", null, null, null, null, null, null},
                {"/Repos/", WorksheetLocation.REPOS, WorksheetType.REPOS, "Repos",
                        1, "/Repos/", Arrays.asList()},
        });
    }

    @Before
    public void setUp() {
        if (expectedLocation == null) {
            thrown.expect(IllegalArgumentException.class);
        }
    }

    @Test
    public void constructor() {
        Path pathObject = new Path(path);
        assertNotNull(pathObject);
        assertEquals(expectedLocation, pathObject.getLocation());
        assertEquals(expectedType, pathObject.getType());
        assertEquals(expectedName, pathObject.getName());
        assertEquals(expectedLevelNum, pathObject.getLevelNum());
        assertEquals(expectedPathItems, pathObject.getParentPathItems());
    }

    @Test
    public void getStandardPath() {
        Path pathObject = new Path(path);
        assertNotNull(pathObject);
        assertEquals(expectedStandardPath, pathObject.getStandardPath());
    }
}
