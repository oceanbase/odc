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
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;

import com.oceanbase.odc.common.file.zip.ZipFileTree;
import com.oceanbase.odc.common.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PlSqlMultiFileOutput extends AbstractThirdPartyOutput {
    private static final Set<String> LEGAL_FILE_SUFFIXES = new HashSet<>();
    private static final Set<ObjectType> PL_OBJECTS = new HashSet<>();

    static {
        for (ObjectType type : ObjectType.values()) {
            LEGAL_FILE_SUFFIXES.add(type.suffix);
        }
        PL_OBJECTS.add(ObjectType.FUNCTION);
        PL_OBJECTS.add(ObjectType.PROCEDURE);
        PL_OBJECTS.add(ObjectType.TRIGGER);
        PL_OBJECTS.add(ObjectType.TYPE);
        PL_OBJECTS.add(ObjectType.PACKAGE);
        PL_OBJECTS.add(ObjectType.TYPE_BODY);
        PL_OBJECTS.add(ObjectType.PACKAGE_BODY);
    }

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
            if (LEGAL_FILE_SUFFIXES.contains(suf)) {
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
                    ObjectType objectType = ObjectType.from(suf);
                    if (objectType != ObjectType.UNKNOWN) {
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

    private enum ObjectType {
        TABLE("tab"),
        VIEW("vw"),
        SEQUENCE("seq"),
        SYNONYM("syn"),
        // pl types
        PROCEDURE("prc"),
        FUNCTION("fnc"),
        TYPE("tps"),
        TRIGGER("trg"),
        PACKAGE("spc"),
        PACKAGE_BODY("bdy"),
        TYPE_BODY("tpb"),
        // unknown types
        UNKNOWN("null");

        private final String suffix;

        ObjectType(String suffix) {
            this.suffix = suffix;
        }

        static ObjectType from(String suffix) {
            for (ObjectType type : ObjectType.values()) {
                if (StringUtils.equals(suffix, type.suffix)) {
                    return type;
                }
            }
            return UNKNOWN;
        }

        boolean isPlObject() {
            return PL_OBJECTS.contains(this);
        }
    }

}
