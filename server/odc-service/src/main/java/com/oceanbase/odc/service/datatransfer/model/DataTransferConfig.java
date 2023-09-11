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

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.oceanbase.odc.common.json.SensitiveInput;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.flow.model.TaskParameters;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * data transfer config object which is used to packaged information
 *
 * @author yh263208
 * @date 2021-03-23 17:04
 * @since ODC_release_2.4.1
 */
@Getter
@Setter
@EqualsAndHashCode
@ToString(exclude = {"sysPassword"})
public class DataTransferConfig implements Serializable, TaskParameters {
    private String schemaName;
    private Long databaseId;
    private Long connectionId;
    @JsonIgnore
    private ConnectionConfig connectionConfig;
    private DataTransferType transferType;
    private DataTransferFormat dataTransferFormat;
    private boolean transferData;
    private boolean transferDDL;
    private Integer batchCommitNum;

    /**
     * dump or export settings
     */
    private List<DataTransferObject> exportDbObjects;
    private int exportFileMaxSize;
    /**
     * 是否导出全部数据库对象，若为 true 则 {@link #exportDbObjects} 无效
     */
    private boolean exportAllObjects = false;
    private boolean globalSnapshot = false;
    /**
     * if this flag is true, the script you dump will include drop ddl
     */
    private boolean withDropDDL;
    private List<String> skippedDataType;

    /**
     * load or import settings
     */
    private List<String> importFileName;
    private boolean replaceSchemaWhenExists;
    private boolean truncateTableBeforeImport;
    private EncodingType encoding = EncodingType.UTF_8;
    private CsvConfig csvConfig;
    private List<CsvColumnMapping> csvColumnMappings;
    private boolean stopWhenError;
    private boolean notObLoaderDumperCompatible;
    /**
     * xxx@sys account settings
     */
    private String sysUser;
    @SensitiveInput
    @JsonProperty(access = Access.WRITE_ONLY)
    private String sysPassword;
    /**
     * 导出数据输出路径，该参数只在客户端场景下有效
     */
    private String exportFilePath;
    private boolean mergeSchemaFiles;

    /**
     * For internal usage
     */
    private ConnectType type;

    public String getFileType() {
        if (!this.notObLoaderDumperCompatible) {
            return "ZIP";
        }
        return dataTransferFormat.name();
    }

    public void setFileType(String fileType) {
        this.notObLoaderDumperCompatible = !"ZIP".equals(fileType);
    }
}
