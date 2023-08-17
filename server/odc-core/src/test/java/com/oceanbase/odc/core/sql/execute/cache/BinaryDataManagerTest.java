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
package com.oceanbase.odc.core.sql.execute.cache;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.util.ResourceUtils;

import com.oceanbase.odc.core.sql.execute.cache.model.BinaryContentMetaData;

/**
 * Test case for {@code BinaryDataManager}
 *
 * @author yh263208
 * @date 2021-11-05 15:11
 * @since ODC_release_3.2.2
 */
public class BinaryDataManagerTest {

    private static final String DATA_DIR_NAME = "BinaryDataManagerTest".toLowerCase();
    private final Random random = new Random();
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws IOException {
        File dataDir = new File(getBinaryFilePath());
        for (File file : dataDir.listFiles()) {
            FileUtils.forceDelete(file);
        }
    }

    @Test
    public void testNonExistWorkingDir() throws IOException {
        String path = getBinaryFilePath() + "/" + UUID.randomUUID();

        thrown.expect(FileNotFoundException.class);
        thrown.expectMessage("Input path does not exist, workingDir=" + path);
        BinaryDataManager dataManager = new FileBaseBinaryDataManager(path);
    }

    @Test
    public void testWorkingDirIsAFile() throws IOException {
        String path = getBinaryFilePath() + "/" + UUID.randomUUID() + ".txt";
        File file = new File(path);
        Assert.assertTrue(file.createNewFile());

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Input string is not a directory, workingDir=" + path);
        BinaryDataManager dataManager = new FileBaseBinaryDataManager(path);
    }

    @Test
    public void testReadContent() throws IOException {
        Map<Integer, String> index2Content = new HashMap<>();
        Map<Integer, BinaryContentMetaData> index2MetaData = new HashMap<>();

        BinaryDataManager dataManager = getDataManager();
        for (int i = 0; i < 3; i++) {
            String content = getInputContent();
            index2Content.putIfAbsent(i, content);
            index2MetaData.putIfAbsent(i, dataManager.write(getInputContentStream(content)));
        }

        for (int i = 0; i < 3; i++) {
            BinaryContentMetaData metaData = index2MetaData.get(i);
            Assert.assertNotNull(metaData);
            InputStream inputStream = dataManager.read(metaData);
            String fromFile = String.join("", IOUtils.readLines(inputStream));
            String fromMemory = index2Content.get(i);
            Assert.assertEquals(fromFile, fromMemory);
        }
    }

    private FileBaseBinaryDataManager getDataManager() throws IOException {
        return new FileBaseBinaryDataManager(getBinaryFilePath());
    }

    private InputStream getInputContentStream(String inputContent) {
        return new ByteArrayInputStream(inputContent.getBytes());
    }

    private String getInputContent() {
        int contentSizeBytes = random.nextInt(5 * 1024);
        char[] chars = new char[contentSizeBytes];
        for (int i = 0; i < contentSizeBytes; i++) {
            chars[i] = choseChar();
        }
        return new String(chars);
    }

    private char choseChar() {
        if (random.nextBoolean()) {
            return (char) (random.nextInt(26) + 65);
        }
        return (char) (random.nextInt(26) + 97);
    }

    private String getBinaryFilePath() {
        File file;
        try {
            file = new File(ResourceUtils.getURL("classpath:").getPath() + "/" + DATA_DIR_NAME);
            if (!file.exists()) {
                if (!file.mkdir()) {
                    throw new Exception("Failed to create dir");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("fail to get dir");
        }
        return file.getAbsolutePath();
    }

}
