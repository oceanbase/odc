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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.Validate;

import com.alibaba.fastjson.JSONObject;
import com.oceanbase.odc.core.sql.execute.cache.PageManager.Page;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Random access files with paged cache
 *
 * @author yh263208
 * @date 2021-11-29 21:29
 * @since ODC_release_3.2.2
 * @see java.io.Closeable
 */
@Slf4j
public class PagedRandomAccessFile implements Closeable {

    private final ThreadLocal<LogicPointer> logicReadPointer;
    private final ThreadLocal<LogicPointer> logicWritePointer;
    private final MetaInfo metaInfo;
    private final PageManager pageManager;
    private final int pageSize = PageManager.STORAGE_LAYER_PAGE_SIZE_BYTE;
    private final String filePath;
    private final IndexedLinkedPageList pageBuffer = new IndexedLinkedPageList();
    private final int maxBufferSize;
    private final List<Page> preCreatedPages = new LinkedList<>();
    private final int preCreatedPageSize;

    public PagedRandomAccessFile(@NonNull String filePath, @NonNull PageManager pageManager) throws IOException {
        this.pageManager = pageManager;
        this.metaInfo = MetaInfo.of(filePath);
        this.logicReadPointer = ThreadLocal.withInitial(() -> LogicPointer.of(0));
        this.logicWritePointer =
                ThreadLocal.withInitial(() -> LogicPointer.copy(this.metaInfo.getLastLogicPagePointer()));
        this.filePath = filePath;
        int capacity = Math.min((pageManager.getMaxPageCountInMem() - 1) / 2, 16);
        this.maxBufferSize = capacity / 2;
        this.preCreatedPageSize = capacity - this.maxBufferSize;
    }

    public void seekForWrite(int position) {
        Validate.isTrue(position >= 0, "Position can not be negative");
        this.logicWritePointer.get().seek(Math.min(position, length()));
    }

    public String getPath() {
        return this.filePath;
    }

    public void seekForRead(int position) {
        Validate.isTrue(position >= 0, "Position can not be negative");
        this.logicReadPointer.get().seek(Math.min(position, length()));
    }

    public void write(@NonNull byte[] buffer) throws IOException {
        write(buffer, 0, buffer.length);
    }

    public synchronized void write(@NonNull byte[] buffer, int offset, int length) throws IOException {
        if (buffer.length <= offset) {
            throw new IOException("Offset can not bigger than buffer's length");
        }
        int actualLength = Math.min(buffer.length - offset, length);
        int[] logicPageIds = getLogicPageIds(actualLength, this.logicWritePointer.get());
        Map<Integer, Page> physicalPageId2Page = getPhysicalPagesForWrite(logicPageIds);
        if (logicPageIds.length != physicalPageId2Page.size()) {
            throw new IOException("Affect logic page's size is not equal to physical page size");
        }
        int pointer = offset;
        for (int logicPageId : logicPageIds) {
            int seekPostion = 0;
            if (logicPageId == this.logicWritePointer.get().getLogicPageId()) {
                seekPostion = this.logicWritePointer.get().getOffset();
            }
            int copyLength = Math.min(buffer.length - pointer, pageSize - seekPostion);
            Page physicalPage = nullSafeFindPage(physicalPageId2Page, this.metaInfo.getPhysicalPageId(logicPageId));
            try {
                physicalPage.seekForWrite(seekPostion);
                physicalPage.write(buffer, pointer, copyLength);
            } finally {
                physicalPage.remove();
            }
            pointer += copyLength;
        }
        List<Page> targets =
                physicalPageId2Page.values().stream().filter(p -> !pageBuffer.contains(p)).collect(Collectors.toList());
        if (!targets.isEmpty()) {
            pageManager.modify(targets);
        }
        this.logicWritePointer.get().seek(this.logicWritePointer.get().getLogicPosition() + actualLength);
        if (this.logicWritePointer.get().compareTo(this.metaInfo.getLastLogicPagePointer()) > 0) {
            this.metaInfo.getLastLogicPagePointer().seek(this.logicWritePointer.get().getLogicPosition());
        }
    }

    public int read(@NonNull byte[] buffer) throws IOException {
        return read(buffer, 0, buffer.length);
    }

