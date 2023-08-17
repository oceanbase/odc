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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.util.ResourceUtils;

/**
 * Test case for {@link PagedRandomAccessFile}
 *
 * @author yh263208
 * @date 2021-12-03 13:49
 * @since ODC_release_3.2.2
 */
public class PagedRandomAccessFileTest {

    private static final String DATA_DIR_NAME = "PagedRandomAccessFileTest".toLowerCase();
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(256, 256, 0, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(), new ThreadPoolExecutor.CallerRunsPolicy());

    @Before
    public void setUp() throws IOException {
        File dataDir = new File(getBinaryFilePath());
        for (File file : dataDir.listFiles()) {
            FileUtils.forceDelete(file);
        }
    }

    @Test
    public void testWriteContentLessThanPage() throws IOException {
        PageManager pageManager = getPageManager();
        PagedRandomAccessFile randomAccessFile = getRandomFile(UUID.randomUUID().toString(), pageManager);
        Assert.assertNotNull(randomAccessFile);

        String content = generateContents(123);
        randomAccessFile.write(content.getBytes());
        Assert.assertEquals(randomAccessFile.length(), 123);
    }

    @Test
    public void testWriteContentEqualsPage() throws IOException {
        PageManager pageManager = getPageManager();
        PagedRandomAccessFile randomAccessFile = getRandomFile(UUID.randomUUID().toString(), pageManager);
        Assert.assertNotNull(randomAccessFile);

        String content = generateContents(PageManager.STORAGE_LAYER_PAGE_SIZE_BYTE);
        randomAccessFile.write(content.getBytes());
        Assert.assertEquals(randomAccessFile.length(), PageManager.STORAGE_LAYER_PAGE_SIZE_BYTE);
    }

    @Test
    public void testWriteContentEqualsMultiPages() throws IOException {
        PageManager pageManager = getPageManager();
        PagedRandomAccessFile randomAccessFile = getRandomFile(UUID.randomUUID().toString(), pageManager);
        Assert.assertNotNull(randomAccessFile);

        String content = generateContents(PageManager.STORAGE_LAYER_PAGE_SIZE_BYTE * 34 + 234);
        randomAccessFile.write(content.getBytes());
        Assert.assertEquals(randomAccessFile.length(), PageManager.STORAGE_LAYER_PAGE_SIZE_BYTE * 34 + 234);
    }

    @Test
    public void testAppendRandomFile() throws IOException {
        PageManager pageManager = getPageManager();
        PagedRandomAccessFile randomAccessFile = getRandomFile(UUID.randomUUID().toString(), pageManager);
        Assert.assertNotNull(randomAccessFile);

        String content = generateContents(PageManager.STORAGE_LAYER_PAGE_SIZE_BYTE * 34 + 234);
        randomAccessFile.write(content.getBytes());

        content = generateContents(123456);
        randomAccessFile.write(content.getBytes());
        Assert.assertEquals(randomAccessFile.length(),
                PageManager.STORAGE_LAYER_PAGE_SIZE_BYTE * 34 + 234 + 123456);
    }

    @Test
    public void testWriteContentWithSeek() throws IOException {
        PageManager pageManager = getPageManager();
        PagedRandomAccessFile randomAccessFile = getRandomFile(UUID.randomUUID().toString(), pageManager);
        Assert.assertNotNull(randomAccessFile);

        String content = generateContents(PageManager.STORAGE_LAYER_PAGE_SIZE_BYTE * 34 + 234);
        randomAccessFile.write(content.getBytes());

        randomAccessFile.seekForWrite(30);
        content = "abcde";
        randomAccessFile.write(content.getBytes());
        Assert.assertEquals(randomAccessFile.length(), PageManager.STORAGE_LAYER_PAGE_SIZE_BYTE * 34 + 234);
    }

