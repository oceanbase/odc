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
import java.util.LinkedList;
import java.util.List;

import com.oceanbase.odc.common.file.zip.ZipElement;
import com.oceanbase.tools.loaddump.common.enums.ObjectType;

import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link DumpDBObject}
 *
 * @author yh263208
 * @date 2022-06-29 21:17
 * @since ODC_release_3.4.0
 */
@Slf4j
@Getter
public class DumpDBObject {

    private final List<AbstractOutputFile> outputFiles;
    private final ObjectType objectType;

    public DumpDBObject(@NonNull File folder) throws FileNotFoundException {
        if (!folder.exists()) {
            throw new FileNotFoundException("File not found, " + folder.getAbsolutePath());
        }
        if (!folder.isDirectory()) {
            throw new IllegalArgumentException("Target is not a dir, " + folder.getName());
        }
        this.objectType = ObjectType.valueOfName(folder.getName());
        this.outputFiles = getOutputFiles(folder, this.objectType);
    }

    protected DumpDBObject(ZipElement zipElt) throws FileNotFoundException {
        if (!zipElt.isDirectory()) {
            throw new IllegalArgumentException("Target is not a dir");
        }
        this.objectType = ObjectType.valueOfName(zipElt.getName());
        this.outputFiles = getOutputFiles(zipElt, this.objectType);
    }

    private List<AbstractOutputFile> getOutputFiles(File parentDir, ObjectType objectType)
            throws FileNotFoundException {
        List<AbstractOutputFile> returnVal = new LinkedList<>();
        for (File file : parentDir.listFiles()) {
            if (!file.isFile()) {
                continue;
            }
            OutputFileFactory factory = new OutputFileFactory(file, objectType);
            AbstractOutputFile outputFile = factory.generate();
            if (outputFile == null) {
                continue;
            }
            returnVal.add(outputFile);
        }
        return returnVal;
    }

    private List<AbstractOutputFile> getOutputFiles(ZipElement zipElt, ObjectType objectType)
            throws FileNotFoundException {
        List<AbstractOutputFile> returnVal = new LinkedList<>();
        for (ZipElement element : zipElt.listZipElements()) {
            if (element.isDirectory()) {
                continue;
            }
            OutputFileFactory factory = new OutputFileFactory(element, objectType);
            AbstractOutputFile outputFile = factory.generate();
            if (outputFile == null) {
                continue;
            }
            returnVal.add(outputFile);
        }
        return returnVal;
    }

    private static boolean isObjectType(String name) {
        try {
            ObjectType.valueOfName(name);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isObjectType(@NonNull File file) {
        if (!file.isDirectory()) {
            return false;
        }
        return isObjectType(file.getName());
    }

    public static boolean isObjectType(@NonNull ZipElement element) {
        if (!element.isDirectory()) {
            return false;
        }
        return isObjectType(element.getName());
    }

}