    public int read(@NonNull byte[] buffer, int offset, int length) throws IOException {
        if (buffer.length < offset) {
            throw new IOException("Offset can not bigger than buffer's length");
        }
        int bufferCapacity = Math.min(buffer.length - offset, length);
        int fileCapacity = length() - this.logicReadPointer.get().getLogicPosition();
        int actualLength = Math.min(fileCapacity, bufferCapacity);

        int[] logicPageIds = getLogicPageIds(actualLength, this.logicReadPointer.get());
        Map<Integer, Page> physicalPageId2Page = getPhysicalPagesForRead(logicPageIds);
        int pointer = offset;
        for (int logicPageId : logicPageIds) {
            int seekPostion = 0;
            if (logicPageId == this.logicReadPointer.get().getLogicPageId()) {
                seekPostion = this.logicReadPointer.get().getOffset();
            }
            int copyLength = Math.min(buffer.length - pointer, pageSize - seekPostion);
            Page physicalPage = nullSafeFindPage(physicalPageId2Page, this.metaInfo.getPhysicalPageId(logicPageId));
            try {
                physicalPage.seekForRead(seekPostion);
                physicalPage.read(buffer, pointer, copyLength);
            } finally {
                physicalPage.remove();
            }
            pointer += copyLength;
        }
        this.logicReadPointer.get().seek(this.logicReadPointer.get().getLogicPosition() + actualLength);
        if (actualLength == 0) {
            return -1;
        }
        return actualLength;
    }

    public int length() {
        return this.metaInfo.getLastLogicPagePointer().getLogicPosition();
    }

    @Override
    public void close() throws IOException {
        pageManager.modify(pageBuffer);
        MetaInfo.write(this.filePath, this.metaInfo);
    }

    public void remove() {
        this.logicReadPointer.remove();
        this.logicWritePointer.remove();
    }

    private int getAffectPageCount(int size, LogicPointer logicPointer) {
        if (size == 0) {
            return 0;
        }
        int currentPageCapacity = pageSize - Math.min(logicPointer.getOffset(), pageSize);
        int sizeExceptCurrentPage = size - currentPageCapacity;
        if (sizeExceptCurrentPage < 0) {
            return 1;
        }
        int affectPageCount = sizeExceptCurrentPage / pageSize + 1;
        if (sizeExceptCurrentPage % pageSize != 0) {
            affectPageCount++;
        }
        return affectPageCount;
    }

    private int[] getLogicPageIds(int size, LogicPointer logicPointer) {
        int pageCount = getAffectPageCount(size, logicPointer);
        int beginPageId = logicPointer.getLogicPageId();
        if (logicPointer.getOffset() >= pageSize) {
            beginPageId++;
        }
        int[] returnVal = new int[pageCount];
        for (int i = 0; i < pageCount; i++) {
            returnVal[i] = beginPageId++;
        }
        return returnVal;
    }

