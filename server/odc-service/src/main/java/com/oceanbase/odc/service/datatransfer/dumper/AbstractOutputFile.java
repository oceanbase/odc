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
package com.oceanbase.odc.service.datatransfer.dumper;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.lang3.StringUtils;

import com.oceanbase.odc.common.file.zip.ZipElement;
import com.oceanbase.tools.loaddump.common.enums.ObjectType;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link AbstractOutputFile}
 *
 * @author yh263208
 * @date 2022-06-29 21:02
 * @since ODC_release_3.4.0
 */
@Slf4j
@EqualsAndHashCode
public abstract class AbstractOutputFile {

    private final ObjectType objectType;
    protected final URL target;
    private final String fileName;

    public AbstractOutputFile(@NonNull File target, @NonNull ObjectType objectType) throws FileNotFoundException {
        if (!target.exists()) {
            throw new FileNotFoundException("File not found, " + target.getAbsolutePath());
        }
        if (!target.isFile()) {
            throw new IllegalArgumentException("Target is not a file, " + target.getName());
        }
        this.fileName = target.getName();
        this.objectType = objectType;
        try {
            this.target = target.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }

    protected AbstractOutputFile(@NonNull ZipElement zipElt, @NonNull ObjectType objectType) {
        if (zipElt.isDirectory()) {
            throw new IllegalArgumentException("Target is a dir");
        }
        this.fileName = zipElt.getName();
        if (StringUtils.isBlank(this.fileName)) {
            throw new IllegalStateException("File name is blank");
        }
        this.objectType = objectType;
        this.target = zipElt.getUrl();
    }

    public abstract String getObjectName();

    public URL getUrl() {
        return target;
    }

    public String getFileName() {
        return this.fileName;
    }

    public ObjectType getObjectType() {
        return this.objectType;
    }

}
