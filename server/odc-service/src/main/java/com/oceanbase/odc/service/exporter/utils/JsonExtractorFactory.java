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
package com.oceanbase.odc.service.exporter.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import com.oceanbase.odc.common.util.FileZipper;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.service.exporter.impl.JsonExtractor;
import com.oceanbase.odc.service.exporter.model.ExportedFile;
import com.oceanbase.odc.service.objectstorage.ObjectStorageFacade;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class JsonExtractorFactory {

    public static File getConfigJson(String path) {
        File configJson = new File(path, "config.json");

        if (configJson.exists() && configJson.isFile()) {
            return configJson;
        } else {
            return null;
        }
    }

    public static JsonExtractor buildJsonExtractor(ObjectStorageFacade objectStorageFacade, String bucketName,
            String objectId, String secret, String tempFilePath) throws IOException {
        JsonExtractor jsonExtractor = new JsonExtractor();
        // Create a random directory within the specified destination path
        Path randomDir = Files.createTempDirectory(new File(tempFilePath).toPath(), "unzipped-");

        // Create a temporary file to save the InputStream contents
        File tempZipFile = File.createTempFile("tempZip", ".zip", randomDir.toFile());
        jsonExtractor.setTempFilePath(randomDir.toFile().getPath());

        // Write the InputStream to the temporary zip file
        try (FileOutputStream fos = new FileOutputStream(tempZipFile);
                InputStream inputStream = objectStorageFacade.loadObject(bucketName, objectId).getContent()) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }
        }
        FileZipper.unzipFileToPath(tempZipFile, randomDir);
        File configJson = getConfigJson(jsonExtractor.getTempFilePath());
        Verify.notNull(configJson, "Invalid file format, lack of config json.");
        log.info("Files extracted to: {}", randomDir.toAbsolutePath());
        jsonExtractor.setExportedFile(new ExportedFile(configJson, secret));
        return jsonExtractor;
    }

    public static JsonExtractor buildJsonExtractor(ExportedFile exportedFile, String tempPath) throws IOException {
        JsonExtractor jsonExtractor = new JsonExtractor();
        // Create a random directory within the specified destination path
        Path randomDir = Files.createTempDirectory(new File(tempPath).toPath(), "unzipped-");

        // Create a temporary file to save the InputStream contents
        File tempZipFile = File.createTempFile("tempZip", ".zip", randomDir.toFile());
        jsonExtractor.setTempFilePath(randomDir.toFile().getPath());

        // Write the InputStream to the temporary zip file
        try (FileOutputStream fos = new FileOutputStream(tempZipFile);
                InputStream inputStream = Files.newInputStream(exportedFile.getFile().toPath())) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }
        }
        FileZipper.unzipFileToPath(tempZipFile, randomDir);
        File configJson = getConfigJson(jsonExtractor.getTempFilePath());
        Verify.notNull(configJson, "Invalid file format, lack of config json.");
        log.info("Files extracted to: {}", randomDir.toAbsolutePath());
        jsonExtractor.setExportedFile(new ExportedFile(configJson, exportedFile.getSecret()));
        return jsonExtractor;
    }



}
