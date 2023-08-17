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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.util.ResourceUtils;

import com.oceanbase.odc.core.sql.execute.cache.PageManager.Page;

import lombok.NonNull;

/**
 * Test case for {@link PageManager}
 *
 * @author yh263208
 * @date 2021-11-29 16:09
 * @since ODC_release_3.2.2
 */
public class PageManagerTest {

    private static final String DATA_DIR_NAME = "PageManagerTest".toLowerCase();
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
    public void create_createOneNewPage_createSucceed() throws IOException {
        PageManager pageManager = getPageManager();
        Page page = pageManager.create();
        Assert.assertNotNull(page);
        pageManager.close();
    }

    @Test
    public void create_createNewPagesWithSwappedOut_createSucceed() throws IOException {
        int maxPageCountInMemory = 5;
        PageManager pageManager = getPageManager(maxPageCountInMemory);
        for (int i = 0; i < maxPageCountInMemory; i++) {
            pageManager.create();
        }
        Page page = pageManager.create();
        Assert.assertNotNull(page);
    }

    @Test
    public void create_createNewPagedWithoutSwappedOut_createSucceed() throws IOException {
        int maxPageCountInMemory = 3;
        PageManager pageManager = getPageManager(maxPageCountInMemory);
        List<Page> pages = pageManager.create(maxPageCountInMemory);
        Assert.assertEquals(maxPageCountInMemory, pages.size());
        pageManager.close();
    }

    @Test
    public void create_createNewPageWithSwappedOut_createSucceed() throws IOException {
        int maxPageCountInMemory = 3;
        PageManager pageManager = getPageManager(maxPageCountInMemory);
        List<Page> pages = pageManager.create(5);
        Assert.assertEquals(5, pages.size());
        pageManager.close();
    }

    @Test
    public void get_readPageByPhysicalId_getSucceed() throws IOException {
        PageManager pageManager = getPageManager();
        Page page = pageManager.create();

        Page readPage = pageManager.get(page.getPhysicalPageId());
        Assert.assertEquals(page, readPage);
    }

    @Test
    public void get_readPageWithSwappedIn_getSucceed() throws IOException {
        PageManager pageManager = getPageManager(5);
        List<Page> pages = pageManager.create(10);

        Page oldestPage = pages.get(0);
        Page readPage = pageManager.get(oldestPage.getPhysicalPageId());
        Assert.assertEquals(oldestPage, readPage);
    }

    @Test
    public void get_readNonExistPage_expThrown() throws IOException {
        PageManager pageManager = getPageManager(5);
        List<Page> pages = pageManager.create(10);

        int pageId = 16;
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("Page with Id " + pageId + " does not exist");
        pageManager.get(pageId);
    }

    @Test
    public void get_treadPagesWithSwappedIn_getSucceed() throws IOException {
        PageManager pageManager = getPageManager(5);
        List<Page> pages = pageManager.create(13);
        List<Page> readPages =
                pageManager.get(pages.stream().map(Page::getPhysicalPageId).collect(Collectors.toList()));
        Assert.assertEquals(pages.size(), readPages.size());
    }

    @Test
    public void modify_modifyPageInMemory_modifySucceed() throws IOException {
        PageManager pageManager = getPageManager(5);
        List<Page> pageList = pageManager.create(13);

        Page modifyPage = pageList.get(12);
        String content = "abcdefg1234567";
        modifyPage.seekForWrite(3);
        modifyPage.write(content.getBytes());
        pageManager.modify(modifyPage);

        Page readPage = pageManager.get(modifyPage.getPhysicalPageId());
        byte[] buffer = new byte[content.length()];
        readPage.seekForRead(3);
        readPage.read(buffer);

        Assert.assertEquals(content, new String(buffer));
    }

    @Test
    public void modify_modifyPageInDisk_modifySucceed() throws IOException {
        PageManager pageManager = getPageManager(5);
        List<Page> pageList = pageManager.create(13);

        Page modifyPage = pageList.get(0);
        String content = "abcdefg1234567";
        modifyPage.write(content.getBytes());
        pageManager.modify(modifyPage);

        Page readPage = pageManager.get(modifyPage.getPhysicalPageId());
        byte[] buffer = new byte[content.length()];
        readPage.read(buffer);

        Assert.assertEquals(content, new String(buffer));
    }

