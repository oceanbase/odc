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

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.oceanbase.odc.core.sql.execute.cache.PageManager.Page;

/**
 * Test cases for {@link IndexedLinkedPageList}
 *
 * @author yh263208
 * @date 2021-12-07 13:29
 * @since ODC_release_3.2.2
 */
public class IndexedLinkedPageListTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testAppendPage() {
        IndexedLinkedPageList pages = new IndexedLinkedPageList();
        for (int i = 0; i < 10; i++) {
            Assert.assertTrue(pages.add(Page.emptyPage(i)));
        }
        Assert.assertFalse(pages.isEmpty());
        Assert.assertEquals(10, pages.size());
        int i = 0;
        for (Page page : pages) {
            Assert.assertEquals(i++, page.getPhysicalPageId());
        }
    }

    @Test
    public void testAddAnExistsPage() {
        IndexedLinkedPageList pages = new IndexedLinkedPageList();
        for (int i = 0; i < 10; i++) {
            Assert.assertTrue(pages.add(Page.emptyPage(i)));
        }
        Assert.assertFalse(pages.addLast(Page.emptyPage(0)));
    }

    @Test
    public void testAddFirstPage() {
        IndexedLinkedPageList pages = new IndexedLinkedPageList();
        for (int i = 0; i < 10; i++) {
            Assert.assertTrue(pages.addFirst(Page.emptyPage(i)));
        }
        Assert.assertFalse(pages.isEmpty());
        Assert.assertEquals(10, pages.size());
        int i = 10;
        for (Page page : pages) {
            Assert.assertEquals(--i, page.getPhysicalPageId());
        }
    }

    @Test
    public void testAddFirstAnExistsPage() {
        IndexedLinkedPageList pages = new IndexedLinkedPageList();
        for (int i = 0; i < 10; i++) {
            Assert.assertTrue(pages.addFirst(Page.emptyPage(i)));
        }
        Assert.assertFalse(pages.addFirst(Page.emptyPage(0)));
    }

    @Test
    public void testAddAllPage() {
        IndexedLinkedPageList pages = new IndexedLinkedPageList();
        for (int i = 0; i < 10; i++) {
            Assert.assertTrue(pages.addFirst(Page.emptyPage(i)));
        }
        List<Page> pageList = Arrays.asList(Page.emptyPage(11), Page.emptyPage(12));
        Assert.assertTrue(pages.addAll(pageList));
        Assert.assertEquals(12, pages.size());
    }

    @Test
    public void testGetFirstPage() {
        IndexedLinkedPageList pages = new IndexedLinkedPageList();
        Assert.assertNull(pages.getFirst());
        for (int i = 0; i < 10; i++) {
            Assert.assertTrue(pages.addLast(Page.emptyPage(i)));
        }
        Assert.assertEquals(0, pages.getFirst().getPhysicalPageId());
    }

    @Test
    public void testGetLastPage() {
        IndexedLinkedPageList pages = new IndexedLinkedPageList();
        Assert.assertNull(pages.getLast());
        for (int i = 0; i < 10; i++) {
            Assert.assertTrue(pages.addFirst(Page.emptyPage(i)));
        }
        Assert.assertEquals(0, pages.getLast().getPhysicalPageId());
    }

    @Test
    public void testRemovePage() {
        IndexedLinkedPageList pages = new IndexedLinkedPageList();
        for (int i = 0; i < 10; i++) {
            Assert.assertTrue(pages.add(Page.emptyPage(i)));
        }
        Assert.assertTrue(pages.remove(Page.emptyPage(3)));
        Assert.assertEquals(9, pages.size());
        int counter = 0;
        for (Page page : pages) {
            counter++;
            Assert.assertNotEquals(3, page.getPhysicalPageId());
        }
        Assert.assertEquals(9, counter);
    }

    @Test
    public void testRemoveLastPage() {
        IndexedLinkedPageList pages = new IndexedLinkedPageList();
        for (int i = 0; i < 10; i++) {
            Assert.assertTrue(pages.add(Page.emptyPage(i)));
        }
        for (int i = 9; i >= 0; i--) {
            Assert.assertTrue(pages.remove(Page.emptyPage(i)));
        }
        Assert.assertEquals(0, pages.size());
        int counter = 0;
        for (Page page : pages) {
            counter++;
        }
        Assert.assertEquals(0, counter);
    }

    @Test
    public void testRemoveFirstPage() {
        IndexedLinkedPageList pages = new IndexedLinkedPageList();
        for (int i = 0; i < 10; i++) {
            Assert.assertTrue(pages.add(Page.emptyPage(i)));
        }
        for (int i = 0; i < 10; i++) {
            Assert.assertTrue(pages.remove(Page.emptyPage(i)));
        }
        Assert.assertEquals(0, pages.size());
        int counter = 0;
        for (Page page : pages) {
            counter++;
        }
        Assert.assertEquals(0, counter);
    }

    @Test
    public void testRemoveLastMethod() {
        IndexedLinkedPageList pages = new IndexedLinkedPageList();
        for (int i = 0; i < 10; i++) {
            Assert.assertTrue(pages.add(Page.emptyPage(i)));
        }
        for (int i = 9; i >= 0; i--) {
            Page removedPage = pages.removeLast();
            Assert.assertEquals(i, removedPage.getPhysicalPageId());
        }
        Assert.assertNull(pages.removeLast());
        Assert.assertTrue(pages.isEmpty());
        int counter = 0;
        for (Page page : pages) {
            counter++;
        }
        Assert.assertEquals(0, counter);
    }

    @Test
    public void testReverseIteratorMethod() {
        IndexedLinkedPageList pages = new IndexedLinkedPageList();
        for (int i = 0; i < 10; i++) {
            Assert.assertTrue(pages.add(Page.emptyPage(i)));
        }
        Iterator<Page> iter = pages.reverseIterator();
        int i = 9;
        while (iter.hasNext()) {
            Page page = iter.next();
            Assert.assertEquals(i--, page.getPhysicalPageId());
        }
    }

    @Test
    public void testRemoveFirstMethod() {
        IndexedLinkedPageList pages = new IndexedLinkedPageList();
        for (int i = 0; i < 10; i++) {
            Assert.assertTrue(pages.add(Page.emptyPage(i)));
        }
        for (int i = 0; i < 10; i++) {
            Page removedPage = pages.removeFirst();
            Assert.assertEquals(i, removedPage.getPhysicalPageId());
        }
        Assert.assertNull(pages.removeFirst());
        Assert.assertTrue(pages.isEmpty());
        int counter = 0;
        for (Page page : pages) {
            counter++;
        }
        Assert.assertEquals(0, counter);
    }

    @Test
    public void testClearMethod() {
        IndexedLinkedPageList pages = new IndexedLinkedPageList();
        for (int i = 0; i < 10; i++) {
            Assert.assertTrue(pages.add(Page.emptyPage(i)));
        }
        pages.clear();
        Assert.assertNull(pages.getLast());
    }

    @Test
    public void testGetPageUsingIndex() {
        IndexedLinkedPageList pages = new IndexedLinkedPageList();
        for (int i = 0; i < 10; i++) {
            Assert.assertTrue(pages.add(Page.emptyPage(i)));
        }
        for (int i = 0; i < 10; i++) {
            Page page = pages.get(i);
            Assert.assertEquals(i, page.getPhysicalPageId());
        }
        Assert.assertNull(pages.get(10000));
    }

    @Test
    public void testContainsMethod() {
        IndexedLinkedPageList pages = new IndexedLinkedPageList();
        for (int i = 0; i < 10; i++) {
            Assert.assertTrue(pages.add(Page.emptyPage(i)));
        }
        for (int i = 0; i < 10; i++) {
            Assert.assertTrue(pages.contains(Page.emptyPage(i)));
        }
        Assert.assertFalse(pages.contains(Page.emptyPage(1000)));
    }
}