    @Test
    public void testWriteContentWithSeekToMuch() throws IOException {
        PageManager pageManager = getPageManager();
        PagedRandomAccessFile randomAccessFile = getRandomFile(UUID.randomUUID().toString(), pageManager);
        Assert.assertNotNull(randomAccessFile);

        String content = generateContents(PageManager.STORAGE_LAYER_PAGE_SIZE_BYTE * 34 + 234);
        randomAccessFile.write(content.getBytes());

        randomAccessFile.seekForWrite(30);
        content = generateContents(PageManager.STORAGE_LAYER_PAGE_SIZE_BYTE * 34 + 234 + 543);
        randomAccessFile.write(content.getBytes());
        Assert.assertEquals(randomAccessFile.length(),
                PageManager.STORAGE_LAYER_PAGE_SIZE_BYTE * 34 + 234 + 543 + 30);
    }

    @Test
    public void testReadContentLessThanPage() throws IOException {
        PageManager pageManager = getPageManager();
        PagedRandomAccessFile randomAccessFile = getRandomFile(UUID.randomUUID().toString(), pageManager);
        String content = generateContents(123);
        randomAccessFile.write(content.getBytes());

        byte[] buffer = new byte[123];
        int length = randomAccessFile.read(buffer);
        Assert.assertEquals(123, length);
        Assert.assertEquals(content, new String(buffer));
    }

    @Test
    public void testReadContentEqualsPage() throws IOException {
        PageManager pageManager = getPageManager();
        PagedRandomAccessFile randomAccessFile = getRandomFile(UUID.randomUUID().toString(), pageManager);

        String content = generateContents(PageManager.STORAGE_LAYER_PAGE_SIZE_BYTE);
        randomAccessFile.write(content.getBytes());

        byte[] buffer = new byte[PageManager.STORAGE_LAYER_PAGE_SIZE_BYTE];
        int length = randomAccessFile.read(buffer);
        Assert.assertEquals(PageManager.STORAGE_LAYER_PAGE_SIZE_BYTE, length);
        Assert.assertEquals(content, new String(buffer));
    }

    @Test
    public void testReadContentEqualsMultiPages() throws IOException {
        PageManager pageManager = getPageManager();
        PagedRandomAccessFile randomAccessFile = getRandomFile(UUID.randomUUID().toString(), pageManager);
        String content = generateContents(PageManager.STORAGE_LAYER_PAGE_SIZE_BYTE * 34 + 234);
        randomAccessFile.write(content.getBytes());

        byte[] buffer = new byte[PageManager.STORAGE_LAYER_PAGE_SIZE_BYTE * 34 + 234];
        int length = randomAccessFile.read(buffer);
        Assert.assertEquals(PageManager.STORAGE_LAYER_PAGE_SIZE_BYTE * 34 + 234, length);
        Assert.assertEquals(content, new String(buffer));
    }

    @Test
    public void testAppendAndReadRandomFile() throws IOException {
        PageManager pageManager = getPageManager();
        PagedRandomAccessFile randomAccessFile = getRandomFile(UUID.randomUUID().toString(), pageManager);
        String content = generateContents(PageManager.STORAGE_LAYER_PAGE_SIZE_BYTE * 34 + 234);
        randomAccessFile.write(content.getBytes());
        String content1 = generateContents(123456);
        randomAccessFile.write(content1.getBytes());

        byte[] buffer = new byte[PageManager.STORAGE_LAYER_PAGE_SIZE_BYTE * 34 + 234 + 123456];
        int length = randomAccessFile.read(buffer);
        Assert.assertEquals(PageManager.STORAGE_LAYER_PAGE_SIZE_BYTE * 34 + 234 + 123456, length);
        Assert.assertEquals(content + content1, new String(buffer));
    }

