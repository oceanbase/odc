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

package com.oceanbase.odc.service.task;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.service.task.caller.DefaultExecutorIdentifier;
import com.oceanbase.odc.service.task.caller.ExecutorIdentifier;
import com.oceanbase.odc.service.task.caller.ExecutorIdentifierParser;
import com.oceanbase.odc.service.task.exception.JobException;

/**
 * @author yaobin
 * @date 2023-12-25
 * @since 4.2.4
 */
public class ExecutorIdentifierTest {


    @Test
    public void test_parser_successful() throws JobException {

        String identifierString = "http://odc:8989/test";
        ExecutorIdentifier identifier = ExecutorIdentifierParser.parser(identifierString);
        Assert.assertNotNull(identifierString);
        Assert.assertEquals("test", identifier.getExecutorName());
        Assert.assertNull(identifier.getNamespace());
        Assert.assertEquals("odc", identifier.getHost());
        Assert.assertEquals(8989, identifier.getPort());
        Assert.assertEquals(identifier.toString(), identifierString);
    }

    @Test
    public void test_parser_decode() {
        // new version
        String str = "http://odc:8989/region/group/default/xxx:xxxx001";
        ExecutorIdentifier identifier = ExecutorIdentifierParser.parser(str);
        Assert.assertEquals(identifier.getRegion(), "region");
        Assert.assertEquals(identifier.getNamespace(), "default");
        Assert.assertEquals(identifier.getGroup(), "group");
        Assert.assertEquals(identifier.getExecutorName(), "xxx:xxxx001");

        // old version1
        String str2 = "http://odc:8989/default/xxx:xxxx001";
        ExecutorIdentifier identifierOld = ExecutorIdentifierParser.parser(str2);
        Assert.assertNull(identifierOld.getRegion());
        Assert.assertEquals(identifierOld.getNamespace(), "default");
        Assert.assertEquals(identifierOld.getExecutorName(), "xxx:xxxx001");
        // old version2
        String str3 = "http://odc:8989/xxx:xxxx001";
        ExecutorIdentifier identifierOld2 = ExecutorIdentifierParser.parser(str3);
        Assert.assertNull(identifierOld2.getRegion());
        Assert.assertNull(identifierOld2.getNamespace());
        Assert.assertEquals(identifierOld2.getExecutorName(), "xxx:xxxx001");
        // old version3
        String str4 = "http://odc:8989/";
        ExecutorIdentifier identifierOld3 = ExecutorIdentifierParser.parser(str4);
        Assert.assertNull(identifierOld3.getRegion());
        Assert.assertNull(identifierOld3.getNamespace());
        Assert.assertEquals(identifierOld3.getExecutorName(), "");
    }

    @Test
    public void test_executorDefaultValue_successful() throws JobException {
        ExecutorIdentifier identifier = DefaultExecutorIdentifier.builder().namespace(null)
                .executorName("test").build();

        Assert.assertEquals("test", identifier.getExecutorName());
        Assert.assertNull(identifier.getNamespace());
        Assert.assertEquals(DefaultExecutorIdentifier.DEFAULT_HOST, identifier.getHost());
        Assert.assertEquals(DefaultExecutorIdentifier.DEFAULT_PORT.intValue(), identifier.getPort());
    }

}