    @Test
    public void modify_modifyFullPageInDisk_modifySucceed() throws IOException {
        PageManager pageManager = getPageManager(5);
        List<Page> pageList = pageManager.create(13);

        Page modifyPage = pageList.get(0);
        StringBuilder content = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < PageManager.STORAGE_LAYER_PAGE_SIZE_BYTE; i++) {
            content.append((char) (random.nextInt(26) + 97));
        }
        modifyPage.write(content.toString().getBytes());
        pageManager.modify(modifyPage);

        Page readPage = pageManager.get(modifyPage.getPhysicalPageId());
        byte[] buffer = new byte[content.toString().length()];
        readPage.read(buffer);

        Assert.assertEquals(content.toString(), new String(buffer));
    }

    @Test
    public void modify_modifyPagesInMemory_modifySucceed() throws IOException {
        PageManager pageManager = getPageManager(5);
        List<Page> pageList = pageManager.create(13);

        List<Page> modifiedPages = new LinkedList<>();
        for (int i = 0; i < 5; i++) {
            Page modifyPage = pageList.get(i);
            String content = "abcdefg_" + i;
            modifyPage.write(content.getBytes());
            modifiedPages.add(modifyPage);
        }
        pageManager.modify(modifiedPages);

        for (int i = 0; i < 5; i++) {
            Page tmp = modifiedPages.get(i);
            Page readPage = pageManager.get(tmp.getPhysicalPageId());
            String content = "abcdefg_" + i;
            byte[] buffer = new byte[content.length()];
            readPage.read(buffer);
            Assert.assertEquals(content, new String(buffer));
        }
    }

    @Test
    public void modify_ModifyPagesInDisk_modifySucceed() throws IOException {
        PageManager pageManager = getPageManager(5);
        List<Page> pageList = pageManager.create(13);

        List<Page> modifiedPages = new LinkedList<>();
        for (int i = 0; i < 13; i++) {
            Page modifyPage = pageList.get(i);
            String content = "abcdefg_" + i;
            modifyPage.write(content.getBytes());
            modifiedPages.add(modifyPage);
        }
        pageManager.modify(modifiedPages);

        for (int i = 0; i < 13; i++) {
            Page tmp = modifiedPages.get(i);
            Page readPage = pageManager.get(tmp.getPhysicalPageId());
            String content = "abcdefg_" + i;
            byte[] buffer = new byte[content.length()];
            readPage.read(buffer);
            Assert.assertEquals(content, new String(buffer));
        }
    }

    @Test
    public void get_getFromClosedPageManager_expThrown() throws IOException {
        PageManager pageManager = getPageManager(5);
        List<Page> pageList = pageManager.create(13);

        pageManager.close();
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Page manager is closed");
        pageManager.get(pageList.get(0).getPhysicalPageId());
    }

    @Test
    public void create_createPageConcurrently_createSucceed()
            throws IOException, InterruptedException, TimeoutException, ExecutionException {
        PageManager pageManager = getPageManager(15);
        List<Future<List<Page>>> futureList = new LinkedList<>();
        int createCount = 100;
        for (int i = 0; i < 99; i++) {
            futureList.add(executor.submit(new CreatePageCallable(pageManager, createCount)));
        }

        for (Future<List<Page>> future : futureList) {
            List<Page> pageList = future.get(20, TimeUnit.SECONDS);
            Assert.assertEquals(createCount, pageList.size());
        }
        pageManager.close();
    }

    @Test
    public void create_createMultiPagesConcurrently_createSucceed()
            throws IOException, InterruptedException, TimeoutException, ExecutionException {
        PageManager pageManager = getPageManager(15);
        int eachCreateCount = 24;
        int loopCount = 13;
        List<Future<List<Page>>> futureList = new LinkedList<>();
        for (int i = 0; i < 34; i++) {
            futureList.add(executor.submit(new CreateMultiPagesCallable(pageManager, loopCount, eachCreateCount)));
        }

        for (Future<List<Page>> future : futureList) {
            List<Page> pageList = future.get(20, TimeUnit.SECONDS);
            Assert.assertEquals(loopCount * eachCreateCount, pageList.size());
        }
        pageManager.close();
    }

    @Test
    public void get_getMultiPagesConcurrently_getSucceed()
            throws IOException, InterruptedException, TimeoutException, ExecutionException {
        PageManager pageManager = getPageManager(5);
        Random random = new Random();
        List<Future<List<Page>>> futureList = new LinkedList<>();
        int getCount = 13;
        for (int i = 0; i < 57; i++) {
            List<Page> createdPages = pageManager.create(getCount);
            Future<List<Page>> future;
            if (random.nextBoolean()) {
                future = executor.submit(new GetPageCallable(pageManager,
                        createdPages.stream().map(Page::getPhysicalPageId).collect(Collectors.toList())));
            } else {
                future = executor.submit(new GetMultiPagesCallable(pageManager,
                        createdPages.stream().map(Page::getPhysicalPageId).collect(Collectors.toList()), getCount));
            }
            futureList.add(future);
        }

        for (Future<List<Page>> future : futureList) {
            List<Page> pageList = future.get(20, TimeUnit.SECONDS);
            Assert.assertEquals(getCount, pageList.size());
        }
        pageManager.close();
    }

    @Test
    public void modify_modifyMultiPagesConcurrently_modifySucceed()
            throws IOException, InterruptedException, TimeoutException, ExecutionException {
        PageManager pageManager = getPageManager(30);
        Random random = new Random();
        List<Future<List<Page>>> futureList = new LinkedList<>();
        int modifyCount = 13;
        for (int i = 0; i < 45; i++) {
            List<Page> modifyPage = pageManager.create(modifyCount);
            Future<List<Page>> future;
            if (random.nextBoolean()) {
                future = executor.submit(new ModifyPageCallable(pageManager,
                        modifyPage.stream().map(Page::getPhysicalPageId).collect(Collectors.toList())));
            } else {
                future = executor.submit(new ModifyMultiPagesCallable(pageManager,
                        modifyPage.stream().map(Page::getPhysicalPageId).collect(Collectors.toList()), modifyCount));
            }
            futureList.add(future);
        }
        Map<Integer, String> pageId2Content = new HashMap<>();
        for (Future<List<Page>> future : futureList) {
            List<Page> pageList = future.get(20, TimeUnit.SECONDS);
            Assert.assertEquals(modifyCount, pageList.size());
            for (Page page : pageList) {
                byte[] buffer = new byte[PageManager.STORAGE_LAYER_PAGE_SIZE_BYTE];
                page.read(buffer);
                pageId2Content.putIfAbsent(page.getPhysicalPageId(), new String(buffer));
            }
        }

        for (Map.Entry<Integer, String> entry : pageId2Content.entrySet()) {
            byte[] buffer = new byte[PageManager.STORAGE_LAYER_PAGE_SIZE_BYTE];
            Page page = pageManager.get(entry.getKey());
            page.read(buffer);
            if (!Objects.equals(entry.getValue(), new String(buffer))) {
                System.out.println(entry.getKey());
            }
            Assert.assertEquals(entry.getValue(), new String(buffer));
        }
        pageManager.close();
    }

    @Test
    public void allOperations_createModifyAndGetConcurrently_operateSucceed()
            throws IOException, InterruptedException, TimeoutException, ExecutionException {
        PageManager pageManager = getPageManager(15);
        Random random = new Random();
        List<Future<List<Page>>> getFutureList = new LinkedList<>();
        List<Future<List<Page>>> modifyFutureList = new LinkedList<>();
        List<Future<List<Page>>> createFutureList = new LinkedList<>();
        int modifyCount = 13;
        int getCount = 18;
        int createCount = 15;
        for (int i = 0; i < 100; i++) {
            int randomNum = random.nextInt(100);
            boolean chose = random.nextBoolean();
            if (randomNum < 25) {
                // get or get list
                List<Page> pageList = pageManager.create(getCount);
                Future<List<Page>> future;
                if (chose) {
                    // get
                    future = executor.submit(new GetPageCallable(pageManager,
                            pageList.stream().map(Page::getPhysicalPageId).collect(Collectors.toList())));
                } else {
                    // get list
                    future = executor.submit(new GetMultiPagesCallable(pageManager,
                            pageList.stream().map(Page::getPhysicalPageId).collect(Collectors.toList()), getCount));
                }
                getFutureList.add(future);
            } else if (randomNum < 63) {
                // modify or modify list
                List<Page> pageList = pageManager.create(modifyCount);
                Future<List<Page>> future;
                if (chose) {
                    // modify
                    future = executor.submit(new ModifyPageCallable(pageManager,
                            pageList.stream().map(Page::getPhysicalPageId).collect(Collectors.toList())));
                } else {
                    // modify list
                    future = executor.submit(new ModifyMultiPagesCallable(pageManager,
                            pageList.stream().map(Page::getPhysicalPageId).collect(Collectors.toList()), modifyCount));
                }
                modifyFutureList.add(future);
            } else {
                // create or create list
                Future<List<Page>> future;
                if (chose) {
                    // create
                    future = executor.submit(new CreatePageCallable(pageManager, createCount));
                } else {
                    // create list
                    future = executor.submit(new CreateMultiPagesCallable(pageManager, createCount, createCount));
                }
                createFutureList.add(future);
            }
        }

        for (Future<List<Page>> future : getFutureList) {
            future.get(20, TimeUnit.SECONDS);
        }

        for (Future<List<Page>> future : createFutureList) {
            future.get(20, TimeUnit.SECONDS);
        }

        Map<Integer, String> pageId2Content = new HashMap<>();
        for (Future<List<Page>> future : modifyFutureList) {
            List<Page> pageList = future.get(20, TimeUnit.SECONDS);
            Assert.assertEquals(modifyCount, pageList.size());
            for (Page page : pageList) {
                byte[] buffer = new byte[PageManager.STORAGE_LAYER_PAGE_SIZE_BYTE];
                page.read(buffer);
                pageId2Content.putIfAbsent(page.getPhysicalPageId(), new String(buffer));
            }
        }

        for (Map.Entry<Integer, String> entry : pageId2Content.entrySet()) {
            byte[] buffer = new byte[PageManager.STORAGE_LAYER_PAGE_SIZE_BYTE];
            Page page = pageManager.get(entry.getKey());
            page.read(buffer);
            if (!Objects.equals(entry.getValue(), new String(buffer))) {
                System.out.println(entry.getKey());
            }
            Assert.assertEquals(entry.getValue(), new String(buffer));
        }
        pageManager.close();
    }

    private PageManager getPageManager(int maxPageCount) throws IOException {
        return new PageManager(getBinaryFilePath(), maxPageCount);
    }

    private PageManager getPageManager() throws IOException {
        return new PageManager(getBinaryFilePath());
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


class CreatePageCallable implements Callable<List<Page>> {
    private final int createCount;
    private final PageManager pageManager;

    public CreatePageCallable(PageManager pageManager, int createCount) {
        this.createCount = createCount;
        this.pageManager = pageManager;
    }

    @Override
    public List<Page> call() throws Exception {
        List<Page> pages = new LinkedList<>();
        for (int i = 0; i < this.createCount; i++) {
            Page page = pageManager.create();
            pages.add(page);
        }
        return pages;
    }
}


class CreateMultiPagesCallable implements Callable<List<Page>> {
    private final int loopCount;
    private final int eachCreateCount;
    private final PageManager pageManager;

    public CreateMultiPagesCallable(PageManager pageManager, int loopCount, int eachCreateCount) {
        this.loopCount = loopCount;
        this.eachCreateCount = eachCreateCount;
        this.pageManager = pageManager;
    }

    @Override
    public List<Page> call() throws Exception {
        List<Page> pageList = new LinkedList<>();
        for (int i = 0; i < loopCount; i++) {
            List<Page> pages = pageManager.create(eachCreateCount);
            pageList.addAll(pages);
        }
        return pageList;
    }
}


class GetPageCallable implements Callable<List<Page>> {
    private final List<Integer> pageIds;
    private final PageManager pageManager;

    public GetPageCallable(PageManager pageManager, List<Integer> pageIds) {
        this.pageIds = pageIds;
        this.pageManager = pageManager;
    }

    @Override
    public List<Page> call() throws Exception {
        List<Page> pageList = new LinkedList<>();
        for (Integer pageId : pageIds) {
            Page savedPage = pageManager.get(pageId);
            if (!Objects.equals(savedPage.getPhysicalPageId(), pageId)) {
                throw new IllegalStateException(
                        "Expect page id " + pageId + ", actual page id " + savedPage.getPhysicalPageId());
            }
            pageList.add(savedPage);
        }
        return pageList;
    }
}


class GetMultiPagesCallable implements Callable<List<Page>> {
    private final List<Integer> pageIds;
    private final PageManager pageManager;
    private final int step;

    public GetMultiPagesCallable(PageManager pageManager, List<Integer> pageIds, int step) {
        this.pageIds = pageIds;
        this.pageManager = pageManager;
        this.step = step;
    }

    @Override
    public List<Page> call() throws Exception {
        List<List<Integer>> splitedPageIds = splitPagesByStep(this.pageIds, step);
        List<Page> returnVal = new LinkedList<>();
        for (List<Integer> subPageIds : splitedPageIds) {
            List<Page> savedPages = pageManager.get(subPageIds);
            if (!Objects.equals(subPageIds.size(), savedPages.size())) {
                throw new IllegalStateException(
                        "Expect pages size " + subPageIds.size() + ", actual pages size " + savedPages.size());
            }
            for (Integer subPageId : subPageIds) {
                boolean result = false;
                for (Page subPage : savedPages) {
                    if (subPage.getPhysicalPageId() == subPageId) {
                        returnVal.add(subPage);
                        result = true;
                        break;
                    }
                }
                if (!result) {
                    throw new IllegalStateException("Get wrong page " + subPageId);
                }
            }
        }
        return returnVal;
    }

    private <T> List<List<T>> splitPagesByStep(@NonNull List<T> items, int step) {
        List<List<T>> returnValue = new LinkedList<>();
        int i = 0;
        int pageCount = items.size();
        while (i < pageCount) {
            List<T> subList = new LinkedList<>();
            int j = 0;
            while (j++ < step && i < pageCount) {
                subList.add(items.get(i++));
            }
            returnValue.add(subList);
        }
        return returnValue;
    }
}


class ModifyPageCallable implements Callable<List<Page>> {
    private final List<Integer> pageIds;
    private final PageManager pageManager;

    public ModifyPageCallable(PageManager pageManager, List<Integer> pageIds) {
        this.pageIds = pageIds;
        this.pageManager = pageManager;
    }

    @Override
    public List<Page> call() throws Exception {
        List<Page> pageList = new LinkedList<>();
        for (Integer pageId : pageIds) {
            Page savedPage = pageManager.get(pageId);
            if (!Objects.equals(savedPage.getPhysicalPageId(), pageId)) {
                throw new IllegalStateException(
                        "Expect page id " + pageId + ", actual page id " + savedPage.getPhysicalPageId());
            }
            String content = generateContents(PageManager.STORAGE_LAYER_PAGE_SIZE_BYTE);
            savedPage.write(content.getBytes());
            pageList.add(pageManager.modify(savedPage));
        }
        return pageList;
    }

    private String generateContents(int size) {
        Random random = new Random();
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < size; i++) {
            content.append((char) (random.nextInt(26) + 97));
        }
        return content.toString();
    }
}


