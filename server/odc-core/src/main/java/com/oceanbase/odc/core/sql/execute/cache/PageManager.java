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

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Validate;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Paging manager, used to manage paging data. Including the swap-in and swap-out logic between
 * memory pages and disk pages, as well as the read, write and update of memory pages
 *
 * @author yh263208
 * @date 2021-11-26 16:05
 * @since ODC_release_3.2.2
 */
@Slf4j
public class PageManager implements Closeable {
    /**
     * Storage layer page size
     */
    public static final int STORAGE_LAYER_PAGE_SIZE_BYTE = 1024 * 32;
    /**
     * The {@code FileBaseBinaryDataManager} writes binary data into several files. In order to avoid
     * too many files, write multiple binary data into one file to reduce small files. This value is the
     * maximum size (Byte) of a data file
     */
    public static final int MAX_SINGLE_FILE_SIZE_IN_BYTES = 64 * 1024 * 1024;
    /**
     * Internal random access file, through this file to achieve the underlying random file access
     */
    private final File workingDirectory;
    @Getter
    private final int maxPageCountInMem;
    private final Map<Long, File> offsetCount2StorageFile = new ConcurrentHashMap<>();
    private final Map<Long, Lock> offsetCount2FileLock = new ConcurrentHashMap<>();
    private final IndexedLinkedPageList pagesInMemory = new IndexedLinkedPageList();
    private final AtomicInteger pageIdGenerator = new AtomicInteger(0);
    private volatile boolean isClosed = false;
    private final int maxSingleFileSize;
    private final static int MAX_RETRY_COUNT = 3;
    private final Semaphore pageCountSemaphore;
    private final static int TRY_LOCK_TIMEOUT_SECONDS = 3;

    public PageManager(@NonNull String workingDir) throws IOException {
        this(workingDir, 512);
    }

    public PageManager(@NonNull String workingDir, int maxPageCountInMem) throws IOException {
        this(workingDir, maxPageCountInMem, MAX_SINGLE_FILE_SIZE_IN_BYTES);
    }

    public PageManager(@NonNull String workingDir, int maxPageCountInMem, int maxSingleFileSize) throws IOException {
        Validate.isTrue(maxPageCountInMem > 0, "PageCountInMem can not be negative");
        this.maxPageCountInMem = maxPageCountInMem;
        this.workingDirectory = new File(workingDir);
        if (!this.workingDirectory.exists()) {
            throw new FileNotFoundException("Input path does not exist, workingDir " + workingDir);
        }
        if (!this.workingDirectory.isDirectory()) {
            throw new IllegalArgumentException("Input string is not a directory, workingDir " + workingDir);
        }
        Validate.isTrue(maxSingleFileSize > 0, "MaxSingleFileSize can not be negative");
        int maxPageCountInSingleFile = maxSingleFileSize / STORAGE_LAYER_PAGE_SIZE_BYTE;
        if (maxSingleFileSize % STORAGE_LAYER_PAGE_SIZE_BYTE != 0) {
            maxPageCountInSingleFile++;
        }
        this.maxSingleFileSize = maxPageCountInSingleFile * STORAGE_LAYER_PAGE_SIZE_BYTE;
        this.pageCountSemaphore = new Semaphore(maxPageCountInMem);
    }

    public Page create() throws IOException {
        closedCheck();
        return innerInsert(Page.emptyPage(pageIdGenerator.getAndIncrement()), false, null);
    }

    public List<Page> create(int pageCount) throws IOException {
        closedCheck();
        Validate.isTrue(pageCount > 0, "Page Count can not be negative");
        List<Page> createdPages = new LinkedList<>();
        for (int i = 0; i < pageCount; i++) {
            createdPages.add(Page.emptyPage(pageIdGenerator.getAndIncrement()));
        }
        return innerInsert(createdPages, page -> false, null);
    }

    public Page get(int pageId) throws IOException {
        closedCheck();
        if (!isPageExists(pageId)) {
            throw new NullPointerException("Page with Id " + pageId + " does not exist");
        }
        if (isPageExistsInMemory(pageId)) {
            Page returnVal = consumePageInMemory(pageId, false, null);
            if (returnVal != null) {
                return returnVal;
            }
        }
        int retryCount = MAX_RETRY_COUNT;
        while (retryCount-- > 0) {
            Optional<Page> optionalPage = swapIn(pageId);
            if (optionalPage.isPresent()) {
                return optionalPage.get();
            }
            if (!isPageExistsInMemory(pageId)) {
                continue;
            }
            Page returnVal = consumePageInMemory(pageId, false, null);
            if (returnVal != null) {
                return returnVal;
            }
        }
        throw new IllegalStateException("Failed to get page " + pageId + ", Reason: Can not swap in any pages");
    }

