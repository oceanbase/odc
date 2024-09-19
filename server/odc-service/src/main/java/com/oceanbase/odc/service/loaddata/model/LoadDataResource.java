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

import javax.validation.constraints.NotNull;

import lombok.Data;

@Data
public class LoadDataResource {

    /**
     * 文件校验时若不填，则仅校验文件是否存在，文件大小以及对象存储凭证。 文件预览和发起导入时必填。
     *
     * <pre>
     * +---------------------------------------------------------+
     *         CSV     |   FileFormat.CSV
     * +---------------------------------------------------------+
     *         ORC     |   FileFormat.ORC
     * +---------------------------------------------------------+
     *       Insert    |   FileFormat.SQL or FileFormat.INSERT
     * +---------------------------------------------------------+
     *       Parquet   |   FileFormat.PARQUET
     * +---------------------------------------------------------+
     *         SQL     |   FileFormat.DDL or FileFormat.MIX
     * +---------------------------------------------------------+
     * </pre>
     */
    private FileFormat fileFormat;

    /**
     * Provided by Java Charset.availableCharsets.
     * <p>
     * Big5 GB18030 GB2312 GBK ISO-8859-1 UTF-16 UTF-32 UTF-8
     */
    private String fileEncoding = "UTF-8";

    /**
     * csv 配置. 除导入 sample data 外，其他场景导入 csv 必填.
     */
    private SimpleCsvConfig csvConfig = new SimpleCsvConfig();

    /**
     * 表示待导入资源的来源 UPLOAD -> 上传文件 CLOUD_OBJECT_STORAGE -> 云存储 SAMPLE_DATA -> 样本数据
     */
    @NotNull(message = "\"source\" cannot be null")
    private Source source;

    /**
     * 当多文件导入时可以不填。 单文件导入时必填： 当 source 为 UPLOAD 或 CLOUD_OBJECT_STORAGE 时，填写待导入文件名； 当 source 为
     * SAMPLE_DATA 时，该字段填写具体的模型： 支持的模型有： - TPC_H_25M，TPC_DS_25M
     */
    private String objectName;

    /**
     * 对象存储配置. 导入 sample data 或本地上传时不传，从云存储导入时则需要填写
     */
    private ObjectStorageConfig objectStorageConfig;

    private FileMetadata file;

    /**
     *
     */
    public enum Source {

        /**
         *
         */
        UPLOAD,

        /**
         *
         */
        CLOUD_OBJECT_STORAGE,

        /**
         *
         */
        SAMPLE_DATA
    }
}
