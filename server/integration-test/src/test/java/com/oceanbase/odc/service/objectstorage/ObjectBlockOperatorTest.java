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
package com.oceanbase.odc.service.objectstorage;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Optional;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.metadb.objectstorage.ObjectBlockEntity;
import com.oceanbase.odc.metadb.objectstorage.ObjectBlockRepository;
import com.oceanbase.odc.service.objectstorage.model.ObjectMetadata;
import com.oceanbase.odc.service.objectstorage.operator.ObjectBlockIterator;
import com.oceanbase.odc.service.objectstorage.operator.ObjectBlockOperator;

public class ObjectBlockOperatorTest extends ServiceTestEnv {
    private static final String OBJECT_ID = "fake_object_id";

    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    @Autowired
    private ObjectBlockOperator objectBlockOperator;

    @Autowired
    private ObjectBlockRepository blockRepository;

    private File file;

    @Before
    public void setUp() throws IOException {
        objectBlockOperator.deleteByObjectId(OBJECT_ID);
        file = folder.newFile("test.tmp");
        try (FileWriter fileWriter = new FileWriter(file)) {
            fileWriter.write("test_file_content");
        }
    }

    @After
    public void tearDown() {
        objectBlockOperator.deleteByObjectId(OBJECT_ID);
    }

    @Test
    public void testSaveObjectBlock_Success() {
        ObjectMetadata meta = new ObjectMetadata();
        meta.setTotalLength(file.length());
        meta.setSplitLength(1_048_576);
        meta.setObjectId(OBJECT_ID);
        meta.setObjectName("TEST-" + OBJECT_ID);
        meta.setSha1("test-sha1");
        meta.setBucketName("test-bucket");

        // 保存文件信息到 db
        objectBlockOperator.saveObjectBlock(meta, file);
        Optional<ObjectBlockEntity> entityOpt = blockRepository.findByObjectIdAndIndex(OBJECT_ID, 0L);
        assertTrue(entityOpt.isPresent());

        // 查询文件信息
        ObjectBlockIterator iterator = objectBlockOperator.getBlockIterator(OBJECT_ID);
        assertTrue(iterator.hasNext());
        Assert.assertEquals(file.length(), iterator.next().length);

        // 删除文件信息.
        int size = objectBlockOperator.deleteByObjectId(OBJECT_ID);
        assertEquals(1, size);
        entityOpt = blockRepository.findByObjectIdAndIndex(OBJECT_ID, 0L);
        assertFalse(entityOpt.isPresent());
    }
}
