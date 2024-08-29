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

import com.oceanbase.odc.core.shared.exception.BadArgumentException;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.service.worksheet.utils.WorksheetPathUtil;

@RunWith(Parameterized.class)
public class PathMoveWhenDestinationPathExistTest {

    private final Path path;
    private final Path move;
    private final Path destination;
    private final Path expectedPathAfterMove;
    private final boolean expectedResult;

    public PathMoveWhenDestinationPathExistTest(Path path, Path move, Path destination, Path expectedPathAfterMove,
            boolean expectedResult) {
        this.path = path;
        this.move = move;
        this.destination = destination;
        this.expectedPathAfterMove = expectedPathAfterMove;
        this.expectedResult = expectedResult;
    }

    @Parameters
    public static List<Object[]> data() {
        return Arrays.asList(
                // bad case: null
                new Object[] {new Path("/Worksheets/"), null,
                        new Path("/Worksheets/folder1/"), null, false},
                new Object[] {new Path("/Worksheets/"), new Path("/Worksheets/file"),
                        null, null, false},
                // bad case: move path cannot be system define path(type=Root/Worksheets/Repos/Git_Repo)
                new Object[] {new Path("/Worksheets/"), new Path("/Worksheets/"),
                        new Path("/Worksheets/"), null, false},
                // bad case: destination path cannot be type=Root/Repos path
                new Object[] {new Path("/Worksheets/file1"), new Path("/Worksheets/file1"),
                        Path.root(), null, false},
                // bad case:the location between movePath and destinationPath must be same;
                new Object[] {new Path("/Worksheets/file1"), new Path("/Worksheets/file1"),
                        new Path("/Repos/folder2/"), null, false},
                // bad case: movePath cannot be the parent of destinationPath;
                new Object[] {new Path("/Worksheets/folder1/"), new Path("/Worksheets/folder1/"),
                        new Path("/Worksheets/folder1/sub1/"), null, false},
                // bad case: Same path cannot be moved
                new Object[] {new Path("/Worksheets/folder1/"), new Path("/Worksheets/folder1/"),
                        new Path("/Worksheets/folder1/"), null, false},
                // bad case: destinationPath can't be a file when movePath is a directory.
                new Object[] {new Path("/Worksheets/folder1/"), new Path("/Worksheets/folder1/"),
                        new Path("/Worksheets/folder1/file2"), null, false},
                // bad case: when destinationPath has existed,movePath and destinationPath cannot both be files at
                // the same time
                new Object[] {new Path("/Worksheets/folder1/file1"), new Path("/Worksheets/folder1/file1"),
                        new Path("/Worksheets/folder1/file2"), null, false},


                // destinationPath has existed, movePath is file, destinationPath is directory/Worksheets/Git_Repo,
                // create movePath in destinationPath
                new Object[] {new Path("/Worksheets/file1"), new Path("/Worksheets/file1"),
                        new Path("/Worksheets/folder1/"), new Path("/Worksheets/folder1/file1"), true},
                new Object[] {new Path("/Worksheets/folder1/file1"), new Path("/Worksheets/folder1/file1"),
                        new Path("/Worksheets/"), new Path("/Worksheets/file1"), true},
                new Object[] {new Path("/Repos/Name/file1"), new Path("/Repos/Name/file1"),
                        new Path("/Repos/Name/folder1/"), new Path("/Repos/Name/folder1/file1"), true},
                new Object[] {new Path("/Repos/Name/folder1/file1"), new Path("/Repos/Name/folder1/file1"),
                        new Path("/Repos/Name/"), new Path("/Repos/Name/file1"), true},
                // destinationPath has existed, movePath is directory, destinationPath is
                // directory/Worksheets/Git_Repo, create movePath in destinationPath
                new Object[] {new Path("/Worksheets/folder2/"), new Path("/Worksheets/folder2/"),
                        new Path("/Worksheets/folder1/"), new Path("/Worksheets/folder1/folder2/"), true},
                new Object[] {new Path("/Worksheets/folder2/file1"), new Path("/Worksheets/folder2/"),
                        new Path("/Worksheets/folder1/"), new Path("/Worksheets/folder1/folder2/file1"), true},
                new Object[] {new Path("/Worksheets/folder2/sub1/"), new Path("/Worksheets/folder2/"),
                        new Path("/Worksheets/folder1/"), new Path("/Worksheets/folder1/folder2/sub1/"), true},
                new Object[] {new Path("/Worksheets/folder1/folder2/"), new Path("/Worksheets/folder1/folder2/"),
                        new Path("/Worksheets/"), new Path("/Worksheets/folder2/"), true},
                new Object[] {new Path("/Worksheets/folder1/folder2/sub1/file1"),
                        new Path("/Worksheets/folder1/folder2/"),
                        new Path("/Worksheets/"), new Path("/Worksheets/folder2/sub1/file1"), true},
                new Object[] {new Path("/Repos/Name/folder2/"), new Path("/Repos/Name/folder2/"),
                        new Path("/Repos/Name/folder1/"), new Path("/Repos/Name/folder1/folder2/"), true},
                new Object[] {new Path("/Repos/Name/folder2/sub1/sub2/fil1"), new Path("/Repos/Name/folder2/"),
                        new Path("/Repos/Name/folder1/"), new Path("/Repos/Name/folder1/folder2/sub1/sub2/fil1"), true},
                new Object[] {new Path("/Repos/Name/folder1/folder2/"), new Path("/Repos/Name/folder1/folder2/"),
                        new Path("/Repos/Name/"), new Path("/Repos/Name/folder2/"), true},
                new Object[] {new Path("/Repos/Name/folder1/folder2/file1"), new Path("/Repos/Name/folder1/folder2/"),
                        new Path("/Repos/Name/"), new Path("/Repos/Name/folder2/file1"), true});
    }

    @Test
    public void rename() {
        try {
            WorksheetPathUtil.checkMoveValid(move, destination);
            WorksheetPathUtil.checkMoveValidWithDestinationPathExist(move, destination);
            Optional<Path> moved = path.moveWhenDestinationPathExist(this.move, destination);
            assertEquals(expectedResult, moved.isPresent());
            if (expectedResult) {
                assertEquals(moved.get(), expectedPathAfterMove);
                assertEquals(moved.get().getLevelNum(), expectedPathAfterMove.getLevelNum());
            }
        } catch (BadArgumentException | BadRequestException e) {
            assert !expectedResult;
        }
    }
}