class ModifyMultiPagesCallable implements Callable<List<Page>> {
    private final List<Integer> pageIds;
    private final PageManager pageManager;
    private final int step;

    public ModifyMultiPagesCallable(PageManager pageManager, List<Integer> pageIds, int step) {
        this.pageIds = pageIds;
        this.pageManager = pageManager;
        this.step = step;
    }

    @Override
    public List<Page> call() throws Exception {
        List<List<Integer>> splitedPageIds = splitPagesByStep(this.pageIds, step);
        List<Page> returnVal = new LinkedList<>();
        for (List<Integer> subPageIds : splitedPageIds) {
            List<Page> savedPages = pageManager.get(subPageIds);
            if (!Objects.equals(subPageIds.size(), savedPages.size())) {
                throw new IllegalStateException(
                        "Expect pages size " + subPageIds.size() + ", actual pages size " + savedPages.size());
            }
            for (Integer subPageId : subPageIds) {
                boolean result = false;
                for (Page subPage : savedPages) {
                    if (subPage.getPhysicalPageId() == subPageId) {
                        result = true;
                        String content = generateContents(PageManager.STORAGE_LAYER_PAGE_SIZE_BYTE);
                        subPage.write(content.getBytes());
                        break;
                    }
                }
                if (!result) {
                    throw new IllegalStateException("Get wrong page " + subPageId);
                }
            }
            returnVal.addAll(pageManager.modify(savedPages));
        }
        return returnVal;
    }

    private String generateContents(int size) {
        Random random = new Random();
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < size; i++) {
            content.append((char) (random.nextInt(26) + 97));
        }
        return content.toString();
    }

    private <T> List<List<T>> splitPagesByStep(@NonNull List<T> items, int step) {
        List<List<T>> returnValue = new LinkedList<>();
        int i = 0;
        int pageCount = items.size();
        while (i < pageCount) {
            List<T> subList = new LinkedList<>();
            int j = 0;
            while (j++ < step && i < pageCount) {
                subList.add(items.get(i++));
            }
            returnValue.add(subList);
        }
        return returnValue;
    }
}
