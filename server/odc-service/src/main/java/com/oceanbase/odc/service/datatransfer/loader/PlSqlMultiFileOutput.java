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
package com.oceanbase.odc.service.datatransfer.loader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;

import com.oceanbase.odc.common.file.zip.ZipFileTree;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PlSqlMultiFileOutput extends AbstractThirdPartyOutput {

    private final ZipFileTree zip;

    public PlSqlMultiFileOutput(File origin) throws IOException {
        super(origin);
        this.zip = new ZipFileTree(origin);
    }

    @Override
    public boolean supports() {
        if (!origin.getName().endsWith(".zip")) {
            return false;
        }
        AtomicBoolean isFromPlSql = new AtomicBoolean(false);
        zip.forEach((integer, zipElement) -> {
            if (zipElement.isDirectory()) {
                return;
            }
            String filename = zipElement.getName();
            String[] split = filename.split("\\.");
            String suf = split[split.length - 1];
            if (PlSqlFormat.isPlFileSuffix(suf)) {
                isFromPlSql.set(true);
            }
        });
        return isFromPlSql.get();
    }

    @Override
    public void toObLoaderDumperCompatibleFormat(File dest) throws Exception {
        try (FileOutputStream outputStream = new FileOutputStream(dest);
                ArchiveOutputStream out = new ZipArchiveOutputStream(outputStream)) {
            zip.forEach((integer, zipElement) -> {
                if (zipElement.isDirectory()) {
                    return;
                }
                String filename = zipElement.getName();
                String[] split = filename.split("\\.");
                String suf = split[split.length - 1];
                try {
                    PlSqlFormat.ObjectType objectType = PlSqlFormat.ObjectType.from(suf);
                    if (objectType != PlSqlFormat.ObjectType.UNKNOWN) {
                        String directory = objectType.name();
                        // point may exist in object name
                        String objectName = filename.split("\\." + suf)[0];

                        out.putArchiveEntry(new ZipArchiveEntry(directory + "/" + objectName + "-schema.sql"));
                        if (objectType.isPlObject()) {
                            out.write("delimiter /\n".getBytes());
                        }
                        try (InputStream inputStream = zipElement.getUrl().openStream()) {
                            IOUtils.copy(inputStream, out);
                        }
                        out.closeArchiveEntry();
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    @Override
    public String getNewFilePrefix() {
        return "plsql";
    }

}
