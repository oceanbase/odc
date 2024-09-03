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
import java.util.Comparator;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class PathGetPathComparatorTest {
    private final String path1;
    private final String path2;
    private final Integer expectedResult;

    public PathGetPathComparatorTest(String path1, String path2, Integer expectedResult) {
        this.path1 = path1;
        this.path2 = path2;
        this.expectedResult = expectedResult;
    }

    @Parameters
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {"/Worksheets/folder1/", "/Worksheets/folder1/", 0},
                {"/Repos/git1/file2", "/Repos/git1/file2", 0},
                {"/Worksheets/folder1/", "/Worksheets/folder2/", -1},
                {"/Worksheets/file1", "/Worksheets/file2", -1},
                {"/Repos/git1/file2", "/Repos/git1/file1", 1},
                {"/Repos/git1/", "/Repos/git2/", -1},
                // Directory before file
                {"/Worksheets/folder1", "/Worksheets/folder1/", 1},
                {"/Repos/git1/file1", "/Repos/git1/folder1/", 1},
                // Worksheets before Repos
                {"/Worksheets/", "/Repos/", -1},
                // pre parent not equal
                {"/Worksheets/file1", "/Worksheets/folder1/folder2/", 1},
                {"/Worksheets/folder1/file1", "/Worksheets/folder2/folder2/", -1},
                // one is parent
                {"/Worksheets/folder1/folder4/", "/Worksheets/folder1/folder4/file5.sql/", -1},
        });
    }

    @Test
    public void getPathComparator() {
        Comparator<Path> comparator = Path.getPathComparator();
        Path o1 = new Path(path1);
        Path o2 = new Path(path2);
        Integer result = comparator.compare(o1, o2);
        assertEquals(expectedResult, result);
    }
}
