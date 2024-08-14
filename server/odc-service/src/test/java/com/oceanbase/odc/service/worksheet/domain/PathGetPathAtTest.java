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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class PathGetPathAtTest {

    private final Path path;
    private final int index;
    private final Optional<Path> expectedResultPath;
    private final Boolean hasException;
    private final boolean expectedIsSystemDefine;

    public PathGetPathAtTest(String pathStr, int index, String expectedResult, Boolean hasException,
            Boolean expectedIsSystemDefine) {
        this.index = index;
        this.path = new Path(pathStr);
        this.expectedResultPath = expectedResult == null ? Optional.empty() : Optional.of(new Path(expectedResult));
        this.hasException = hasException;
        this.expectedIsSystemDefine = expectedIsSystemDefine;
    }

    @Parameters
    public static List<Object[]> data() {
        return Arrays.asList(
                new Object[] {"/Worksheets/folder1/file1", 0, "/Worksheets/", false, true},
                new Object[] {"/Worksheets/folder1/file1", 1, "/Worksheets/folder1/", false, false},
                new Object[] {"/Worksheets/folder1/file1", 2, "/Worksheets/folder1/file1", false, false},
                new Object[] {"/Worksheets/folder1/file1", 3, null, true, false},

                new Object[] {"/Repos/repo/folder1/file1", 0, "/Repos/", false, true},
                new Object[] {"/Repos/repo/folder1/file1", 1, "/Repos/repo/", false, true},
                new Object[] {"/Repos/repo/folder1/file1", 2, "/Repos/repo/folder1/", false, false},
                new Object[] {"/Repos/repo/folder1/file1", 3, "/Repos/repo/folder1/file1", false, false},
                new Object[] {"/Repos/repo/folder1/file1", 4, null, true, false});
    }

    @Test
    public void testGetPathAt() {
        try {
            Optional<Path> result = path.getPathAt(index);
            assertEquals(expectedResultPath, result);
            assert !hasException;
            if (result.isPresent() || expectedIsSystemDefine) {
                assertEquals(expectedIsSystemDefine, result.get().isSystemDefine());
            }
        } catch (Exception e) {
            if (!hasException) {
                throw e;
            }
        }

    }
}
