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

import com.oceanbase.odc.common.file.zip.ZipElement;
import com.oceanbase.tools.loaddump.common.enums.ObjectType;

import lombok.NonNull;

/**
 * {@link OutputFileFactory}
 *
 * @author yh263208
 * @date 2022-06-29 21:23
 * @since ODC_release_3.4.0
 */
public class OutputFileFactory {

    private final ZipElement zipElt;
    private final File file;
    private final ObjectType objectType;

    public OutputFileFactory(@NonNull File file, @NonNull ObjectType objectType) {
        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("File is illegal, " + file.getAbsolutePath());
        }
        this.file = file;
        this.objectType = objectType;
        this.zipElt = null;
    }

    public OutputFileFactory(@NonNull ZipElement zipElt, @NonNull ObjectType objectType) {
        if (zipElt.isDirectory()) {
            throw new IllegalArgumentException("Entry is illegal");
        }
        this.file = null;
        this.objectType = objectType;
        this.zipElt = zipElt;
    }

    public AbstractOutputFile generate() throws FileNotFoundException {
        if (file != null) {
            return forFile();
        }
        return forZipEntry();
    }

    private AbstractOutputFile forFile() throws FileNotFoundException {
        if (SchemaFile.instanceOf(file)) {
            return new SchemaFile(file, objectType);
        }
        if (DataFile.instanceOf(file)) {
            return new DataFile(file, objectType);
        }
        return null;
    }

    private AbstractOutputFile forZipEntry() {
        if (SchemaFile.instanceOf(zipElt)) {
            return new SchemaFile(zipElt, objectType);
        }
        if (DataFile.instanceOf(zipElt)) {
            return new DataFile(zipElt, objectType);
        }
        return null;
    }

}