    @Test
    public void testReadContentWithSeek() throws IOException {
        PageManager pageManager = getPageManager();
        PagedRandomAccessFile randomAccessFile = getRandomFile(UUID.randomUUID().toString(), pageManager);
        String content = generateContents(PageManager.STORAGE_LAYER_PAGE_SIZE_BYTE * 34 + 234);
        randomAccessFile.write(content.getBytes());
        randomAccessFile.seekForWrite(PageManager.STORAGE_LAYER_PAGE_SIZE_BYTE - 3);
        content = "abcde";
        randomAccessFile.write(content.getBytes());

        byte[] buffer = new byte[PageManager.STORAGE_LAYER_PAGE_SIZE_BYTE * 34 + 234];
        int length = randomAccessFile.read(buffer);
        Assert.assertEquals(PageManager.STORAGE_LAYER_PAGE_SIZE_BYTE * 34 + 234, length);

        randomAccessFile.seekForRead(PageManager.STORAGE_LAYER_PAGE_SIZE_BYTE - 3);
        buffer = new byte[content.length()];
        randomAccessFile.read(buffer, 0, 5);
        Assert.assertEquals(content, new String(buffer));
    }

    @Test
    public void testReadContentWithSeekToMuch() throws IOException {
        PageManager pageManager = getPageManager();
        PagedRandomAccessFile randomAccessFile = getRandomFile(UUID.randomUUID().toString(), pageManager);
        String content = generateContents(PageManager.STORAGE_LAYER_PAGE_SIZE_BYTE * 34 + 234);
        randomAccessFile.write(content.getBytes());
        randomAccessFile.seekForWrite(30);
        content = generateContents(PageManager.STORAGE_LAYER_PAGE_SIZE_BYTE * 34 + 234 + 543);
        randomAccessFile.write(content.getBytes());

        byte[] buffer = new byte[PageManager.STORAGE_LAYER_PAGE_SIZE_BYTE * 34 + 234 + 543 + 40];
        int length = randomAccessFile.read(buffer);
        Assert.assertEquals(PageManager.STORAGE_LAYER_PAGE_SIZE_BYTE * 34 + 234 + 543 + 30, length);
        Assert.assertEquals(-1, randomAccessFile.read(buffer));
    }

    @Test
    public void testReloadFileFromDisk() throws IOException {
        PageManager pageManager = getPageManager();
        String fileName = UUID.randomUUID().toString();
        PagedRandomAccessFile randomAccessFile = getRandomFile(fileName, pageManager);
        String content = generateContents(PageManager.STORAGE_LAYER_PAGE_SIZE_BYTE * 34 + 234);
        randomAccessFile.write(content.getBytes());
        randomAccessFile.close();

        PagedRandomAccessFile file = getRandomFile(fileName, pageManager);
        byte[] buffer = new byte[PageManager.STORAGE_LAYER_PAGE_SIZE_BYTE * 34 + 234];
        Assert.assertEquals(PageManager.STORAGE_LAYER_PAGE_SIZE_BYTE * 34 + 234, file.read(buffer));
        Assert.assertEquals(content, new String(buffer));
    }

    @Test
    public void testCopyFileFromDisk() throws IOException {
        PageManager pageManager = getPageManager();
        String fileName = UUID.randomUUID().toString();
        PagedRandomAccessFile randomAccessFile = getRandomFile(fileName, pageManager);
        String content = generateContents(PageManager.STORAGE_LAYER_PAGE_SIZE_BYTE * 34 + 234);
        randomAccessFile.write(content.getBytes());
        randomAccessFile.close();

        PagedRandomAccessFile fileSrc = getRandomFile(fileName, pageManager);
        PagedRandomAccessFile fileDest = getRandomFile(UUID.randomUUID().toString(), pageManager);
        byte[] buffer = new byte[123];
        int length = fileSrc.read(buffer);
        while (length != -1) {
            fileDest.write(buffer, 0, length);
            length = fileSrc.read(buffer, 0, length);
        }
        buffer = new byte[PageManager.STORAGE_LAYER_PAGE_SIZE_BYTE * 34 + 234];
        Assert.assertEquals(PageManager.STORAGE_LAYER_PAGE_SIZE_BYTE * 34 + 234, fileDest.read(buffer));
        Assert.assertEquals(content, new String(buffer));
        fileSrc.close();
        fileDest.close();
        pageManager.close();
    }

