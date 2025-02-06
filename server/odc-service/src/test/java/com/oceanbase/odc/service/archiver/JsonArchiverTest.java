/*
 * Copyright (c) 2025 OceanBase.
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

package com.oceanbase.odc.service.archiver;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.common.security.PasswordUtils;

public class JsonArchiverTest {

    @Test
    public void test_archive_map() throws Exception {
        JsonArchiver archiver = new JsonArchiver();
        Map<String, String> map = new HashMap<>();
        map.put("test", "test");
        Properties metadata = new Properties();
        metadata.setProperty(ArchiveConstants.FILE_PATH, "./");
        metadata.setProperty(ArchiveConstants.FILE_NAME, "test.txt");
        String secret = new BCryptPasswordEncoder().encode(PasswordUtils.random());
        ArchivedFile archivedFile = archiver.archiveFullDataToLocal(map, metadata, secret);
        JsonExtractor jsonExtractor = new JsonExtractor();
        ArchivedData<Map<String, String>> mapArchivedData = jsonExtractor.extractFullData(archivedFile,
                new TypeReference<ArchivedData<Map<String, String>>>() {});
        Assert.assertEquals(mapArchivedData.getData(), map);
    }


}
