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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.oceanbase.odc.core.shared.exception.BadArgumentException;
import com.oceanbase.odc.service.worksheet.utils.WorksheetPathUtil;

@RunWith(Parameterized.class)
public class PathRenameTest {

    private final Path path;
    private final Path from;
    private final Path destination;
    private final Path expectedPathAfterRename;
    private final boolean expectedResult;

    public PathRenameTest(Path path, Path from, Path destination, Path expectedPathAfterRename,
            boolean expectedResult) {
        this.path = path;
        this.from = from;
        this.destination = destination;
        this.expectedPathAfterRename = expectedPathAfterRename;
        this.expectedResult = expectedResult;
    }

    @Parameters
    public static List<Object[]> data() {
        return Arrays.asList(
                new Object[] {new Path("/Worksheets/Sheet1"), new Path("/Worksheets/Sheet1"),
                        new Path("/Worksheets/Sheet2"), new Path("/Worksheets/Sheet2"), true},
                new Object[] {new Path("/Worksheets/Sheet1/"), new Path("/Worksheets/Sheet1/"),
                        new Path("/Worksheets/Sheet2/"), new Path("/Worksheets/Sheet2/"), true},
                new Object[] {new Path("/Worksheets/Sheet1/SubSheet1"), new Path("/Worksheets/Sheet1/"),
                        new Path("/Worksheets/Sheet2/"), new Path("/Worksheets/Sheet2/SubSheet1"), true},
                // test cases: although 'from' and 'destination' meet the renaming criteria, 'path' and 'from' do
                // not match and cannot be renamed
                new Object[] {new Path("/Worksheets/Sheet1/"), new Path("/Worksheets/Sheet1/file1"),
                        new Path("/Worksheets/Sheet1/file2"), new Path("/Worksheets/Sheet1/"), false},
                new Object[] {new Path("/Worksheets/Sheet1/file3"), new Path("/Worksheets/Sheet1/file1"),
                        new Path("/Worksheets/Sheet1/file2"), new Path("/Worksheets/Sheet1//file3"), false},
                new Object[] {new Path("/Worksheets/Sheet1/file1/"), new Path("/Worksheets/Sheet1/file1"),
                        new Path("/Worksheets/Sheet1/file2"), new Path("/Worksheets/Sheet1/file1/"), false},
                new Object[] {new Path("/Worksheets/Sheet1/SubSheet1"),
                        new Path("/Worksheets/Sheet1/SubSheet1/SubFile1"),
                        new Path("/Worksheets/Sheet1/SubSheet1/SubFile2"), new Path("/Worksheets/Sheet1/SubSheet1"),
                        false},
                // test cases: /Worksheets/ folder cannot be renamed
                new Object[] {new Path("/Worksheets/"), new Path("/Worksheets/"),
                        new Path("/Worksheets/sheet/"), new Path("/Worksheets/"), false},
                // test cases: 'path' and 'from' do not match, cannot be renamed
                new Object[] {new Path("/Worksheets/Sheet1/SubSheet1"), new Path("/Worksheets/Sheet1"),
                        new Path("/Worksheets/Sheet1/SubSheet2"), new Path("/Worksheets/Sheet1/SubSheet1"), false},
                // test cases: 'from' and 'destination' do not meet the renaming criteria and cannot be renamed
                new Object[] {new Path("/Worksheets/Sheet2/SubSheet1"), new Path("/Worksheets/Sheet2/SubSheet1"),
                        new Path("/Worksheets/Sheet1/SubSheet2"), new Path("/Worksheets/Sheet2/SubSheet1"), false},
                // test cases: 'path' and 'from' do not match, cannot be renamed
                new Object[] {new Path("/Worksheets/Sheet1/SubSheet1/SubFile1"),
                        new Path("/Worksheets/Sheet1/SubSheet1"), new Path("/Worksheets/Sheet1/SubSheet1/SubFile2"),
                        new Path("/Worksheets/Sheet1/SubSheet1/SubFile1"), false},

                // For Repos
                new Object[] {new Path("/Repos/git1/Sheet1"), new Path("/Repos/git1/Sheet1"),
                        new Path("/Repos/git1/Sheet2"), new Path("/Repos/git1/Sheet2"), true},
                new Object[] {new Path("/Repos/git1/Sheet1/"), new Path("/Repos/git1/Sheet1/"),
                        new Path("/Repos/git1/Sheet2/"), new Path("/Repos/git1/Sheet2/"), true},
                new Object[] {new Path("/Repos/git1/Sheet1/SubSheet1"), new Path("/Repos/git1/Sheet1/"),
                        new Path("/Repos/git1/Sheet2/"), new Path("/Repos/git1/Sheet2/SubSheet1"), true},
                // test cases: although 'from' and 'destination' meet the renaming criteria, 'path' and 'from' do
                // not match and cannot be renamed
                new Object[] {new Path("/Repos/git1/Sheet1/"), new Path("/Repos/git1/Sheet1/file1"),
                        new Path("/Repos/git1/Sheet1/file2"), new Path("/Repos/git1/Sheet1/"), false},
                new Object[] {new Path("/Repos/git1/Sheet1/file3"), new Path("/Repos/git1/Sheet1/file1"),
                        new Path("/Repos/git1/Sheet1/file2"), new Path("/Repos/git1/Sheet1/file3"), false},
                new Object[] {new Path("/Repos/git1/Sheet1/file1/"), new Path("/Repos/git1/Sheet1/file1"),
                        new Path("/Repos/git1/Sheet1/file2"), new Path("/Repos/git1/Sheet1/file1/"), false},
                new Object[] {new Path("/Repos/git1/Sheet1/SubSheet1"),
                        new Path("/Repos/git1/Sheet1/SubSheet1/SubFile1"),
                        new Path("/Repos/git1/Sheet1/SubSheet1/SubFile2"), new Path("/Repos/git1/Sheet1/SubSheet1"),
                        false},
                // test cases: /Repos/git1/ folder cannot be renamed
                new Object[] {new Path("/Repos/git1/"), new Path("/Repos/git1/"),
                        new Path("/Repos/git12/"), new Path("/Repos/git1/"), false},
                // test cases: 'path' and 'from' do not match, cannot be renamed
                new Object[] {new Path("/Repos/git1/Sheet1/SubSheet1"), new Path("/Repos/git1/Sheet1"),
                        new Path("/Repos/git1/Sheet1/SubSheet2"), new Path("/Repos/git1/Sheet1/SubSheet1"), false},
                // test cases: 'from' and 'destination' do not meet the renaming criteria and cannot be renamed
                new Object[] {new Path("/Repos/git1/Sheet2/SubSheet1"), new Path("/Repos/git1/Sheet2/SubSheet1"),
                        new Path("/Repos/git1/Sheet1/SubSheet2"), new Path("/Repos/git1/Sheet2/SubSheet1"), false},
                // test cases: 'path' and 'from' do not match, cannot be renamed
                new Object[] {new Path("/Repos/git1/Sheet1/SubSheet1/SubFile1"),
                        new Path("/Repos/git1/Sheet1/SubSheet1"), new Path("/Repos/git1/Sheet1/SubSheet1/SubFile2"),
                        new Path("/Repos/git1/Sheet1/SubSheet1/SubFile1"), false});
    }

    @Test
    public void rename() {
        try {
            WorksheetPathUtil.renameValidCheck(from, destination);
            boolean result = path.rename(from, destination);
            assertEquals(path, expectedPathAfterRename);
            assertEquals(expectedResult, result);
        } catch (BadArgumentException e) {
            assert !expectedResult;
        }
    }
}