    private Map<Integer, Page> getPhysicalPagesForWrite(int[] logicPageIds) throws IOException {
        List<Integer> existsPhysicalPageIds = new LinkedList<>();
        List<Integer> nonExistsLogicPageIds = new LinkedList<>();
        for (int logicPageId : logicPageIds) {
            if (this.metaInfo.exists(logicPageId)) {
                existsPhysicalPageIds.add(this.metaInfo.getPhysicalPageId(logicPageId));
            } else {
                nonExistsLogicPageIds.add(logicPageId);
            }
        }
        Map<Integer, Page> id2Page = new HashMap<>();
        if (!nonExistsLogicPageIds.isEmpty()) {
            Iterator<Integer> nonExistsPageIter = nonExistsLogicPageIds.iterator();
            Iterator<Page> preCreatedPageIter = preCreatedPages.iterator();
            while (nonExistsPageIter.hasNext() && preCreatedPageIter.hasNext()) {
                Page preCreatedPage = preCreatedPageIter.next();
                this.metaInfo.putPageId(nonExistsPageIter.next(), preCreatedPage.getPhysicalPageId());
                id2Page.putIfAbsent(preCreatedPage.getPhysicalPageId(), preCreatedPage);
                nonExistsPageIter.remove();
                preCreatedPageIter.remove();
            }
            if (preCreatedPages.size() == 0) {
                List<Page> createdPages = pageManager.create(nonExistsLogicPageIds.size() + preCreatedPageSize);
                Iterator<Page> createdPageIter = createdPages.iterator();
                while (createdPageIter.hasNext() && preCreatedPages.size() < preCreatedPageSize) {
                    preCreatedPages.add(createdPageIter.next());
                    createdPageIter.remove();
                }
                if (createdPages.size() != nonExistsLogicPageIds.size()) {
                    throw new IllegalStateException("Unknown error, created pages size is illegal");
                }
                for (int i = 0; i < createdPages.size(); i++) {
                    Page target = createdPages.get(i);
                    Integer logicPageId = nonExistsLogicPageIds.get(i);
                    this.metaInfo.putPageId(logicPageId, target.getPhysicalPageId());
                    id2Page.putIfAbsent(target.getPhysicalPageId(), target);
                }
            }
        }
        int removableCount = maxBufferSize;
        if (!existsPhysicalPageIds.isEmpty()) {
            List<Integer> nonCachedPageIds = existsPhysicalPageIds.stream().filter(pageId -> {
                if (!pageBuffer.contains((int) pageId)) {
                    return true;
                }
                Page target = pageBuffer.findByIdWithLRU(pageId);
                if (target == null) {
                    return true;
                }
                id2Page.putIfAbsent(target.getPhysicalPageId(), target);
                return false;
            }).collect(Collectors.toList());
            removableCount -= (existsPhysicalPageIds.size() - nonCachedPageIds.size());
            if (!nonCachedPageIds.isEmpty()) {
                pageManager.get(nonCachedPageIds).forEach(p -> id2Page.putIfAbsent(p.getPhysicalPageId(), p));
            }
        }
        List<Page> removedPages = new LinkedList<>();
        for (Page target : id2Page.values()) {
            if (removableCount <= 0) {
                break;
            }
            if (pageBuffer.contains(target)) {
                continue;
            }
            if (pageBuffer.size() >= maxBufferSize) {
                Page removedPage = pageBuffer.removeLast();
                removedPages.add(removedPage);
                removableCount--;
            }
            pageBuffer.addFirst(target);
        }
        if (!removedPages.isEmpty()) {
            pageManager.modify(removedPages);
        }
        return id2Page;
    }

    private Page nullSafeFindPage(@NonNull Map<Integer, Page> id2Page, int physicalPageId) {
        Page target = id2Page.get(physicalPageId);
        if (target == null) {
            throw new NullPointerException("Can not find page with id " + physicalPageId);
        }
        return target;
    }

    private Map<Integer, Page> getPhysicalPagesForRead(int[] logicPageIds) throws IOException {
        List<Integer> physicalPageIdList = new LinkedList<>();
        for (int affectLogicPageId : logicPageIds) {
            physicalPageIdList.add(this.metaInfo.getPhysicalPageId(affectLogicPageId));
        }
        Map<Integer, Page> id2Page = new HashMap<>();
        List<Integer> nonCachedPageIds = physicalPageIdList.stream().filter(pageId -> {
            if (!pageBuffer.contains((int) pageId)) {
                return true;
            }
            Page target = pageBuffer.findByIdWithLRU(pageId);
            if (target == null) {
                return true;
            }
            id2Page.putIfAbsent(target.getPhysicalPageId(), target);
            return false;
        }).collect(Collectors.toList());
        if (!nonCachedPageIds.isEmpty()) {
            pageManager.get(nonCachedPageIds).forEach(p -> id2Page.putIfAbsent(p.getPhysicalPageId(), p));
        }
        return id2Page;
    }

    /**
     * Inner position object, used to map postion to logic address
     *
     * @author yh263208
     * @date 2021-11-29 21:41
     * @since ODC_release_3.2.2
     */
    @Setter
    @Getter
    private static class LogicPointer implements Comparable<LogicPointer> {
        private int logicPageId;
        private int offset;

        public LogicPointer() {
            this.logicPageId = 0;
            this.offset = 0;
        }

