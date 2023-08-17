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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Test cases for {@link ResourceManager}
 *
 * @author yh263208
 * @date 2022-04-21 20:32
 * @since ODC_release_3.3.1
 */
public class ResourceFileManagerTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void getResources_relyExists_returnOrderedFiles() throws IOException {
        ResourceManager manager =
                new ResourceManager("migrate/resource/dir2", "migrate/resource/iam_user_role_permission.yml");
        List<String> orderedPaths = manager.getResourceUrls().stream().map(url -> {
            String str = url.toString();
            return str.substring(str.indexOf("migrate/resource/"));
        }).collect(Collectors.toList());
        orderedPaths.remove("migrate/resource/iam_user_role_permission.yml");
        List<String> expect = Arrays.asList("migrate/resource/dir2/c.yaml", "migrate/resource/dir2/b.yaml",
                "migrate/resource/dir2/a.yml");
        Assert.assertEquals(expect, orderedPaths);
    }

    @Test
    public void getResources_relyFileNotInvolved_expThrown() throws IOException {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("File not found resource/dir/d.yml");
        ResourceManager manager =
                new ResourceManager("migrate/resource/dir1", "migrate/resource/iam_user_role_permission.yml");
    }

    @Test
    public void getResources_circularReference_expThrown() throws IOException {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Circular reference detected");
        ResourceManager manager =
                new ResourceManager("migrate/resource/dir1", "migrate/resource/dir",
                        "migrate/resource/iam_user_role_permission.yml");
    }

    @Test
    public void getResource_relyExists_getContent() throws IOException {
        ResourceManager manager =
                new ResourceManager("migrate/resource/dir2", "migrate/resource/iam_user_role_permission.yml");
        manager.getResourceUrls().forEach(s -> Assert.assertNotNull(manager.findByUrl(s)));
    }

    @Test
    public void getResource_relyExists_returnRelyContent() throws IOException {
        ResourceManager manager =
                new ResourceManager("migrate/resource/dir2", "migrate/resource/iam_user_role_permission.yml");
        Assert.assertNotNull(manager.findBySuffix("migrate/resource/dir2/b.yaml"));
    }

}
