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
package com.oceanbase.odc.core.shared;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.BadArgumentException;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.core.shared.exception.OverLimitException;

public class PreConditionsTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void notNull_NotNull_ReturnObj() {
        String ret = PreConditions.notNull("", "p1");
        Assert.assertEquals("", ret);
    }

    @Test
    public void notNull_Null_ThrowNotNull() {
        thrown.expect(BadArgumentException.class);
        thrown.expectMessage("parameter p1 may not be null");
        PreConditions.notNull(null, "p1");
    }

    @Test
    public void notEmpty_Null_ThrowNotNull() {
        String val = null;
        thrown.expect(BadArgumentException.class);
        thrown.expectMessage("parameter p1 may not be null");
        PreConditions.notEmpty(val, "p1");
    }

    @Test
    public void notEmpty_Empty_ThrowNotEmpty() {
        thrown.expect(BadArgumentException.class);
        thrown.expectMessage("parameter p1 may not be empty");
        PreConditions.notEmpty("", "p1");
    }

    @Test
    public void notEmpty_NotEmpty_ReturnObj() {
        String ret = PreConditions.notEmpty("v", "p1");
        Assert.assertEquals("v", ret);
    }

    @Test
    public void validateExists_Exists_ReturnTrue() {
        boolean ret = PreConditions.validExists(ResourceType.OB_TABLE, "tableName", "t1", () -> true);
        Assert.assertTrue(ret);
    }

    @Test
    public void validateExists_NotExists_ThrowNotFound() {
        thrown.expect(NotFoundException.class);
        thrown.expectMessage("OB_TABLE not found by tableName=t1");
        PreConditions.validExists(ResourceType.OB_TABLE, "tableName", "t1", () -> false);
    }

    @Test
    public void validNotSqlInjection_ParamIsNull_ReturnTrue() {
        boolean isNotInjection = PreConditions.validNotSqlInjection(null, "cluster");
        Assert.assertTrue(isNotInjection);

    }

    @Test
    public void validNotSqlInjection_IsInjection_ThrowBadArgument() {
        thrown.expect(BadArgumentException.class);
        thrown.expectMessage("cluster may contain invalid characters");
        PreConditions.validNotSqlInjection("'and(select*from(select+sleep(5))a/**/union/**/select+1)='",
                "cluster");
    }

    @Test
    public void validNotSqlInjection_NotInjection_ReturnTrue() {
        boolean isNotInjection = PreConditions.validNotSqlInjection("oms_cluster", "cluster");
        Assert.assertTrue(isNotInjection);
    }

    @Test
    public void maxLength_ASCII_OverThreshold_OverLimitException() {
        thrown.expect(OverLimitException.class);
        thrown.expectMessage("sql has exceeded sql length limit: 18");
        PreConditions.maxLength("select * from dual;", "sql", 18);
    }

    @Test
    public void maxLength_ZHCN_OverThreshold_OverLimitException() {
        thrown.expect(OverLimitException.class);
        thrown.expectMessage("sql has exceeded sql size limit: 11");
        PreConditions.maxSize("中文测试", "sql", 11);
    }

    @Test
    public void validExists_NotExists_NotFoundException() {
        thrown.expect(NotFoundException.class);
        thrown.expectMessage("File not found by fileName=somefile-not-exists.txt");
        PreConditions.validExists(new File("somefile-not-exists.txt"));
    }

    @Test
    public void validExists_Exists_ReturnTrue() throws IOException {
        File tempFile = File.createTempFile("test", null, new File("."));
        tempFile.deleteOnExit();

        boolean exists = PreConditions.validExists(tempFile);

        Assert.assertTrue(exists);
    }

    @Test
    public void validNoPathTraversal_NoTraversal_ReturnTrue() {
        String filePath = "src/test/resources/db/somefilename.txt";

        boolean valid = PreConditions.validNoPathTraversal(filePath, "src/test/resources/db");

        Assert.assertTrue(valid);
    }

    @Test(expected = BadRequestException.class)
    public void validNoPathTraversal_Traversal_BadRequestException() {
        String filePath = "../../resources/db/somefilename.txt";
        PreConditions.validNoPathTraversal(filePath, "src/test/resources/db");
    }

    @Test
    public void validFileSuffix_InSafeSuffixList_ReturnTrue() {
        String fileName = "1.sql";
        boolean safeSuffix = PreConditions.validFileSuffix(fileName, Arrays.asList(".sql", ".zip", ".csv", ".pl"));
        Assert.assertTrue(safeSuffix);
    }

    @Test
    public void validFileSuffix_FileNotContainsSuffix_ReturnTrue() {
        String fileName = "whatever_file_name";
        boolean safeSuffix = PreConditions.validFileSuffix(fileName, Arrays.asList(".sql", ".zip", ".csv", ".pl"));
        Assert.assertTrue(safeSuffix);
    }

    @Test
    public void validFileSuffix_AllowAllSuffix_ReturnTrue() {
        String fileName = "whatever_file_name.sh";
        boolean safeSuffix = PreConditions.validFileSuffix(fileName, Arrays.asList("*", ".sql", ".zip", ".csv", ".pl"));
        Assert.assertTrue(safeSuffix);
    }

    @Test(expected = BadRequestException.class)
    public void validFileSuffix_NotInSafeSuffixList_ThrowBadRequestException() {
        String fileName = "whatever_file_name.sh";
        PreConditions.validFileSuffix(fileName, Arrays.asList(".sql", ".zip", ".csv", ".pl"));
    }

    @Test(expected = BadArgumentException.class)
    public void validFileSuffix_BlankFileName_ThrowBadRequestException() {
        String fileName = "";
        PreConditions.validFileSuffix(fileName, Arrays.asList(".sql", ".zip", ".csv", ".pl"));
    }

    @Test(expected = BadArgumentException.class)
    public void validSingleton_EmptyCollection_ThrowBadArgumentException() {
        PreConditions.validSingleton(Collections.emptyList(), "collection");
    }

    @Test(expected = BadArgumentException.class)
    public void validSingleton_MoreThanOneCollection_ThrowBadArgumentException() {
        PreConditions.validSingleton(Arrays.asList(1, 2, 3), "collection");
    }
}