        public static LogicPointer copy(@NonNull PagedRandomAccessFile.LogicPointer logicPointer) {
            return new LogicPointer(logicPointer.getLogicPageId(), logicPointer.getOffset());
        }

        public static LogicPointer of(int logicPosition) {
            return new LogicPointer(logicPosition);
        }

        public void seek(int logicPosition) {
            init(logicPosition);
        }

        public int getLogicPosition() {
            return this.logicPageId * PageManager.STORAGE_LAYER_PAGE_SIZE_BYTE + this.offset;
        }

        private LogicPointer(int logicPosition) {
            init(logicPosition);
        }

        private LogicPointer(int logicPageId, int offset) {
            this.logicPageId = logicPageId;
            this.offset = offset;
        }

        private void init(int logicPosition) {
            Validate.isTrue(logicPosition >= 0, "Position can not be negative");
            this.logicPageId = logicPosition / PageManager.STORAGE_LAYER_PAGE_SIZE_BYTE;
            this.offset = logicPosition % PageManager.STORAGE_LAYER_PAGE_SIZE_BYTE;
        }

        @Override
        public int compareTo(@NonNull PagedRandomAccessFile.LogicPointer that) {
            if (this.logicPageId > that.logicPageId) {
                return 1;
            } else if (this.logicPageId == that.logicPageId) {
                return Integer.compare(this.offset, that.offset);
            }
            return -1;
        }
    }

    /**
     * Meta info for {@link PagedRandomAccessFile}
     *
     * @author yh263208
     * @date 2021-12-01 17:13
     * @since ODC_release_3.2.1
     */
    @Getter
    @Setter
    private static class MetaInfo {
        private Map<Integer, Integer> pageTable;
        private LogicPointer lastLogicPagePointer;

        public MetaInfo() {
            pageTable = new ConcurrentHashMap<>();
            this.lastLogicPagePointer = LogicPointer.of(0);
        }

        public static MetaInfo of(@NonNull String filePath) throws IOException {
            File metaInfoFile = getFile(filePath);
            String content = FileUtils.readFileToString(metaInfoFile);
            MetaInfo metaInfo = JSONObject.parseObject(content, MetaInfo.class);
            if (metaInfo == null) {
                return new MetaInfo();
            }
            Map<Integer, Integer> customPageTable = metaInfo.getPageTable();
            Map<Integer, Integer> pageTable = new ConcurrentHashMap<>();
            metaInfo.setPageTable(pageTable);
            if (customPageTable != null) {
                for (Map.Entry<Integer, Integer> entry : customPageTable.entrySet()) {
                    pageTable.putIfAbsent(entry.getKey(), entry.getValue());
                }
            }
            if (metaInfo.getLastLogicPagePointer() == null) {
                metaInfo.setLastLogicPagePointer(LogicPointer.of(0));
            }
            return metaInfo;
        }

        public static void write(@NonNull String filePath, @NonNull MetaInfo metaInfo) throws IOException {
            File metaInfoFile = getFile(filePath);
            String jsonString = JSONObject.toJSONString(metaInfo);
            FileUtils.write(metaInfoFile, jsonString);
        }

        public int getPhysicalPageId(int logicPageId) {
            Integer physicalPageId = this.pageTable.get(logicPageId);
            if (physicalPageId == null) {
                throw new NullPointerException("Physical page is not found for logic page " + logicPageId);
            }
            return physicalPageId;
        }

        public boolean exists(int logicPageId) {
            return this.pageTable.containsKey(logicPageId);
        }

        public boolean putPageId(int logicPageId, int physicalPageId) {
            if (this.pageTable.containsKey(logicPageId)) {
                throw new IllegalArgumentException("LogicPage " + logicPageId + " has been mapped to physical page "
                        + this.pageTable.get(logicPageId));
            }
            return this.pageTable.putIfAbsent(logicPageId, physicalPageId) != null;
        }

        private static File getFile(String filePath) throws FileNotFoundException {
            File metaInfoFile = new File(filePath);
            if (!metaInfoFile.exists()) {
                throw new FileNotFoundException(filePath);
            } else if (!metaInfoFile.isFile()) {
                throw new IllegalArgumentException(filePath + " is not a file");
            }
            return metaInfoFile;
        }
    }

}
