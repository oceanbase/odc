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
package com.oceanbase.odc.plugin.task.api.datatransfer.dumper;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.oceanbase.odc.common.file.zip.ZipElement;
import com.oceanbase.tools.loaddump.common.enums.ObjectType;

import lombok.NonNull;

/**
 * {@link SchemaFile}
 *
 * @author yh263208
 * @date 2022-06-29 20:06
 * @since ODC_release_3.4.0
 */
public class SchemaFile extends AbstractOutputFile {
    /**
     * file pattern of schema file which provided by ob-dumper. eg.
     *
     * <pre>
     * xxx - schema.sql
     * </pre>
     */
    private static final Pattern FILE_PATTERN =
            Pattern.compile("^\"?(.+)\"?-schema\\.(sql)$", Pattern.CASE_INSENSITIVE);
    protected final String objectName;

    public SchemaFile(File file, ObjectType objectType) throws FileNotFoundException {
        super(file, objectType);
        this.objectName = parseObjectName();
    }

    protected SchemaFile(ZipElement zipElt, ObjectType objectType) {
        super(zipElt, objectType);
        this.objectName = parseObjectName();
    }

    private String parseObjectName() {
        String filename = getFileName();
        Matcher matcher = FILE_PATTERN.matcher(filename);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Target is not a schema file, " + filename);
        }
        return matcher.group(1);
    }

    @Override
    public String getObjectName() {
        return this.objectName;
    }

    public static boolean instanceOf(@NonNull File file) {
        if (!file.isFile()) {
            return false;
        }
        return instanceOf(file.getName());
    }

    public static boolean instanceOf(@NonNull ZipElement element) {
        if (element.isDirectory()) {
            return false;
        }
        return instanceOf(element.getName());
    }

    private static boolean instanceOf(@NonNull String name) {
        Matcher matcher = FILE_PATTERN.matcher(name);
        return matcher.matches();
    }

}
