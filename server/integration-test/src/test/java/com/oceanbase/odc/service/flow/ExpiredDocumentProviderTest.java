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
package com.oceanbase.odc.service.flow;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.oceanbase.odc.service.flow.provider.BaseExpiredDocumentProvider;
import com.oceanbase.odc.service.flow.tool.TestExpiredDocumentProvider;

/**
 * Test cases for {@link BaseExpiredDocumentProvider}
 *
 * @author yh263208
 * @date 2022-04-01 10:15
 * @since ODC_release_3.3.0
 */
public class ExpiredDocumentProviderTest {

    private final static String ROOT_FILE_DIR = "root";

    @Before
    public void setUp() throws IOException {
        File rootDir = new File(ROOT_FILE_DIR);
        if (rootDir.exists()) {
            FileUtils.forceDelete(rootDir);
        }
    }

    @After
    public void tearDown() throws IOException {
        File rootDir = new File(ROOT_FILE_DIR);
        if (rootDir.exists()) {
            FileUtils.forceDelete(rootDir);
        }
    }

    @Test
    public void provide_NonExistRootDir_ReturnEmpty() {
        File rootDir = new File(ROOT_FILE_DIR);
        Assert.assertFalse(rootDir.exists());

        TestExpiredDocumentProvider provider = new TestExpiredDocumentProvider(1, TimeUnit.SECONDS, rootDir);
        Assert.assertTrue(provider.provide().isEmpty());
    }

    @Test
    public void provide_NonExpiredFiles_ReturnEmpty() throws IOException {
        File rootDir = new File(ROOT_FILE_DIR);
        FileUtils.forceMkdir(rootDir);
        Assert.assertTrue(rootDir.exists());

        File file = new File(rootDir.getAbsolutePath() + "/read.txt");
        Assert.assertTrue(file.createNewFile());
        Assert.assertTrue(file.isFile());

        TestExpiredDocumentProvider provider = new TestExpiredDocumentProvider(20, TimeUnit.SECONDS, rootDir);
        Assert.assertTrue(provider.provide().isEmpty());
    }

    @Test
    public void provide_OneFileExpiredAnotherNonExpired_ReturnOne() throws IOException, InterruptedException {
        File rootDir = new File(ROOT_FILE_DIR);
        FileUtils.forceMkdir(rootDir);
        Assert.assertTrue(rootDir.exists());

        File file = new File(rootDir.getAbsolutePath() + "/read.txt");
        Assert.assertTrue(file.createNewFile());
        Assert.assertTrue(file.isFile());

        Thread.sleep(1000);
        File file1 = new File(rootDir.getAbsolutePath() + "/read1.txt");
        Assert.assertTrue(file1.createNewFile());
        Assert.assertTrue(file1.isFile());

        long interval = System.currentTimeMillis() - file1.lastModified();
        TestExpiredDocumentProvider provider =
                new TestExpiredDocumentProvider(interval + 600, TimeUnit.MILLISECONDS, rootDir);
        List<File> fileList = provider.provide();
        Assert.assertEquals(1, fileList.size());
        Assert.assertEquals(file.getAbsolutePath(), fileList.get(0).getAbsolutePath());
    }

    @Test
    public void provide_BothExpired_ReturnAll() throws IOException {
        File rootDir = new File(ROOT_FILE_DIR);
        FileUtils.forceMkdir(rootDir);
        Assert.assertTrue(rootDir.exists());

        File file = new File(rootDir.getAbsolutePath() + "/read.txt");
        Assert.assertTrue(file.createNewFile());
        Assert.assertTrue(file.isFile());

        File file1 = new File(rootDir.getAbsolutePath() + "/read1.txt");
        Assert.assertTrue(file1.createNewFile());
        Assert.assertTrue(file1.isFile());

        TestExpiredDocumentProvider provider = new TestExpiredDocumentProvider(1, TimeUnit.MILLISECONDS, rootDir);
        List<File> fileList = provider.provide();
        Assert.assertEquals(2, fileList.size());
    }

}

