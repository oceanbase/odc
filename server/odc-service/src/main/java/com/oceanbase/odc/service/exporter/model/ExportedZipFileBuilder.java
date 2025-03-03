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
package com.oceanbase.odc.service.exporter.model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.annotation.Nullable;

import com.oceanbase.odc.core.shared.Verify;

public class ExportedZipFileBuilder {

    private final File configFile;

    private final Map<String, File> additionFiles;

    public ExportedZipFileBuilder(File configFile, Map<String, File> additionFiles) {
        Verify.notNull(configFile, "configFile");
        this.configFile = configFile;
        this.additionFiles = additionFiles;
    }

    public ExportedFile build(String outputZipFileName, @Nullable String secret) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(outputZipFileName);
                ZipOutputStream zos = new ZipOutputStream(fos)) {
            try (InputStream configInputStream = Files.newInputStream(configFile.toPath())) {
                addFileToZip("config.json", configInputStream, zos);
            }
            if (additionFiles != null) {
                for (Map.Entry<String, File> entry : additionFiles.entrySet()) {
                    addFileToZip(entry.getKey(), Files.newInputStream(entry.getValue().toPath()), zos);
                }
            }
        }
        return new ExportedFile(new File(outputZipFileName), secret);
    }

    private void addFileToZip(String fileName, InputStream inputStream, ZipOutputStream zos) throws IOException {

        try {
            ZipEntry zipEntry = new ZipEntry(fileName);
            zos.putNextEntry(zipEntry);

            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) > 0) {
                zos.write(buffer, 0, len);
            }

            zos.closeEntry();
        } finally {
            inputStream.close();
        }
    }

}
