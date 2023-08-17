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
package com.oceanbase.odc.core.migrate.resource;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.core.migrate.resource.ResourceManager.YamlFilePredicate;
import com.oceanbase.odc.core.migrate.resource.factory.LocalFileResourceUrlFactory;

/**
 * Test cases for {@link LocalFileResourceUrlFactory}
 *
 * @author yh263208
 * @date 2022-04-29 14:05
 * @since ODC_release_3.3.1
 */
public class LocalFileResourceUrlFactoryTest {

    @Test
    public void generateResourceUrls_scanLocation_returnNotEmpty() throws IOException {
        URL url = LocalFileResourceUrlFactoryTest.class.getClassLoader().getResource("migrate/resource");
        Assert.assertNotNull(url);
        LocalFileResourceUrlFactory factory = new LocalFileResourceUrlFactory(url, new YamlFilePredicate());
        List<URL> urlList = factory.generateResourceUrls();
        Assert.assertFalse(urlList.isEmpty());
        for (URL item : urlList) {
            Assert.assertTrue(item.toString().endsWith("yaml") || item.toString().endsWith("yml"));
        }
    }

    @Test
    public void generateResourceUrls_specificYamlFile_returnOne() throws IOException {
        URL url = LocalFileResourceUrlFactoryTest.class.getClassLoader().getResource("migrate/resource/dir1/e.yaml");
        Assert.assertNotNull(url);
        LocalFileResourceUrlFactory factory = new LocalFileResourceUrlFactory(url, new YamlFilePredicate());
        List<URL> urlList = factory.generateResourceUrls();
        Assert.assertEquals(1, urlList.size());
    }

    @Test
    public void generateResourceUrls_specificSqlFile_returnEmpty() throws IOException {
        URL url = LocalFileResourceUrlFactoryTest.class.getClassLoader()
                .getResource("migrate/resource/init_resource.sql");
        Assert.assertNotNull(url);
        LocalFileResourceUrlFactory factory = new LocalFileResourceUrlFactory(url, new YamlFilePredicate());
        List<URL> urlList = factory.generateResourceUrls();
        Assert.assertTrue(urlList.isEmpty());
    }

    @Test
    public void ddd() {
        URL prefix = ResourceManager.class.getClassLoader().getResource("");
        System.out.println(prefix.toString());
    }

}
