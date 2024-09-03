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
import static org.junit.Assert.assertFalse;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.oceanbase.odc.service.worksheet.model.WorksheetLocation;
import com.oceanbase.odc.service.worksheet.model.WorksheetType;

@RunWith(Parameterized.class)
public class PathGetParentPathTest {
    private final String path;
    private final WorksheetLocation expectedLocation;
    private final WorksheetType expectedType;
    private final String expectedName;
    private final Integer expectedLevelNum;
    private final List<String> expectedPathItems;


    public PathGetParentPathTest(

            String path, WorksheetLocation expectedLocation, WorksheetType expectedType, String expectedName,
            Integer expectedLevelNum, List<String> expectedPathItems) {


        this.path = path;
        this.expectedLocation = expectedLocation;
        this.expectedType = expectedType;
        this.expectedName = expectedName;
        this.expectedLevelNum = expectedLevelNum;
        this.expectedPathItems = expectedPathItems;
    }

    @Parameters
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {"Worksheets / path2 / file.txt ", WorksheetLocation.WORKSHEETS,
                        WorksheetType.DIRECTORY, "path2", 2, Arrays.asList("Worksheets")},
                {"Worksheets \\ path2 \\  ", WorksheetLocation.WORKSHEETS,
                        WorksheetType.WORKSHEETS, "Worksheets", 1, Arrays.asList()},
                {"Worksheets /  ", WorksheetLocation.ROOT,
                        WorksheetType.ROOT, ROOT_PATH_NAME, 0, Arrays.asList()},
                {" /Repos/git \\ ", WorksheetLocation.REPOS,
                        WorksheetType.REPOS, "Repos", 1, Arrays.asList()},
                {" \\Repos/git / folder1/   ", WorksheetLocation.REPOS,
                        WorksheetType.GIT_REPO, "git", 2, Arrays.asList("Repos")},
                {" \\Repos/git / folder1/file1   ", WorksheetLocation.REPOS,
                        WorksheetType.DIRECTORY, "folder1", 3, Arrays.asList("Repos", "git")},
        });
    }

    @Test
    public void getParentPath() {
        Optional<Path> parentPathOptional = new Path(path).getParentPath();
        if (expectedLocation == null) {
            assertFalse(parentPathOptional.isPresent());
            return;
        }
        Path pathObject = parentPathOptional.get();
        assertEquals(expectedLocation, pathObject.getLocation());
        assertEquals(expectedType, pathObject.getType());
        assertEquals(expectedName, pathObject.getName());
        assertEquals(expectedLevelNum, pathObject.getLevelNum());
        assertEquals(expectedPathItems, pathObject.getParentPathItems());
    }

}