    public List<Page> get(@NonNull Collection<Integer> pageIds) throws IOException {
        closedCheck();
        List<Page> returnVal = new LinkedList<>();
        List<Integer> pageIdsNotInMemory = pageIds.stream().filter(pageId -> {
            if (!isPageExistsInMemory(pageId)) {
                return true;
            }
            Page target = consumePageInMemory(pageId, false, null);
            if (target == null) {
                return true;
            }
            returnVal.add(target);
            return false;
        }).collect(Collectors.toList());
        List<List<Integer>> splitedPageIds = splitPagesByStep(pageIdsNotInMemory, this.maxPageCountInMem);
        for (List<Integer> pageIdList : splitedPageIds) {
            Map<Integer, Page> id2SwappedInPage = swapIn(pageIdList).stream().collect(
                    Collectors.toMap(Page::getPhysicalPageId, page -> page));
            Iterator<Integer> iterator = pageIdList.iterator();
            while (iterator.hasNext()) {
                Integer pageId = iterator.next();
                Page target = id2SwappedInPage.get(pageId);
                if (target != null) {
                    returnVal.add(target);
                    iterator.remove();
                    continue;
                }
                if (!isPageExistsInMemory(pageId)) {
                    continue;
                }
                target = consumePageInMemory(pageId, false, null);
                if (target == null) {
                    continue;
                }
                returnVal.add(target);
                iterator.remove();
            }
            for (Integer pageId : pageIdList) {
                returnVal.add(get(pageId));
            }
        }
        return returnVal;
    }

    public Page modify(@NonNull Page page) throws IOException {
        closedCheck();
        int pageId = page.getPhysicalPageId();
        if (!isPageExists(pageId)) {
            throw new NullPointerException("Page with Id " + pageId + " does not exist");
        }
        if (isPageExistsInMemory(pageId)) {
            Page pageModified = consumePageInMemory(pageId, true, target -> deepCopyPage(page, target));
            if (pageModified != null) {
                return pageModified;
            }
        }
        int retryCount = MAX_RETRY_COUNT;
        while (retryCount-- > 0) {
            Optional<Page> optionalPage = swapIn(pageId, true, target -> deepCopyPage(page, target));
            if (optionalPage.isPresent()) {
                return optionalPage.get();
            }
            if (!isPageExistsInMemory(pageId)) {
                continue;
            }
            Page pageToBeModified = consumePageInMemory(pageId, true, target -> deepCopyPage(page, target));
            if (pageToBeModified != null) {
                return pageToBeModified;
            }
            log.warn("Page does not exist in mem, will retry, pageId={}, retryCount={}", pageId, retryCount);
        }
        throw new IllegalStateException("Failed to modify page " + pageId + ", Reason: Can not swap in any pages");
    }

