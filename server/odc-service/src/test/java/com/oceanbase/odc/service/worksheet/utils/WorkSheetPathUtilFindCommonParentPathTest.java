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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.oceanbase.odc.service.worksheet.domain.Path;

@RunWith(Parameterized.class)
public class WorkSheetPathUtilFindCommonParentPathTest {

    private final Set<Path> paths;
    private final Path expectedResult;

    public WorkSheetPathUtilFindCommonParentPathTest(Set<Path> paths, Path expectedResult) {
        this.paths = paths;
        this.expectedResult = expectedResult;
    }

    @Parameters
    public static List<Object[]> data() {
        return Arrays.asList(
                // Empty paths
                new Object[] {Collections.emptySet(), null},

                // Single path
                new Object[] {Collections.singleton(new Path("/Worksheets/Sheet1")),
                        new Path("/Worksheets/Sheet1")},

                // Multiple paths with common parent
                new Object[] {
                        new HashSet<>(Arrays.asList(
                                new Path("/Worksheets/Sheet1/SubSheet1"),
                                new Path("/Worksheets/Sheet1/SubSheet2"))),
                        new Path("/Worksheets/Sheet1/")
                },
                // One is parent
                new Object[] {
                        new HashSet<>(Arrays.asList(
                                new Path("/Worksheets/Sheet1/SubSheet1/"),
                                new Path("/Worksheets/Sheet1/SubSheet1/file1"))),
                        new Path("/Worksheets/Sheet1/SubSheet1/")
                },
                new Object[] {
                        new HashSet<>(Arrays.asList(
                                new Path("/Worksheets/Sheet1/SubSheet1"),
                                new Path("/Worksheets/Sheet2/SubSheet2"))),
                        new Path("/Worksheets/")
                },
                new Object[] {
                        new HashSet<>(Arrays.asList(
                                new Path("/Worksheets/Sheet2/SubSheet2"),
                                new Path("/Worksheets/Sheet2/SubSheet2"),
                                new Path("/Worksheets/Sheet2/SubSheet2"),
                                new Path("/Worksheets/Sheet2/SubSheet2"),
                                new Path("/Worksheets/Sheet2/SubSheet2"),
                                new Path("/Worksheets/Sheet2/SubSheet2"),
                                new Path("/Worksheets/Sheet1/SubSheet2"))),
                        new Path("/Worksheets/")
                },
                new Object[] {
                        new HashSet<>(Arrays.asList(
                                new Path("/Worksheets/Sheet2/SubSheet2/SubSheet2"),
                                new Path("/Worksheets/Sheet2/SubSheet2/SubSheet2"),
                                new Path("/Worksheets/Sheet2/SubSheet2/SubSheet2"),
                                new Path("/Worksheets/Sheet2/SubSheet2/SubSheet1"),
                                new Path("/Worksheets/Sheet2/SubSheet2/SubSheet2"),
                                new Path("/Worksheets/Sheet2/SubSheet2/SubSheet2"),
                                new Path("/Worksheets/Sheet2/SubSheet2/SubSheet2"))),
                        new Path("/Worksheets/Sheet2/SubSheet2/")
                },

                // Multiple paths without common parent
                new Object[] {
                        new HashSet<>(Arrays.asList(
                                new Path("/Worksheets/Sheet1/SubSheet1"),
                                new Path("/Repos/Repo/SubSheet2"))),
                        Path.root()
                });

    }

    @Test
    public void testFindCommonParentPath() {
        try {
            Path result = WorksheetPathUtil.findCommonParentPath(paths);
            assertEquals(expectedResult, result);
        } catch (Exception e) {
            assert expectedResult == null;
        }
    }
}
