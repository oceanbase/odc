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
package com.oceanbase.odc.service.datatransfer.loader;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.util.StreamUtils;

public class PlSqlSingleFileOutputTest {
    private static final String SQL_FILE_PATH = "src/test/resources/datatransfer/PlSqlOutput.sql";
    private static final String DEST_FILE_PATH = "src/test/resources/datatransfer/obCompatible.sql";

    private final PlSqlSingleFileOutput plSqlSingleFileOutput = new PlSqlSingleFileOutput(new File(SQL_FILE_PATH));

    @Test
    public void test_Support() {
        Assert.assertTrue(plSqlSingleFileOutput.supports());
    }

    @Test
    public void test_ToObCompatibleFormat() throws Exception {
        File dest = new File(DEST_FILE_PATH);
        try {
            plSqlSingleFileOutput.toObLoaderDumperCompatibleFormat(dest);
            try (InputStream input = new FileInputStream(dest)) {
                String sqls = StreamUtils.copyToString(input, StandardCharsets.UTF_8);
                System.out.println(sqls);
            }
        } finally {
            FileUtils.deleteQuietly(dest);
        }
    }

}
