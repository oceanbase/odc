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
package com.oceanbase.odc.common.util;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ResourceUtilsTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void loadDirectory_NotExists_IsDirectory() {
        thrown.expectMessage("not exists");

        ResourceUtils.loadDirectory("migrate/common/notexists");
    }

    @Test
    public void getFileAsStream_NotExists_Exception() {
        thrown.expectMessage("not exists");

        ResourceUtils.getFileAsStream("test_file_not_exists.txt");
    }

    @Test
    public void listResourcesFromDirectory_NotExists_Exception() {
        thrown.expectMessage("not exists");

        ResourceUtils.listResourcesFromDirectory("db/notexists");
    }

    @Test
    public void calcRootDirectoryInJar_x86() {
        String location = calcRootDirectoryInJar("migrate/common",
                "jar:file:/C:/Program%20Files%20(x86)/OceanBase%20Developer%20Center/OceanBase%20Developer%20Center/"
                        + "resources/libraries/java/odc.jar!/BOOT-INF/classes!/migrate/common");
        Assert.assertEquals("BOOT-INF/classes/migrate/common/", location);
    }

    @Test
    public void calcRootDirectoryInJar_x64() {
        String location = calcRootDirectoryInJar("migrate/common",
                "jar:file:/C:/Program%20Files/OceanBase%20Developer%20Center/OceanBase%20Developer%20Center/"
                        + "resources/libraries/java/odc.jar!/BOOT-INF/classes!/migrate/common");
        Assert.assertEquals("BOOT-INF/classes/migrate/common/", location);
    }

    private String calcRootDirectoryInJar(String location, String path) {
        String jarPath = path.replaceFirst("jar:file:", "")
                .replaceFirst("!.*$", "");
        String rootDirectory = StringUtils.replace(path, "jar:file:", "", 1);
        rootDirectory = StringUtils.replace(rootDirectory, jarPath, "", 1);
        rootDirectory = StringUtils.replace(rootDirectory, "!/", "/");
        if (rootDirectory.startsWith("/")) {
            rootDirectory = rootDirectory.substring(1);
        }
        if (!rootDirectory.endsWith("/")) {
            rootDirectory = rootDirectory + "/";
        }
        return rootDirectory;
    }

}
