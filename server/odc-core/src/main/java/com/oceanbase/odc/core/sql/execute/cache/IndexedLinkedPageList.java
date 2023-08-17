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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang3.Validate;

import com.oceanbase.odc.core.sql.execute.cache.PageManager.Page;

import lombok.Getter;
import lombok.NonNull;

/**
 * Indexed list, which adds an index table for checking each element to increase access speed
 *
 * @author yh263208
 * @date 2021-12-07 10:32
 * @since ODC_release_3.2.2
 */
public class IndexedLinkedPageList implements List<Page> {

    private final ElementNode headNode;
    private final AtomicInteger elementCounter = new AtomicInteger(0);
    private final Lock writeLock = new ReentrantLock();
    private final Map<Integer, ElementNode> pagedId2Element = new ConcurrentHashMap<>();
    private ElementNode lastNode;

    public IndexedLinkedPageList() {
        this.headNode = ElementNode.empty();
        lastNode = headNode;
    }

    @Override
    public int size() {
        return this.elementCounter.get();
    }

    @Override
    public boolean isEmpty() {
        return elementCounter.get() == 0;
    }

    public boolean addFirst(Page page) {
        Validate.notNull(page, "Page can not be null");
        this.writeLock.lock();
        try {
            if (this.pagedId2Element.containsKey(page.getPhysicalPageId())) {
                return false;
            }
            ElementNode elementNode = new ElementNode(page);
            ElementNode theNext = headNode.nextNode;
            headNode.nextNode = elementNode;
            elementNode.priorNode = headNode;
            elementNode.nextNode = theNext;
            if (theNext == null) {
                lastNode = elementNode;
            } else {
                theNext.priorNode = elementNode;
            }
            this.pagedId2Element.putIfAbsent(page.getPhysicalPageId(), elementNode);
            this.elementCounter.incrementAndGet();
            return true;
        } finally {
            this.writeLock.unlock();
        }
    }

    @Override
    public boolean add(Page page) {
        Validate.notNull(page, "Page can not be null");
        this.writeLock.lock();
        try {
            if (this.pagedId2Element.containsKey(page.getPhysicalPageId())) {
                return false;
            }
            ElementNode elementNode = new ElementNode(page);
            lastNode.nextNode = elementNode;
            elementNode.priorNode = lastNode;
            lastNode = elementNode;
            this.pagedId2Element.putIfAbsent(page.getPhysicalPageId(), elementNode);
            this.elementCounter.incrementAndGet();
            return true;
        } finally {
            this.writeLock.unlock();
        }
    }

    public boolean addLast(Page page) {
        return add(page);
    }

    @Override
    public boolean addAll(Collection<? extends Page> pages) {
        Validate.notNull(pages, "Pages can not be null");
        if (pages == this) {
            throw new IllegalArgumentException("Can not add your self");
        }
        this.writeLock.lock();
        try {
            for (Page page : pages) {
                add(page);
            }
            return true;
        } finally {
            this.writeLock.unlock();
        }
    }

    public Page getLast() {
        if (this.elementCounter.get() == 0) {
            return null;
        }
        return this.lastNode.getContent();
    }

    public Page getFirst() {
        if (this.elementCounter.get() == 0) {
            return null;
        }
        ElementNode elementNode = this.headNode.nextNode;
        if (elementNode != null) {
            return elementNode.getContent();
        }
        return null;
    }

    public Page removeFirst() {
        if (this.elementCounter.get() == 0) {
            return null;
        }
        this.writeLock.lock();
        try {
            if (this.elementCounter.get() == 0) {
                return null;
            }
            ElementNode deletedNode = headNode.nextNode;
            ElementNode nextNode = deletedNode.nextNode;
            headNode.nextNode = nextNode;
            if (nextNode != null) {
                nextNode.priorNode = headNode;
            } else {
                lastNode = headNode;
            }
            deletedNode.nextNode = null;
            deletedNode.priorNode = null;
            pagedId2Element.remove(deletedNode.content.getPhysicalPageId());
            this.elementCounter.decrementAndGet();
            return deletedNode.getContent();
        } finally {
            this.writeLock.unlock();
        }
    }

    public Page removeLast() {
        if (this.elementCounter.get() == 0) {
            return null;
        }
        this.writeLock.lock();
        try {
            if (this.elementCounter.get() == 0) {
                return null;
            }
            ElementNode deletedNode = lastNode;
            lastNode = deletedNode.priorNode;
            lastNode.nextNode = null;
            deletedNode.priorNode = null;
            pagedId2Element.remove(deletedNode.content.getPhysicalPageId());
            this.elementCounter.decrementAndGet();
            return deletedNode.getContent();
        } finally {
            this.writeLock.unlock();
        }
    }

    @Override
    public boolean remove(Object o) {
        if (!(o instanceof Page)) {
            return false;
        }
        Page pageToBeRemoved = (Page) o;
        return removeById(pageToBeRemoved.getPhysicalPageId()) != null;
    }

    public Page removeById(int pageId) {
        if (!this.pagedId2Element.containsKey(pageId)) {
            return null;
        }
        this.writeLock.lock();
        try {
            if (!this.pagedId2Element.containsKey(pageId)) {
                return null;
            }
            ElementNode elementNode = this.pagedId2Element.get(pageId);
            if (elementNode == null) {
                throw new NullPointerException("Element is null for pageId " + pageId);
            }
            ElementNode priorNode = elementNode.priorNode;
            ElementNode nextNode = elementNode.nextNode;
            priorNode.nextNode = nextNode;
            if (nextNode != null) {
                nextNode.priorNode = priorNode;
            } else {
                lastNode = priorNode;
            }
            elementNode.nextNode = null;
            elementNode.priorNode = null;
            this.pagedId2Element.remove(pageId);
            this.elementCounter.decrementAndGet();
            return elementNode.getContent();
        } finally {
            this.writeLock.unlock();
        }
    }

