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
package com.oceanbase.odc.common.file.zip;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test cases for {@link ZipFileTree}
 *
 * @author yh263208
 * @date 2022-06-30 22:22
 * @since ODC_release_3.4.0
 */
public class ZipFileTreeTest {

    @Test
    public void forEach_countElement_8ElementsReturn() throws IOException {
        ZipFileTree tree = new ZipFileTree(getTestZipFile());
        AtomicInteger counter = new AtomicInteger();
        tree.forEach((integer, element) -> counter.incrementAndGet());

        Assert.assertEquals(8, counter.get());
    }

    @Test
    public void filter_filterAllFile_3FilesReturn() throws IOException {
        ZipFileTree zipFileTree = new ZipFileTree(getTestZipFile());
        List<ZipElement> elementList = zipFileTree.filter(element -> !element.isDirectory());

        Assert.assertEquals(5, elementList.size());
    }

    @Test
    public void getUrl_fileUrl_returnCorrectUrl() throws IOException {
        ZipFileTree zipFileTree = new ZipFileTree(getTestZipFile());
        List<ZipElement> elementList = zipFileTree.filter(element -> !element.isDirectory());
        List<String> urls =
                elementList.stream().map(element -> element.getUrl().getPath()).collect(Collectors.toList());

        for (String url : urls) {
            boolean res = url.endsWith("test.zip") || url.endsWith("CHECKPOINT.bin") || url.endsWith("MANIFEST.bin")
                    || url.endsWith("TEST-schema.sql") || url.endsWith("TEST.0.0.sql");
            Assert.assertTrue(res);
        }
    }

    @Test
    public void isDirectory_filterDirectory_assertTrue() throws IOException {
        ZipFileTree zipFileTree = new ZipFileTree(getTestZipFile());
        List<ZipElement> elementList = zipFileTree.filter(ZipElement::isDirectory);

        for (ZipElement element : elementList) {
            Assert.assertTrue(element.isDirectory());
        }
    }

    @Test
    public void listZipElements_rootNode_listAlllElts() throws IOException {
        ZipFileTree zipFileTree = new ZipFileTree(getTestZipFile());
        ZipElement element = zipFileTree.getRootElement();

        List<ZipElement> list = element.listZipElements();
        Assert.assertEquals(4, list.size());
    }

    @Test
    public void getParent_accquireRootElt_returnRoot() throws IOException {
        ZipFileTree zipFileTree = new ZipFileTree(getTestZipFile());
        ZipElement element = zipFileTree.getRootElement();

        ZipElement elt = element.listZipElements().get(0).getParent();
        Assert.assertEquals(elt.getRelativePath(), element.getRelativePath());
    }

    @Test
    public void toString_compareToStringValue_toStringEquals_() throws IOException {
        ZipFileTree zipFileTree = new ZipFileTree(getTestZipFile());
        String toString = "test.zip\n"
                + "|_\n"
                + "|_CHECKPOINT.bin\n"
                + "|_MANIFEST.bin\n"
                + "|_GSH\n"
                + "|__TABLE\n"
                + "|___TEST-schema.sql\n"
                + "|___TEST.0.0.sql\n";

        Assert.assertEquals(toString, zipFileTree.toString());
    }

    private File getTestZipFile() {
        URL url = ZipFileTree.class.getClassLoader().getResource("test.zip");
        assert url != null;
        return new File(url.getPath());
    }
}