    @Test
    public void testConcurrentReadWriteFile()
            throws IOException, ExecutionException, InterruptedException, TimeoutException {
        PageManager pageManager = getPageManager();
        Map<String, Future<String>> fileName2Future = new HashMap<>();
        Random random = new Random();
        Map<String, String> fileName2Content = new HashMap<>();
        for (int i = 0; i < 100; i++) {
            int contentSize = PageManager.STORAGE_LAYER_PAGE_SIZE_BYTE * (34 + random.nextInt(28));
            String content =
                    generateContents(contentSize + random.nextInt(PageManager.STORAGE_LAYER_PAGE_SIZE_BYTE));
            String fileName = UUID.randomUUID().toString();
            fileName2Content.putIfAbsent(fileName, content);
            Future<String> future = executor.submit(getTestTask(fileName, content, pageManager));
            fileName2Future.putIfAbsent(fileName, future);
        }

        for (Map.Entry<String, Future<String>> entry : fileName2Future.entrySet()) {
            String filename = entry.getKey();
            Future<String> future = entry.getValue();
            Assert.assertEquals(fileName2Content.get(filename), future.get(20, TimeUnit.SECONDS));
        }
        pageManager.close();
    }

    private FileTestCallable getTestTask(String fileName, String content, PageManager pageManager) {
        return new FileTestCallable(fileName, getBinaryFilePath(), content, pageManager);
    }

    private String generateContents(int size) {
        Random random = new Random();
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < size; i++) {
            content.append((char) (random.nextInt(26) + 97));
        }
        return content.toString();
    }

    private PageManager getPageManager() throws IOException {
        return new PageManager(getBinaryFilePath());
    }

    private PagedRandomAccessFile getRandomFile(String fileName, PageManager pageManager) throws IOException {
        File randomFile = new File(getBinaryFilePath() + "/" + fileName);
        if (!randomFile.exists()) {
            Assert.assertTrue(randomFile.createNewFile());
        }
        return new PagedRandomAccessFile(randomFile.getAbsolutePath(), pageManager);
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


class FileTestCallable implements Callable<String> {
    private final String content;
    private final PageManager pageManager;
    private final String binaryPath;
    private final int bufferSize = 16 * 1024;
    private final String fileName;

    public FileTestCallable(String fileName, String binaryPath, String content, PageManager pageManager) {
        this.content = content;
        this.pageManager = pageManager;
        this.binaryPath = binaryPath;
        this.fileName = fileName;
    }

    @Override
    public String call() throws Exception {
        PagedRandomAccessFile randomAccessFile = getRandomFile(this.fileName, pageManager);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(content.getBytes());
        byte[] buffer = new byte[bufferSize];
        int length = inputStream.read(buffer);
        while (length != -1) {
            randomAccessFile.write(buffer, 0, length);
            length = inputStream.read(buffer, 0, length);
        }
        inputStream.close();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        randomAccessFile.seekForRead(0);
        length = randomAccessFile.read(buffer);
        while (length != -1) {
            outputStream.write(buffer, 0, length);
            length = randomAccessFile.read(buffer, 0, length);
        }
        randomAccessFile.close();
        outputStream.close();
        return new String(outputStream.toByteArray());
    }

    private PagedRandomAccessFile getRandomFile(String fileName, PageManager pageManager) throws IOException {
        File randomFile = new File(this.binaryPath + "/" + fileName);
        if (!randomFile.exists()) {
            Assert.assertTrue(randomFile.createNewFile());
        }
        return new PagedRandomAccessFile(randomFile.getAbsolutePath(), pageManager);
    }
}