    @Override
    public void clear() {
        this.pagedId2Element.clear();
        this.headNode.nextNode = null;
        this.lastNode = headNode;
        this.elementCounter.set(0);
    }

    @Override
    public Page get(int index) {
        if (index < 0 || index >= this.elementCounter.get()) {
            return null;
        }
        int subIndex = 0;
        for (ElementNode pointer = this.headNode.nextNode; pointer != null; pointer = pointer.nextNode) {
            if ((subIndex++) == index) {
                return pointer.getContent();
            }
        }
        return null;
    }

    public Page findById(int pageId) {
        if (!this.pagedId2Element.containsKey(pageId)) {
            return null;
        }
        ElementNode elementNode = this.pagedId2Element.get(pageId);
        if (elementNode == null) {
            throw new NullPointerException("Element is null for pageId " + pageId);
        }
        return elementNode.getContent();
    }

    public Page findByIdWithLRU(int pageId) {
        if (!this.pagedId2Element.containsKey(pageId)) {
            return null;
        }
        this.writeLock.lock();
        try {
            if (!this.pagedId2Element.containsKey(pageId)) {
                return null;
            }
            ElementNode elementNode = this.pagedId2Element.get(pageId);
            if (elementNode == null) {
                throw new NullPointerException("Element is null for pageId " + pageId);
            }
            ElementNode priorNode = elementNode.priorNode;
            ElementNode nextNode = elementNode.nextNode;
            priorNode.nextNode = nextNode;
            if (nextNode != null) {
                nextNode.priorNode = priorNode;
            } else {
                lastNode = priorNode;
            }
            ElementNode theNext = headNode.nextNode;
            headNode.nextNode = elementNode;
            elementNode.priorNode = headNode;
            elementNode.nextNode = theNext;
            if (theNext == null) {
                lastNode = elementNode;
            } else {
                theNext.priorNode = elementNode;
            }
            return elementNode.getContent();
        } finally {
            this.writeLock.unlock();
        }
    }

    @Override
    public boolean contains(Object o) {
        if (!(o instanceof Page)) {
            return false;
        }
        Page destPage = (Page) o;
        return this.pagedId2Element.containsKey(destPage.getPhysicalPageId());
    }

    public boolean contains(int pageId) {
        return this.pagedId2Element.containsKey(pageId);
    }

    @Override
    public Iterator<Page> iterator() {
        return new PageListIterator(this.headNode);
    }

    public Iterator<Page> reverseIterator() {
        return new PageListReverseIterator(this.headNode, this.lastNode);
    }

    @Override
    public Object[] toArray() {
        throw new UnsupportedOperationException("toArray");
    }

    @Override
    public <T> T[] toArray(T[] a) {
        throw new UnsupportedOperationException("toArray");
    }

    @Override
    public Page remove(int index) {
        throw new UnsupportedOperationException("remove");
    }

    @Override
    public Page set(int index, Page element) {
        throw new UnsupportedOperationException("set");
    }

    @Override
    public void add(int index, Page element) {
        throw new UnsupportedOperationException("add");
    }

    @Override
    public int indexOf(Object o) {
        throw new UnsupportedOperationException("indexOf");
    }

    @Override
    public int lastIndexOf(Object o) {
        throw new UnsupportedOperationException("lastIndexOf");
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        throw new UnsupportedOperationException("containsAll");
    }

    @Override
    public boolean addAll(int index, Collection<? extends Page> c) {
        throw new UnsupportedOperationException("addAll");
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException("removeAll");
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException("retainAll");
    }

    @Override
    public ListIterator<Page> listIterator() {
        throw new UnsupportedOperationException("listIterator");
    }

    @Override
    public ListIterator<Page> listIterator(int index) {
        throw new UnsupportedOperationException("listIterator");
    }

    @Override
    public List<Page> subList(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException("subList");
    }

    private static class ElementNode {
        @Getter
        private final Page content;
        private ElementNode nextNode;
        private ElementNode priorNode;

        public ElementNode(@NonNull Page content) {
            this.content = content;
            this.nextNode = null;
            this.priorNode = null;
        }

        public static ElementNode empty() {
            return new ElementNode();
        }

        private ElementNode() {
            this.content = null;
            this.nextNode = null;
            this.priorNode = null;
        }
    }

    public static class PageListReverseIterator implements Iterator<Page> {
        private ElementNode iterPointer;
        private final ElementNode headNode;

        private PageListReverseIterator(@NonNull ElementNode headNode, @NonNull ElementNode lastNode) {
            this.iterPointer = lastNode;
            this.headNode = headNode;
        }

        @Override
        public boolean hasNext() {
            return this.iterPointer != headNode && this.iterPointer != null;
        }

        @Override
        public Page next() {
            Page returnVal = this.iterPointer.getContent();
            this.iterPointer = this.iterPointer.priorNode;
            return returnVal;
        }
    }

    public static class PageListIterator implements Iterator<Page> {
        private ElementNode iterPointer;

        private PageListIterator(@NonNull ElementNode headNode) {
            this.iterPointer = headNode.nextNode;
        }

        @Override
        public boolean hasNext() {
            return this.iterPointer != null;
        }

        @Override
        public Page next() {
            Page returnVal = this.iterPointer.getContent();
            this.iterPointer = this.iterPointer.nextNode;
            return returnVal;
        }
    }

}
