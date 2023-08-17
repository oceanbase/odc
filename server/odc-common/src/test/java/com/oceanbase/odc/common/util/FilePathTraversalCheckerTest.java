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
package com.oceanbase.odc.common.util;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class FilePathTraversalCheckerTest {

    @Test
    public void checkPathTraversal_InWhiteList_ReturnTrue() {
        String filePath = "src/test/resources/resource-utils-test-data.jar";
        List<String> whiteList = Arrays.asList("src/test/resources");
        boolean isValid = FilePathTraversalChecker.checkPathTraversal(filePath, whiteList);
        Assert.assertTrue(isValid);
    }

    @Test
    public void checkPathTraversal_NotInWhiteList_ReturnFalse() {
        String filePath = "src/test/resources/resource-utils-test-data.jar";
        List<String> whiteList = Arrays.asList("src/test/resources/db");
        boolean isValid = FilePathTraversalChecker.checkPathTraversal(filePath, whiteList);
        Assert.assertFalse(isValid);
    }

    @Test
    public void checkPathTraversal_FileNotExists_ReturnTrue() {
        String filePath = "src/test/resources/db/filename-not-exists.txt";
        List<String> whiteList = Arrays.asList("src/test/resources/db");
        boolean isValid = FilePathTraversalChecker.checkPathTraversal(filePath, whiteList);
        Assert.assertTrue(isValid);
    }

    @Test
    public void checkPathTraversal_Traversal_ReturnFalse() {
        String filePath = "../../resources/db/filename-not-exists.txt";
        List<String> whiteList = Arrays.asList("src/test/resources/db");
        boolean isValid = FilePathTraversalChecker.checkPathTraversal(filePath, whiteList);
        Assert.assertFalse(isValid);
    }

    @Test(expected = NullPointerException.class)
    public void checkPathTraversal_WhiteListIsNull_BadArgumentException() {
        String filePath = "src/test/resources/resource-utils-test-data.jar";
        FilePathTraversalChecker.checkPathTraversal(filePath, null);
    }

    @Test
    public void checkPathTraversal_WhiteListIsEmpty_ReturnTrue() {
        String filePath = "src/test/resources/resource-utils-test-data.jar";
        boolean isValid = FilePathTraversalChecker.checkPathTraversal(filePath, Arrays.asList(""));
        Assert.assertTrue(isValid);
    }
}
