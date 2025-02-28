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
package com.oceanbase.odc.service.exporter;

import static com.oceanbase.odc.common.security.EncryptAlgorithm.AES;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.fasterxml.jackson.databind.JsonNode;
import com.oceanbase.odc.common.security.PasswordUtils;
import com.oceanbase.odc.service.exporter.impl.JsonExtractor;
import com.oceanbase.odc.service.exporter.impl.LocalJsonExporter;
import com.oceanbase.odc.service.exporter.model.Encryptable;
import com.oceanbase.odc.service.exporter.model.ExportConstants;
import com.oceanbase.odc.service.exporter.model.ExportProperties;
import com.oceanbase.odc.service.exporter.model.ExportRowDataAppender;
import com.oceanbase.odc.service.exporter.model.ExportRowDataReader;
import com.oceanbase.odc.service.exporter.model.ExportedFile;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class LocalJsonExporterTest {

    @Test
    public void test_extract_metadata() throws Exception {
        LocalJsonExporter archiver = new LocalJsonExporter();
        ExportProperties metadata = new ExportProperties();
        metadata.putTransientProperties(ExportConstants.FILE_PATH, "./");
        metadata.putTransientProperties(ExportConstants.FILE_NAME, "test2");
        metadata.put("TEST", "test");
        metadata.put("TEST2", 123456);
        ExportedFile build = null;
        try (ExportRowDataAppender testRowDataExportRowDataAppender =
                archiver.buildRowDataAppender(metadata)) {
            testRowDataExportRowDataAppender.append(new TestEncryptable("1", "1"));
            testRowDataExportRowDataAppender.append(new TestEncryptable("2", "2"));
            testRowDataExportRowDataAppender.append(new TestEncryptable("3", "3"));
            build = testRowDataExportRowDataAppender.build();
        }
        try (JsonExtractor extractor = JsonExtractor.buildJsonExtractor(build, ".");
                ExportRowDataReader<JsonNode> rowDataReader = extractor.getRowDataReader()) {
            ExportProperties metaData = rowDataReader.getMetaData();
            Assert.assertEquals(metaData.getValue("TEST"), "test");
            Assert.assertEquals(metaData.getValue("TEST2"), 123456);
            Assert.assertNull(metaData.getValue(ExportConstants.FILE_PATH));
        }
        FileUtils.deleteQuietly(build.getFile());
    }

    @Test
    public void test_archive_rowData() throws Exception {
        LocalJsonExporter archiver = new LocalJsonExporter();
        ExportProperties metadata = new ExportProperties();
        metadata.putTransientProperties(ExportConstants.FILE_PATH, "./");
        metadata.putTransientProperties(ExportConstants.FILE_NAME, "test2");
        ExportedFile build = null;
        try (ExportRowDataAppender testRowDataExportRowDataAppender =
                archiver.buildRowDataAppender(metadata)) {
            testRowDataExportRowDataAppender.append(new TestEncryptable("1", "1"));
            testRowDataExportRowDataAppender.append(new TestEncryptable("2", "2"));
            testRowDataExportRowDataAppender.append(new TestEncryptable("3", "3"));
            build = testRowDataExportRowDataAppender.build();
        }
        try (JsonExtractor extractor = JsonExtractor.buildJsonExtractor(build, ".");
                ExportRowDataReader<JsonNode> rowDataReader = extractor.getRowDataReader()) {
            Assert.assertEquals(rowDataReader.readRow(TestEncryptable.class), new TestEncryptable("1", "1"));
            Assert.assertEquals(rowDataReader.readRow(TestEncryptable.class), new TestEncryptable("2", "2"));
            Assert.assertEquals(rowDataReader.readRow(TestEncryptable.class), new TestEncryptable("3", "3"));
            Assert.assertNull(rowDataReader.readRow(TestEncryptable.class));
        }
        FileUtils.deleteQuietly(build.getFile());
    }

    @Test
    public void test_archive_rowData_encrypt() throws Exception {
        LocalJsonExporter archiver = new LocalJsonExporter();
        ExportProperties metadata = new ExportProperties();
        metadata.putTransientProperties(ExportConstants.FILE_PATH, "./");
        metadata.putTransientProperties(ExportConstants.FILE_NAME, "test2");
        ExportedFile build = null;
        String secret = new BCryptPasswordEncoder().encode(PasswordUtils.random());
        try (ExportRowDataAppender testRowDataExportRowDataAppender =
                archiver.buildRowDataAppender(metadata, secret)) {
            testRowDataExportRowDataAppender.append(new TestEncryptable("1", "1"));
            testRowDataExportRowDataAppender.append(new TestEncryptable("2", "2"));
            testRowDataExportRowDataAppender.append(new TestEncryptable("3", "3"));
            build = testRowDataExportRowDataAppender.build();
        }
        try (Extractor<JsonNode> extractor = JsonExtractor.buildJsonExtractor(build, ".");
                ExportRowDataReader<JsonNode> rowDataReader = extractor.getRowDataReader()) {
            Assert.assertEquals(rowDataReader.readRow(TestEncryptable.class), new TestEncryptable("1", "1"));
            Assert.assertEquals(rowDataReader.readRow(TestEncryptable.class), new TestEncryptable("2", "2"));
            Assert.assertEquals(rowDataReader.readRow(TestEncryptable.class), new TestEncryptable("3", "3"));
            Assert.assertNull(rowDataReader.readRow(TestEncryptable.class));
        }
        FileUtils.deleteQuietly(build.getFile());
    }

    @Test
    public void test_archive_rowData_encrypt_signature() throws Exception {
        LocalJsonExporter archiver = new LocalJsonExporter();
        ExportProperties metadata = new ExportProperties();
        metadata.putTransientProperties(ExportConstants.FILE_PATH, "./");
        metadata.putTransientProperties(ExportConstants.FILE_NAME, "test2");
        ExportedFile build = null;
        String secret = new BCryptPasswordEncoder().encode(PasswordUtils.random());
        System.out.println(secret);
        try (ExportRowDataAppender testRowDataExportRowDataAppender =
                archiver.buildRowDataAppender(metadata,
                        secret)) {
            testRowDataExportRowDataAppender.append(new TestEncryptable("1", "1"));
            testRowDataExportRowDataAppender.append(new TestEncryptable("2", "2"));
            testRowDataExportRowDataAppender.append(new TestEncryptable("3", "3"));
            build = testRowDataExportRowDataAppender.build();
        }
        try (Extractor<JsonNode> extractor = JsonExtractor.buildJsonExtractor(build, ".");) {
            Assert.assertTrue(extractor.checkSignature());
        }
        FileUtils.deleteQuietly(build.getFile());
    }

    @Test
    public void test_archive_rowData_With_file() throws Exception {
        LocalJsonExporter archiver = new LocalJsonExporter();
        ExportProperties metadata = new ExportProperties();
        metadata.putTransientProperties(ExportConstants.FILE_PATH, "./");
        metadata.putTransientProperties(ExportConstants.FILE_NAME, "test4");
        ExportedFile build = null;
        String secret = new BCryptPasswordEncoder().encode(PasswordUtils.random());
        System.out.println(secret);
        try (ExportRowDataAppender testRowDataExportRowDataAppender =
                archiver.buildRowDataAppender(metadata,
                        secret)) {
            testRowDataExportRowDataAppender.append(new TestEncryptable("1", "1"));
            testRowDataExportRowDataAppender.append(new TestEncryptable("2", "2"));
            testRowDataExportRowDataAppender.append(new TestEncryptable("3", "3"));

            File file = new File("./test2.zip");
            file.createNewFile();
            testRowDataExportRowDataAppender.addAdditionFile("test2.zip", file);
            build = testRowDataExportRowDataAppender.build();
        }
        try (Extractor<JsonNode> extractor = JsonExtractor.buildJsonExtractor(build, ".");) {
            ExportRowDataReader<JsonNode> rowDataReader = extractor.getRowDataReader();
            File file = rowDataReader.getFile("test2.zip");
            Assert.assertTrue(file.exists());
            Assert.assertTrue(extractor.checkSignature());
        }
        FileUtils.deleteQuietly(build.getFile());
        FileUtils.deleteQuietly(new File("./test2.zip"));

    }

    @Test
    public void test_archive_rowData_encrypt_signature_incroct_secret() throws Exception {
        LocalJsonExporter archiver = new LocalJsonExporter();
        ExportProperties metadata = new ExportProperties();
        metadata.putTransientProperties(ExportConstants.FILE_PATH, "./");
        metadata.putTransientProperties(ExportConstants.FILE_NAME, "test2");
        ExportedFile build = null;
        String secret = new BCryptPasswordEncoder().encode(PasswordUtils.random());
        try (ExportRowDataAppender testRowDataExportRowDataAppender =
                archiver.buildRowDataAppender(metadata,
                        secret)) {
            testRowDataExportRowDataAppender.append(new TestEncryptable("1", "1"));
            testRowDataExportRowDataAppender.append(new TestEncryptable("2", "2"));
            testRowDataExportRowDataAppender.append(new TestEncryptable("3", "3"));
            build = testRowDataExportRowDataAppender.build();
        }
        ExportedFile exportedFile = new ExportedFile(build.getFile(),
                new BCryptPasswordEncoder().encode(PasswordUtils.random()), true);

        try (Extractor<JsonNode> extractor = JsonExtractor.buildJsonExtractor(exportedFile, ".");) {
            Assert.assertFalse(extractor.checkSignature());
        }
        FileUtils.deleteQuietly(build.getFile());
    }

    @Test
    public void test_archive_FullData_encrypt_signature() throws Exception {
        LocalJsonExporter archiver = new LocalJsonExporter();
        ExportProperties metadata = new ExportProperties();
        metadata.putTransientProperties(ExportConstants.FILE_PATH, "./");
        metadata.putTransientProperties(ExportConstants.FILE_NAME, "test2");
        ExportedFile build = null;
        try (
                ExportRowDataAppender testRowDataExportRowDataAppender =
                        archiver.buildRowDataAppender(metadata)) {
            testRowDataExportRowDataAppender.append(new TestEncryptable("1", "1"));
            testRowDataExportRowDataAppender.append(new TestEncryptable("2", "2"));
            testRowDataExportRowDataAppender.append(new TestEncryptable("3", "3"));
            build = testRowDataExportRowDataAppender.build();

        }
        try (JsonExtractor extractor = JsonExtractor.buildJsonExtractor(build, ".")) {
            Assert.assertTrue(extractor.checkSignature());
        }
        FileUtils.deleteQuietly(build.getFile());


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
