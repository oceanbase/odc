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
package com.oceanbase.odc.service.archiver;

import static com.oceanbase.odc.common.security.EncryptAlgorithm.AES;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.oceanbase.odc.common.security.PasswordUtils;
import com.oceanbase.odc.common.util.FileZipper;
import com.oceanbase.odc.service.archiver.impl.JsonExtractor;
import com.oceanbase.odc.service.archiver.impl.LocalJsonArchiver;
import com.oceanbase.odc.service.archiver.model.ArchiveConstants;
import com.oceanbase.odc.service.archiver.model.ArchiveProperties;
import com.oceanbase.odc.service.archiver.model.ArchiveRowDataAppender;
import com.oceanbase.odc.service.archiver.model.ArchiveRowDataReader;
import com.oceanbase.odc.service.archiver.model.ArchivedData;
import com.oceanbase.odc.service.archiver.model.ArchivedFile;
import com.oceanbase.odc.service.archiver.model.Encryptable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class LocalJsonArchiverTest {

    @Test
    public void test_archive_full_data_map() throws Exception {
        LocalJsonArchiver archiver = new LocalJsonArchiver();
        Map<String, String> map = new HashMap<>();
        map.put("test", "test");
        ArchiveProperties metadata = new ArchiveProperties();
        metadata.put(ArchiveConstants.FILE_PATH, ".");
        metadata.put(ArchiveConstants.FILE_NAME, "test2");
        String secret = new BCryptPasswordEncoder().encode(PasswordUtils.random());
        ArchivedFile archivedFile = archiver.archiveFullData(map, metadata, secret);
        try (JsonExtractor jsonExtractor = JsonExtractor.buildJsonExtractor(archivedFile, ".");) {
            ArchivedData<Map<String, String>> mapArchivedData =
                    jsonExtractor.extractFullData(new TypeReference<ArchivedData<Map<String, String>>>() {});
            Assert.assertEquals(mapArchivedData.getData(), map);
        }
    }

    @Test
    public void test_archive_rowData() throws Exception {
        LocalJsonArchiver archiver = new LocalJsonArchiver();
        ArchiveProperties metadata = new ArchiveProperties();
        metadata.put(ArchiveConstants.FILE_PATH, "./");
        metadata.put(ArchiveConstants.FILE_NAME, "test2");
        ArchivedFile build = null;
        try (ArchiveRowDataAppender testRowDataArchiveRowDataAppender =
                archiver.buildRowDataAppender(metadata)) {
            testRowDataArchiveRowDataAppender.append(new TestEncryptable("1", "1"));
            testRowDataArchiveRowDataAppender.append(new TestEncryptable("2", "2"));
            testRowDataArchiveRowDataAppender.append(new TestEncryptable("3", "3"));
            build = testRowDataArchiveRowDataAppender.build();
        }
        try (JsonExtractor extractor = JsonExtractor.buildJsonExtractor(build, ".");
                ArchiveRowDataReader<JsonNode> rowDataReader = extractor.getRowDataReader()) {
            Assert.assertEquals(rowDataReader.readRow(TestEncryptable.class), new TestEncryptable("1", "1"));
            Assert.assertEquals(rowDataReader.readRow(TestEncryptable.class), new TestEncryptable("2", "2"));
            Assert.assertEquals(rowDataReader.readRow(TestEncryptable.class), new TestEncryptable("3", "3"));
            Assert.assertNull(rowDataReader.readRow(TestEncryptable.class));
        }
        FileZipper.deleteFiles(build.toFile());
    }

    @Test
    public void test_archive_rowData_encrypt() throws Exception {
        LocalJsonArchiver archiver = new LocalJsonArchiver();
        ArchiveProperties metadata = new ArchiveProperties();
        metadata.put(ArchiveConstants.FILE_PATH, "./");
        metadata.put(ArchiveConstants.FILE_NAME, "test2");
        ArchivedFile build = null;
        String secret = new BCryptPasswordEncoder().encode(PasswordUtils.random());
        try (ArchiveRowDataAppender testRowDataArchiveRowDataAppender =
                archiver.buildRowDataAppender(metadata, secret)) {
            testRowDataArchiveRowDataAppender.append(new TestEncryptable("1", "1"));
            testRowDataArchiveRowDataAppender.append(new TestEncryptable("2", "2"));
            testRowDataArchiveRowDataAppender.append(new TestEncryptable("3", "3"));
            build = testRowDataArchiveRowDataAppender.build();
        }
        try (Extractor<JsonNode> extractor = JsonExtractor.buildJsonExtractor(build, ".");
                ArchiveRowDataReader<JsonNode> rowDataReader = extractor.getRowDataReader()) {
            Assert.assertEquals(rowDataReader.readRow(TestEncryptable.class), new TestEncryptable("1", "1"));
            Assert.assertEquals(rowDataReader.readRow(TestEncryptable.class), new TestEncryptable("2", "2"));
            Assert.assertEquals(rowDataReader.readRow(TestEncryptable.class), new TestEncryptable("3", "3"));
            Assert.assertNull(rowDataReader.readRow(TestEncryptable.class));
        }
        FileZipper.deleteFiles(build.toFile());
    }

    @Test
    public void test_archive_rowData_encrypt_signature() throws Exception {
        LocalJsonArchiver archiver = new LocalJsonArchiver();
        ArchiveProperties metadata = new ArchiveProperties();
        metadata.put(ArchiveConstants.FILE_PATH, "./");
        metadata.put(ArchiveConstants.FILE_NAME, "test2");
        ArchivedFile build = null;
        String secret = new BCryptPasswordEncoder().encode(PasswordUtils.random());
        System.out.println(secret);
        try (ArchiveRowDataAppender testRowDataArchiveRowDataAppender =
                archiver.buildRowDataAppender(metadata,
                        secret)) {
            testRowDataArchiveRowDataAppender.append(new TestEncryptable("1", "1"));
            testRowDataArchiveRowDataAppender.append(new TestEncryptable("2", "2"));
            testRowDataArchiveRowDataAppender.append(new TestEncryptable("3", "3"));
            build = testRowDataArchiveRowDataAppender.build();
        }
        try (Extractor<JsonNode> extractor = JsonExtractor.buildJsonExtractor(build, ".");) {
            Assert.assertTrue(extractor.checkSignature());
        }
        FileZipper.deleteFiles(build.toFile());

    }

    @Test
    public void test_archive_rowData_With_file() throws Exception {
        LocalJsonArchiver archiver = new LocalJsonArchiver();
        ArchiveProperties metadata = new ArchiveProperties();
        metadata.put(ArchiveConstants.FILE_PATH, "./");
        metadata.put(ArchiveConstants.FILE_NAME, "test4");
        ArchivedFile build = null;
        String secret = new BCryptPasswordEncoder().encode(PasswordUtils.random());
        System.out.println(secret);
        try (ArchiveRowDataAppender testRowDataArchiveRowDataAppender =
                archiver.buildRowDataAppender(metadata,
                        secret)) {
            testRowDataArchiveRowDataAppender.append(new TestEncryptable("1", "1"));
            testRowDataArchiveRowDataAppender.append(new TestEncryptable("2", "2"));
            testRowDataArchiveRowDataAppender.append(new TestEncryptable("3", "3"));

            File file = new File("./test2.zip");
            file.createNewFile();
            testRowDataArchiveRowDataAppender.addAdditionFile("test2.zip",
                    Files.newInputStream(Paths.get("./test2.zip")));
            build = testRowDataArchiveRowDataAppender.build();
        }
        try (Extractor<JsonNode> extractor = JsonExtractor.buildJsonExtractor(build, ".");) {
            ArchiveRowDataReader<JsonNode> rowDataReader = extractor.getRowDataReader();
            File file = rowDataReader.getFile("test2.zip");
            Assert.assertTrue(file.exists());
            Assert.assertTrue(extractor.checkSignature());
        }
        FileZipper.deleteFiles(build.toFile());
        FileZipper.deleteFiles(new File("./test2.zip"));


    }

    @Test
    public void test_archive_rowData_encrypt_signature_incroct_secret() throws Exception {
        LocalJsonArchiver archiver = new LocalJsonArchiver();
        ArchiveProperties metadata = new ArchiveProperties();
        metadata.put(ArchiveConstants.FILE_PATH, "./");
        metadata.put(ArchiveConstants.FILE_NAME, "test2");
        ArchivedFile build = null;
        String secret = new BCryptPasswordEncoder().encode(PasswordUtils.random());
        System.out.println(secret);
        try (ArchiveRowDataAppender testRowDataArchiveRowDataAppender =
                archiver.buildRowDataAppender(metadata,
                        secret)) {
            testRowDataArchiveRowDataAppender.append(new TestEncryptable("1", "1"));
            testRowDataArchiveRowDataAppender.append(new TestEncryptable("2", "2"));
            testRowDataArchiveRowDataAppender.append(new TestEncryptable("3", "3"));
            build = testRowDataArchiveRowDataAppender.build();
        }
        ArchivedFile archivedFile = ArchivedFile.fromFile(build.toFile(),
                new BCryptPasswordEncoder().encode(PasswordUtils.random()));

        try (Extractor<JsonNode> extractor = JsonExtractor.buildJsonExtractor(archivedFile, ".");) {
            Assert.assertFalse(extractor.checkSignature());
        }
        FileZipper.deleteFiles(build.toFile());
    }

    @Test
    public void test_archive_FullData_encrypt_signature() throws Exception {
        LocalJsonArchiver archiver = new LocalJsonArchiver();
        ArchiveProperties metadata = new ArchiveProperties();
        metadata.put(ArchiveConstants.FILE_PATH, "./");
        metadata.put(ArchiveConstants.FILE_NAME, "test2");
        ArchivedFile build = null;
        try (
                ArchiveRowDataAppender testRowDataArchiveRowDataAppender =
                        archiver.buildRowDataAppender(metadata)) {
            testRowDataArchiveRowDataAppender.append(new TestEncryptable("1", "1"));
            testRowDataArchiveRowDataAppender.append(new TestEncryptable("2", "2"));
            testRowDataArchiveRowDataAppender.append(new TestEncryptable("3", "3"));
            build = testRowDataArchiveRowDataAppender.build();

        }
        try (JsonExtractor extractor = JsonExtractor.buildJsonExtractor(build, ".")) {
            Assert.assertTrue(extractor.checkSignature());
        }
        FileZipper.deleteFiles(build.toFile());


    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class TestEncryptable implements Encryptable {

        private String id;
        private String name;

        @Override
        public void encrypt(String encryptKey) {
            this.name = AES.encrypt(name, encryptKey, "UTF-8");
        }

        @Override
        public void decrypt(String encryptKey) {
            this.name = AES.decrypt(name, encryptKey, "UTF-8");
        }
    }


}
