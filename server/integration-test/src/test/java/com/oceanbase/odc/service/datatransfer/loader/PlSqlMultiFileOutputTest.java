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
import java.io.FileOutputStream;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.oceanbase.odc.common.file.zip.ZipFileTree;

public class PlSqlMultiFileOutputTest {
    private static final String ZIP_FILE_PATH = "src/test/resources/datatransfer/PlSqlOutput.zip";
    private static final String DEST_FILE_PATH = "src/test/resources/datatransfer/obCompatible.zip";
    private PlSqlMultiFileOutput plSqlMultiFileOutput;

    @Before
    public void setUp() throws Exception {
        File zipFile = new File(ZIP_FILE_PATH);
        mockZipFile(zipFile);
        plSqlMultiFileOutput = new PlSqlMultiFileOutput(zipFile);
    }

    @After
    public void tearDown() {
        FileUtils.deleteQuietly(new File(ZIP_FILE_PATH));
        FileUtils.deleteQuietly(new File(DEST_FILE_PATH));
    }

    @Test
    public void test_Support() {
        Assert.assertTrue(plSqlMultiFileOutput.supports());
    }

    @Test
    public void test_ToObCompatibleFormat() throws Exception {
        File dest = new File(DEST_FILE_PATH);
        plSqlMultiFileOutput.toObLoaderDumperCompatibleFormat(dest);
        AtomicInteger count = new AtomicInteger();
        new ZipFileTree(dest).forEach(((integer, zipElement) -> {
            if (zipElement.getName().endsWith("-schema.sql")) {
                count.getAndIncrement();
            }
        }));
        Assert.assertEquals(2, count.get());
    }

    private void mockZipFile(File file) throws Exception {
        try (FileOutputStream outputStream = new FileOutputStream(file);
                ArchiveOutputStream out = new ZipArchiveOutputStream(outputStream)) {
            out.putArchiveEntry(new ZipArchiveEntry("test_table.tab"));
            out.putArchiveEntry(new ZipArchiveEntry("test_procedure.prc"));
            out.closeArchiveEntry();
        }
    }

}
