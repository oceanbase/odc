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
package com.oceanbase.odc.service.datatransfer.dumper;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.plugin.task.api.datatransfer.dumper.DumperOutput;

public class SchemaMergeOperatorTest {
    private DumperOutput dumperOutput;
    private File folder;

    @Before
    public void setUp() throws Exception {
        dumperOutput = new DumperOutput(getDumpZip());
        folder = new File("datatransfer/temp-export");
        dumperOutput.toFolder(folder);
    }

    @After
    public void tearDown() {
        FileUtils.deleteQuietly(folder);
    }

    @Test
    public void test_MergeSchemaFiles_Success() throws Exception {
        SchemaMergeOperator operator = new SchemaMergeOperator(dumperOutput, "SYS", DialectType.OB_ORACLE);
        File dest = new File("datatransfer/temp-export/schema.sql");
        operator.mergeSchemaFiles(dest, null);

        Assert.assertTrue(dest.exists());
    }

    private File getDumpZip() throws URISyntaxException {
        URL url = DumperOutput.class.getClassLoader().getResource("datatransfer/export.zip");
        assert url != null;
        return new File(url.toURI());
    }

}
