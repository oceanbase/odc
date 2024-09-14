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
package com.oceanbase.odc.service.loaddata.model;

import lombok.Getter;

/**
 * @author xien.sxe
 * @date 2024/3/6
 * @since 1.0.0
 */
public enum FileFormat {

    /**
     * Standard csv file.
     */
    CSV("--csv"),

    /**
     * A file only contains Insert SQL statements.
     */
    INSERT("--sql"),

    /**
     * Standard Apache ORC file.
     */
    ORC("--orc"),

    /**
     * Standard Apache Parquet file.
     */
    PARQUET("--par");

    @Getter
    private final String option;

    /**
     * @param option
     */
    FileFormat(String option) {
        this.option = option;
    }

    /**
     *
     */
    static final FileFormat[] FILE_FORMATS = FileFormat.values();

    /**
     * @param option
     * @return FileFormat
     */
    public FileFormat valueOfOption(String option) {
        for (FileFormat fileFormat : FILE_FORMATS) {
            if (fileFormat.name().equalsIgnoreCase(option)) {
                return fileFormat;
            }
        }
        throw new IllegalArgumentException("Unsupported file format [" + option + "]");
    }
}
