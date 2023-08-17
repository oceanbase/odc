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
package com.oceanbase.odc.service.datatransfer.model;

/**
 * the file format enum of data transfer task file
 *
 * @author yh263208
 * @date 2021-03-23 11:08
 * @since ODC_release_2.4.1
 */
public enum DataTransferFormat {
    /**
     * this format is dumped from obdumper, include ddl only
     */
    SQL("sql format which defined by obdumper, include ddl only", ".sql"),
    /**
     * this format is dumped from obdumper
     */
    CSV("csv format which defined by obdumper", ".csv"),

    EXCEL("excel format which is used in result set export", ".xlsx");

    private String desc;

    private final String extension;

    public String getExtension() {
        return this.extension;
    }

    DataTransferFormat(String desc, String extension) {
        this.desc = desc;
        this.extension = extension;
    }
}