    public List<Page> modify(@NonNull Collection<Page> pages) throws IOException {
        closedCheck();
        List<Page> returnVal = new LinkedList<>();
        List<Page> pagesToBeModified = pages.stream().filter(page -> {
            if (!isPageExistsInMemory(page.getPhysicalPageId())) {
                return true;
            }
            Page pageToBeModified = consumePageInMemory(page.getPhysicalPageId(), true, target -> {
                deepCopyPage(page, target);
                returnVal.add(target);
            });
            return pageToBeModified == null;
        }).collect(Collectors.toList());

        List<List<Page>> pagesNotInMemory = splitPagesByStep(pagesToBeModified);
        for (List<Page> subPageList : pagesNotInMemory) {
            int retryCount = MAX_RETRY_COUNT;
            Map<Integer, Page> id2Page =
                    subPageList.stream().collect(Collectors.toMap(Page::getPhysicalPageId, page -> page));
            while (retryCount-- > 0 && id2Page.size() > 0) {
                swapIn(id2Page.keySet(), page -> true, swappedInPages -> {
                    if (log.isDebugEnabled()) {
                        log.debug("Pages in, pageIds={}",
                                swappedInPages.stream().map(Page::getPhysicalPageId).collect(Collectors.toList()));
                    }
                    for (Page target : swappedInPages) {
                        int pageId = target.getPhysicalPageId();
                        Page srcPage = id2Page.get(pageId);
                        if (srcPage == null) {
                            throw new IllegalStateException("Page is not found by id " + pageId);
                        }
                        deepCopyPage(srcPage, target);
                        id2Page.remove(pageId);
                        returnVal.add(target);
                    }
                });
                Iterator<Map.Entry<Integer, Page>> iterator = id2Page.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<Integer, Page> entry = iterator.next();
                    Integer pageId = entry.getKey();
                    if (!isPageExistsInMemory(pageId)) {
                        continue;
                    }
                    Page pageToBeModified = consumePageInMemory(pageId, true, target -> {
                        deepCopyPage(entry.getValue(), target);
                        returnVal.add(target);
                    });
                    if (pageToBeModified == null) {
                        continue;
                    }
                    iterator.remove();
                }
            }
            if (retryCount < 0 && id2Page.size() > 0) {
                log.warn("Failed to modify pages, leftPageIds={}", id2Page.keySet());
                throw new IllegalStateException("Failed to modify pages, try " + MAX_RETRY_COUNT + " times");
            }
        }
        return returnVal;
    }

    @Override
    public synchronized void close() throws IOException {
        if (this.isClosed) {
            return;
        }
        this.pageCountSemaphore.drainPermits();
        int retryCount = MAX_RETRY_COUNT;
        while (retryCount-- > 0) {
            swapOut(this.pagesInMemory);
            if (this.pagesInMemory.size() <= 0) {
                break;
            }
        }
        this.isClosed = true;
        if (this.pagesInMemory.size() != 0) {
            for (Page item : this.pagesInMemory) {
                log.warn("Failed to swap out the page, pageId={}, ifLocked={}", item.getPhysicalPageId(),
                        item.modifyLock.isLocked());
            }
            throw new IllegalStateException("Failed to close PageManager, Reason: some pages is locked");
        }
        log.info("Page manager closed successfully, workingDir={}", this.workingDirectory.getAbsolutePath());
    }

    public synchronized void flush() throws IOException {
        List<Page> pages = swapOut(this.pagesInMemory);
        if (!pages.isEmpty()) {
            this.pageCountSemaphore.release(pages.size());
        }
        if (log.isDebugEnabled()) {
            log.debug("PageManager has been flushed successfully, filePath={}, cacheCount={}, liveCount={}",
                    this.workingDirectory.getAbsolutePath(), this.pagesInMemory.size(), this.pagesInMemory.size());
        }
    }

    public int size() {
        return this.maxPageCountInMem - this.pageCountSemaphore.availablePermits();
    }

    @Override
    public String toString() {
        return "PageManager: " + this.workingDirectory.getAbsolutePath();
    }

    private File createStorageFile() throws IOException {
        File destFile = new File(workingDirectory.getAbsolutePath() + "/" + generateFileName());
        if (destFile.exists()) {
            throw new IllegalStateException("Unknown error...");
        }
        if (!destFile.createNewFile()) {
            throw new IOException("Failed to create a file, fileName " + destFile.getAbsolutePath());
        }
        return destFile;
    }

    private String generateFileName() {
        return this.getClass().getSimpleName().toLowerCase() + "_" + UUID.randomUUID().toString().replaceAll("-", "")
                + ".data";
    }

    private void closedCheck() {
        if (this.isClosed) {
            throw new IllegalStateException("Page manager is closed");
        }
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

    private <T> List<List<T>> splitPagesByStep(@NonNull List<T> items) {
        return splitPagesByStep(items, getSuitableStep());
    }

    private int getSuitableStep() {
        int lockCount = this.maxPageCountInMem / 2;
        if (lockCount == 0) {
            lockCount++;
        }
        return lockCount;
    }

    private long getStorageFileOffsetCount(int pageId) {
        long offset = (long) pageId * STORAGE_LAYER_PAGE_SIZE_BYTE;
        return offset / maxSingleFileSize;
    }

    private int getSeekOffsetInStorageFile(int pageId) {
        long offsetCount = getStorageFileOffsetCount(pageId);
        long seek = (long) pageId * STORAGE_LAYER_PAGE_SIZE_BYTE - offsetCount * maxSingleFileSize;
        return (int) seek;
    }

    private Map<Long, List<Integer>> getOffsetCount2PageIds(List<Integer> pageIds) {
        Map<Long, List<Integer>> returnVal = new HashMap<>();
        for (int pageId : pageIds) {
            long offsetCount = getStorageFileOffsetCount(pageId);
            List<Integer> target = returnVal.computeIfAbsent(offsetCount, integer -> new LinkedList<>());
            target.add(pageId);
        }
        return returnVal;
    }

    private Map<Long, List<Page>> getOffsetCount2Page(List<Page> pages) {
        Map<Long, List<Page>> returnVal = new HashMap<>();
        for (Page page : pages) {
            long offsetCount = getStorageFileOffsetCount(page.getPhysicalPageId());
            List<Page> target = returnVal.computeIfAbsent(offsetCount, integer -> new LinkedList<>());
            target.add(page);
        }
        return returnVal;
    }

    private Lock getStorageFileLock(long offsetCount) {
        return this.offsetCount2FileLock.computeIfAbsent(offsetCount, item -> new ReentrantLock());
    }

    private boolean acquireLock(@NonNull Lock lock, int timeout, @NonNull TimeUnit timeUnit) {
        Validate.isTrue(timeout > 0, "TimeOut can not be negative");
        try {
            return lock.tryLock(timeout, timeUnit);
        } catch (Exception e) {
            log.warn("Failed to acquire the lock", e);
        }
        return false;
    }

    private boolean acquireLock(@NonNull Lock lock) {
        try {
            return lock.tryLock();
        } catch (Exception e) {
            log.warn("Failed to acquire the lock", e);
        }
        return false;
    }

    private File getStorageFile(long offsetCount) throws IOException {
        File storageFile = this.offsetCount2StorageFile.get(offsetCount);
        if (storageFile != null) {
            return storageFile;
        }
        Lock lock = getStorageFileLock(offsetCount);
        if (!acquireLock(lock, TRY_LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Failed to acquire the lock");
        }
        try {
            storageFile = this.offsetCount2StorageFile.get(offsetCount);
            if (storageFile != null) {
                return storageFile;
            }
            if (log.isDebugEnabled()) {
                log.debug("The storage file does not exist, offsetCount={}", offsetCount);
            }
            storageFile = createStorageFile();
            if (log.isDebugEnabled()) {
                log.debug("The storage file is created successfully, filePath={}, offsetCount={}",
                        storageFile.getAbsolutePath(), offsetCount);
            }
            this.offsetCount2StorageFile.putIfAbsent(offsetCount, storageFile);
            return storageFile;
        } catch (Exception e) {
            log.warn("Failed to create a storage file", e);
            throw e;
        } finally {
            lock.unlock();
        }
    }

    private List<Page> swapIn(Collection<Integer> pageIds) throws IOException {
        return swapIn(pageIds, page -> false, null);
    }

    private Optional<Page> swapIn(Integer pageId) throws IOException {
        return swapIn(pageId, false, null);
    }

    private Optional<Page> swapIn(Integer pageId, boolean ifLockPage, Consumer<Page> consumer) throws IOException {
        List<Page> returnVal = swapIn(Collections.singletonList(pageId), p -> ifLockPage, pages -> {
            if (pages.size() > 1) {
                throw new IllegalStateException("Illegal page size, size is " + pages.size());
            }
            if (pages.size() == 0) {
                return;
            }
            if (consumer != null) {
                consumer.accept(pages.get(0));
            }
        });
        if (returnVal == null || returnVal.size() > 1) {
            throw new IllegalStateException("Unknown error");
        }
        if (returnVal.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(returnVal.get(0));
    }

    private List<Page> swapIn(Collection<Integer> pageIds, Predicate<Page> ifLockPage, Consumer<List<Page>> consumer)
            throws IOException {
        if (log.isDebugEnabled()) {
            log.debug("Pages are going to be swapped in, pageIds={}", pageIds);
        }
        List<Integer> actualSwappedInPageIds =
                pageIds.stream().filter(pageId -> !isPageExistsInMemory(pageId)).sorted().collect(Collectors.toList());
        Validate.isTrue(actualSwappedInPageIds.size() <= this.maxPageCountInMem,
                "Too many pages to be swapped in " + actualSwappedInPageIds.size());
        if (actualSwappedInPageIds.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, List<Integer>> offsetCount2PageIds = getOffsetCount2PageIds(actualSwappedInPageIds);
        List<Page> pagesTobeSwappedIn = new ArrayList<>(actualSwappedInPageIds.size());
        for (Map.Entry<Long, List<Integer>> entry : offsetCount2PageIds.entrySet()) {
            long offsetCount = entry.getKey();
            List<Integer> subPageIds = entry.getValue();
            File storageFile = this.offsetCount2StorageFile.get(offsetCount);
            if (storageFile == null) {
                throw new NullPointerException("Page with id " + Collections.min(subPageIds) + " does not exist");
            }
            try (RandomAccessFile internalFile = new RandomAccessFile(storageFile, "r")) {
                int maxPageId = Collections.max(subPageIds);
                int minFileSize =
                        getSeekOffsetInStorageFile(maxPageId) + STORAGE_LAYER_PAGE_SIZE_BYTE;
                int intervalSize = minFileSize - (int) internalFile.length();
                if (intervalSize > 0) {
                    throw new NullPointerException("Page with id " + maxPageId + " does not exist");
                }
                int currentPosition = 0;
                for (Integer pageId : subPageIds) {
                    int destPosition = getSeekOffsetInStorageFile(pageId);
                    if (currentPosition != destPosition) {
                        internalFile.seek(destPosition);
                    }
                    byte[] content = new byte[STORAGE_LAYER_PAGE_SIZE_BYTE];
                    internalFile.read(content);
                    currentPosition = destPosition + STORAGE_LAYER_PAGE_SIZE_BYTE;
                    pagesTobeSwappedIn.add(Page.newPage(pageId, content));
                }
            }
        }
        return innerInsert(pagesTobeSwappedIn, ifLockPage, consumer);
    }

    private List<Page> swapOut(List<Page> pagesToBeSwappedOut) throws IOException {
        if (log.isDebugEnabled()) {
            log.debug("Pages are going to be swapped out, pageIds={}",
                    pagesToBeSwappedOut.stream().map(Page::getPhysicalPageId).collect(Collectors.toList()));
        }
        List<Page> actualSwappedPages = pagesToBeSwappedOut.stream()
                .filter(page -> isPageExistsInMemory(page.getPhysicalPageId()))
                .sorted(Comparator.comparingInt(Page::getPhysicalPageId)).collect(Collectors.toList());
        if (actualSwappedPages.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, List<Page>> offsetCount2Pages = getOffsetCount2Page(actualSwappedPages);
        List<Page> returnVal = new LinkedList<>();
        for (Map.Entry<Long, List<Page>> entry : offsetCount2Pages.entrySet()) {
            List<Page> subPages = entry.getValue();
            int maxPageId =
                    Collections.max(subPages, Comparator.comparingInt(Page::getPhysicalPageId)).getPhysicalPageId();
            int minFileSize = getSeekOffsetInStorageFile(maxPageId) + STORAGE_LAYER_PAGE_SIZE_BYTE;
            long offsetCount = entry.getKey();
            Lock lock = getStorageFileLock(offsetCount);
            if (!acquireLock(lock, TRY_LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Failed to get the file lock");
            }
            if (log.isDebugEnabled()) {
                log.debug("StorageFile is locked, offsetCount={}", offsetCount);
            }
            try (RandomAccessFile internalFile = new RandomAccessFile(getStorageFile(offsetCount), "rw")) {
                long currentPosition = 0;
                long fileLength = internalFile.length();
                int intervalSize = minFileSize - (int) fileLength;
                if (intervalSize > 0) {
                    byte[] placeholderBuffer = new byte[intervalSize];
                    internalFile.seek(fileLength);
                    internalFile.write(placeholderBuffer);
                    currentPosition = fileLength + intervalSize;
                }
                for (Page pageToBeSwappedOut : subPages) {
                    int pageId = pageToBeSwappedOut.getPhysicalPageId();
                    if (!isPageExistsInMemory(pageId)) {
                        if (log.isDebugEnabled()) {
                            log.debug("Page is not in memory, pageId={}", pageId);
                        }
                        continue;
                    }
                    if (!acquireLock(pageToBeSwappedOut.modifyLock)) {
                        if (log.isDebugEnabled()) {
                            log.debug("Page is locked, give up swapping out, pageId={}", pageId);
                        }
                        continue;
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("Page is locked, pageId={}", pageId);
                    }
                    try {
                        if (removePageInMemory(pageId) == null) {
                            log.warn("Failed to remove a page, pageId={}", pageId);
                            continue;
                        }
                        int destPosition = getSeekOffsetInStorageFile(pageId);
                        if (currentPosition != destPosition) {
                            internalFile.seek(destPosition);
                        }
                        internalFile.write(pageToBeSwappedOut.content);
                        currentPosition = destPosition + STORAGE_LAYER_PAGE_SIZE_BYTE;
                        returnVal.add(pageToBeSwappedOut);
                    } finally {
                        pageToBeSwappedOut.modifyLock.unlock();
                        if (log.isDebugEnabled()) {
                            log.debug("Page is unlocked, pageId={}", pageToBeSwappedOut.getPhysicalPageId());
                        }
                    }
                }
            } finally {
                lock.unlock();
                if (log.isDebugEnabled()) {
                    log.debug("StorageFile is unlocked, offsetCount={}", offsetCount);
                }
            }
        }
        return returnVal;
    }

    private synchronized List<Page> swapOut(int swapOutCount) throws IOException {
        Validate.isTrue(swapOutCount > 0, "SwapOutCount can not be negative");
        List<Page> pagesToBeSwappedOut = new ArrayList<>(swapOutCount);
        Iterator<Page> iterator = this.pagesInMemory.reverseIterator();
        while (iterator.hasNext()) {
            Page swappedOutPage = iterator.next();
            if (!swappedOutPage.modifyLock.isLocked()) {
                pagesToBeSwappedOut.add(swappedOutPage);
            }
            if (pagesToBeSwappedOut.size() >= swapOutCount) {
                break;
            }
        }
        return swapOut(pagesToBeSwappedOut);
    }

    private void deepCopyPage(@NonNull Page srcPage, @NonNull Page destPage) {
        int maxPosition = Math.min(srcPage.content.length, destPage.content.length);
        System.arraycopy(srcPage.content, 0, destPage.content, 0, maxPosition);
    }

    private boolean isPageExistsInMemory(int pageId) {
        return this.pagesInMemory.contains(pageId);
    }

    private boolean isPageExists(int pageId) {
        return pageId < pageIdGenerator.get();
    }

    private Page consumePageInMemory(int pageId, boolean ifLock, Consumer<Page> consumer) {
        Page target = this.pagesInMemory.findByIdWithLRU(pageId);
        if (target == null) {
            return null;
        }
        if (ifLock) {
            if (!acquireLock(target.modifyLock, TRY_LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Failed to lock the page " + target.getPhysicalPageId());
            }
            if (log.isDebugEnabled()) {
                log.debug("Page is locked, pageId={}", pageId);
            }
        }
        try {
            if (ifLock && !isPageExistsInMemory(pageId)) {
                return null;
            }
            target.seekForRead(0);
            target.seekForWrite(0);
            if (consumer != null) {
                consumer.accept(target);
            }
            return target;
        } finally {
            if (ifLock) {
                target.modifyLock.unlock();
                if (log.isDebugEnabled()) {
                    log.debug("Page is unlocked, pageId={}", target.getPhysicalPageId());
                }
            }
            target.remove();
        }
    }

    private Page addPageInMemory(Page page, boolean ifNeedAcquire) {
        closedCheck();
        if (ifNeedAcquire) {
            boolean tryResult = this.pageCountSemaphore.tryAcquire();
            if (!tryResult) {
                throw new OutOfBoundsException(this.pagesInMemory.size(), this.maxPageCountInMem, page);
            }
        } else {
            if (this.pagesInMemory.size() >= maxPageCountInMem) {
                throw new OutOfBoundsException(this.pagesInMemory.size(), this.maxPageCountInMem, page);
            }
        }
        int pageId = page.getPhysicalPageId();
        try {
            if (isPageExistsInMemory(pageId)) {
                this.pageCountSemaphore.release();
                return null;
            }
            boolean result = this.pagesInMemory.addFirst(page);
            if (!result) {
                this.pageCountSemaphore.release();
                return null;
            }
            if (log.isDebugEnabled()) {
                log.debug("New pages are added, pageId={}", pageId);
            }
            return page;
        } catch (Exception e) {
            log.warn("Failed to add the page, pageId={}", pageId, e);
            this.pageCountSemaphore.release();
            throw e;
        }
    }

    private Page removePageInMemory(@NonNull int deletePageId) {
        closedCheck();
        Page page = this.pagesInMemory.removeById(deletePageId);
        if (page == null) {
            return null;
        }
        if (log.isDebugEnabled()) {
            log.debug("Page has been deleted, pageId={}", page.getPhysicalPageId());
        }
        return page;
    }

    private void unLockPages(@NonNull Collection<Page> lockedPages) {
        List<Integer> failedPageIds = lockedPages.stream().filter(page -> {
            try {
                page.modifyLock.unlock();
                return false;
            } catch (Exception e) {
                log.warn("Failed to unlock the page, pageId={}", page.getPhysicalPageId(), e);
            }
            return true;
        }).map(Page::getPhysicalPageId).collect(Collectors.toList());
        if (!failedPageIds.isEmpty()) {
            throw new IllegalStateException("Failed to unlock pages, pageId " + failedPageIds);
        }
        if (log.isDebugEnabled()) {
            log.debug("Pages are unlocked, pageIds={}",
                    lockedPages.stream().map(Page::getPhysicalPageId).collect(Collectors.toList()));
        }
    }

    private List<Page> innerInsert(@NonNull List<Page> insertedPages, Predicate<Page> ifLockPage,
            Consumer<List<Page>> consumer) throws IOException {
        if (insertedPages.isEmpty()) {
            return insertedPages;
        }
        List<Page> lockedPages = insertedPages.stream().filter(ifLockPage).peek(page -> {
            if (!acquireLock(page.modifyLock)) {
                throw new IllegalStateException("Failed to lock the page " + page.getPhysicalPageId());
            }
        }).collect(Collectors.toList());
        if (log.isDebugEnabled() && !lockedPages.isEmpty()) {
            log.debug("Pages are locked, pageIds={}",
                    lockedPages.stream().map(Page::getPhysicalPageId).collect(Collectors.toList()));
        }
        try {
            List<Page> returnValue = new LinkedList<>();
            Iterator<Page> iterator = insertedPages.iterator();
            while (iterator.hasNext() && size() < this.maxPageCountInMem) {
                Page newPage = iterator.next();
                try {
                    newPage = addPageInMemory(newPage, true);
                    if (newPage != null) {
                        returnValue.add(newPage);
                    }
                    iterator.remove();
                } catch (OutOfBoundsException e) {
                    log.warn("Pages in memory is out of bound and need to be swapped out", e);
                    break;
                }
            }
            if (insertedPages.size() == 0) {
                if (consumer != null) {
                    consumer.accept(returnValue);
                }
                return returnValue;
            }
            List<List<Page>> splitedCreatedPages = splitPagesByStep(insertedPages);
            for (List<Page> subPageList : splitedCreatedPages) {
                int swapOutCount = subPageList.size();
                int retryCount = MAX_RETRY_COUNT;
                int beginIndex = 0;
                while (retryCount-- > 0 && swapOutCount > 0) {
                    List<Page> swapOutPages = swapOut(swapOutCount);
                    int actualSwapOutCount = swapOutPages.size();
                    if (log.isDebugEnabled()) {
                        log.debug("Pages out, pageIds={}",
                                swapOutPages.stream().map(Page::getPhysicalPageId).collect(Collectors.toList()));
                    }
                    if (actualSwapOutCount > swapOutCount) {
                        this.pageCountSemaphore.release(actualSwapOutCount);
                        throw new IllegalStateException("Unknown error, wrong swap out count " + actualSwapOutCount);
                    }
                    if (actualSwapOutCount == 0) {
                        continue;
                    }
                    for (int j = 0; j < actualSwapOutCount; j++) {
                        try {
                            Page newPage = subPageList.get(beginIndex + j);
                            newPage = addPageInMemory(newPage, false);
                            if (newPage != null) {
                                returnValue.add(newPage);
                            }
                        } catch (Exception exception) {
                            log.warn("Failed to add page, unknown error", exception);
                            this.pageCountSemaphore.release(actualSwapOutCount - j - 1);
                            throw exception;
                        }
                    }
                    beginIndex += actualSwapOutCount;
                    swapOutCount -= actualSwapOutCount;
                }
                if (retryCount < 0 && swapOutCount > 0) {
                    throw new IllegalStateException("Failed to create pages, try " + MAX_RETRY_COUNT + " times");
                }
            }
            if (consumer != null) {
                consumer.accept(returnValue);
            }
            return returnValue;
        } finally {
            if (!lockedPages.isEmpty()) {
                unLockPages(lockedPages);
            }
        }
    }

    private Page innerInsert(@NonNull Page pageToBeInserted, boolean ifLock, Consumer<Page> consumer)
            throws IOException {
        int pageId = pageToBeInserted.getPhysicalPageId();
        if (ifLock) {
            if (!acquireLock(pageToBeInserted.modifyLock)) {
                throw new IllegalStateException("Failed to lock the page " + pageId);
            }
            if (log.isDebugEnabled()) {
                log.debug("Page is locked, pageId={}", pageId);
            }
        }
        try {
            if (size() < this.maxPageCountInMem) {
                try {
                    Page newPage = addPageInMemory(pageToBeInserted, true);
                    if (newPage == null) {
                        return null;
                    }
                    if (consumer != null) {
                        consumer.accept(newPage);
                    }
                    return newPage;
                } catch (OutOfBoundsException e) {
                    log.warn("Pages in memory is out of bound and need to be swapped out", e);
                }
            }
            int retryCount = MAX_RETRY_COUNT;
            while (retryCount-- > 0) {
                int pageCount = size() + 1 - this.maxPageCountInMem;
                if (pageCount > 0) {
                    List<Page> pageList = swapOut(pageCount);
                    int swapCount = pageList.size();
                    if (log.isDebugEnabled()) {
                        log.debug("Page out, pageIds={}",
                                pageList.stream().map(Page::getPhysicalPageId).collect(Collectors.toList()));
                    }
                    if (swapCount == 1) {
                        Page newPage = addPageInMemory(pageToBeInserted, false);
                        if (newPage == null) {
                            return null;
                        }
                        if (consumer != null) {
                            consumer.accept(newPage);
                        }
                        return newPage;
                    } else if (swapCount > 1) {
                        this.pageCountSemaphore.release(swapCount);
                        throw new IllegalStateException("Unknown error, wrong swap out count " + swapCount);
                    }
                } else {
                    try {
                        Page newPage = addPageInMemory(pageToBeInserted, true);
                        if (newPage == null) {
                            return null;
                        }
                        if (consumer != null) {
                            consumer.accept(newPage);
                        }
                        return newPage;
                    } catch (OutOfBoundsException e) {
                        log.warn("Pages in memory is out of bound and need to be swapped out", e);
                    }
                }
                if (log.isDebugEnabled()) {
                    log.debug("Failed to create page, will retry, pageId={}", pageId);
                }
            }
            throw new IllegalStateException("Failed to insert a page, try " + MAX_RETRY_COUNT + " times");
        } finally {
            if (ifLock) {
                pageToBeInserted.modifyLock.unlock();
                if (log.isDebugEnabled()) {
                    log.debug("Page is unlocked, pageId={}", pageToBeInserted.getPhysicalPageId());
                }
            }
        }
    }

    /**
     * Page Instance
     *
     * @author yh263208
     * @date 2021-11-26 16:09
     * @since ODC-release_3.2.2
     */
    @Getter
    @ToString(of = "physicalPageId")
    @EqualsAndHashCode(of = "physicalPageId")
    public static class Page {
        private final int physicalPageId;
        private final byte[] content;
        private final ThreadLocal<Integer> readPointer;
        private final ThreadLocal<Integer> writePointer;
        private final ReentrantLock modifyLock = new ReentrantLock();

        private static Page newPage(int pageId, @NonNull byte[] content) {
            return new Page(pageId, content);
        }

        public static Page emptyPage(int pageId) {
            return new Page(pageId, new byte[STORAGE_LAYER_PAGE_SIZE_BYTE]);
        }

        private Page(int physicalPageId, @NonNull byte[] content) {
            Validate.isTrue(physicalPageId >= 0, "PageId can not be negative");
            Validate.isTrue(content.length == STORAGE_LAYER_PAGE_SIZE_BYTE,
                    "Content's length is illegal, has to be equal to " + STORAGE_LAYER_PAGE_SIZE_BYTE);
            this.physicalPageId = physicalPageId;
            this.content = content;
            this.readPointer = ThreadLocal.withInitial(() -> 0);
            this.writePointer = ThreadLocal.withInitial(() -> 0);
        }

        public void seekForWrite(int position) {
            Validate.isTrue(position >= 0, "Position can not be negative");
            this.writePointer.set(Math.min(position, STORAGE_LAYER_PAGE_SIZE_BYTE));
        }

        public void seekForRead(int position) {
            Validate.isTrue(position >= 0, "Position can not be negative");
            this.readPointer.set(Math.min(position, STORAGE_LAYER_PAGE_SIZE_BYTE));
        }

        public void write(@NonNull byte[] buffer) throws IOException {
            write(buffer, 0, buffer.length);
        }

        public void write(@NonNull byte[] buffer, int offset, int length) throws IOException {
            if (buffer.length <= offset) {
                throw new IOException("Offset can not bigger than buffer's length");
            }
            int actualLength = Math.min(buffer.length - offset, length);
            int capacity = this.content.length - this.writePointer.get();
            if (actualLength > capacity) {
                throw new IOException("Capacity " + capacity + " is smaller than length " + actualLength);
            }
            System.arraycopy(buffer, offset, this.content, this.writePointer.get(), actualLength);
            this.writePointer.set(this.writePointer.get() + actualLength);
        }

        public int read(@NonNull byte[] buffer) throws IOException {
            return read(buffer, 0, buffer.length);
        }

        public int read(@NonNull byte[] buffer, int offset, int length) throws IOException {
            if (buffer.length <= offset) {
                throw new IOException("Offset can not bigger than buffer's length");
            }
            int bufferCapacity = Math.min(buffer.length - offset, length);
            int pageCapacity = this.content.length - this.readPointer.get();
            int actualLength = Math.min(pageCapacity, bufferCapacity);
            System.arraycopy(this.content, this.readPointer.get(), buffer, offset, actualLength);
            this.readPointer.set(this.readPointer.get() + actualLength);
            if (actualLength == 0) {
                return -1;
            }
            return actualLength;
        }

        public void remove() {
            this.readPointer.remove();
            this.writePointer.remove();
        }
    }


    public static class OutOfBoundsException extends RuntimeException {
        private static final long serialVersionUID = 234122996896267687L;

        public OutOfBoundsException() {
            super();
        }

        public OutOfBoundsException(int current, int max, @NonNull Page page) {
            super(String.format("Page %d is out of bounds, current size %d, max size %d", page.getPhysicalPageId(),
                    current, max));
        }

        public OutOfBoundsException(int current, int max) {
            super(String.format("Current size %d, max size %d", current, max));
        }

        public OutOfBoundsException(String s) {
            super(s);
        }
    }

}
