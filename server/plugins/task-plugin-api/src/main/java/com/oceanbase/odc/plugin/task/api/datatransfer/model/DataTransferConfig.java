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

package com.oceanbase.odc.plugin.task.api.datatransfer.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.oceanbase.odc.common.json.SensitiveInput;
import com.oceanbase.odc.core.shared.constant.ConnectType;

import lombok.Data;
import lombok.ToString;

@Data
@ToString(exclude = {"sysPassword"})
public class DataTransferConfig {

    private String schemaName;
    @JsonIgnore
    private SimpleConnectionConfig connectionConfig;
    private DataTransferType transferType;
    private DataTransferFormat dataTransferFormat;
    private boolean transferData;
    private boolean transferDDL;
    private Integer batchCommitNum;
    private int exportFileMaxSize;
    private boolean exportAllObjects = false;
    private boolean globalSnapshot = false;
    private boolean withDropDDL;
    private List<String> skippedDataType;
    private List<String> importFileName;
    private List<DataTransferObject> exportDbObjects;
    private boolean replaceSchemaWhenExists;
    private boolean truncateTableBeforeImport;
    private EncodingType encoding = EncodingType.UTF_8;
    private CsvConfig csvConfig;
    private List<CsvColumnMapping> csvColumnMappings;
    private boolean stopWhenError;
    private String exportFilePath;
    private boolean mergeSchemaFiles;
    private String querySql;
    /**
     * only for ob-loader-dumper
     */
    private boolean notObLoaderDumperCompatible;
    private String sysUser;
    @SensitiveInput
    @JsonProperty(access = Access.WRITE_ONLY)
    private String sysPassword;

    public String getFileType() {
        if (!this.notObLoaderDumperCompatible) {
            return "ZIP";
        }
        return dataTransferFormat.name();
    }

    public void setFileType(String fileType) {
        this.notObLoaderDumperCompatible = !"ZIP".equals(fileType);
    }

    @Data
    public static class SimpleConnectionConfig {
        private String host;
        private Integer port;
        private String username;
        private String password;
        private String tenant;
        private String cluster;
        private ConnectType connectType;
        private String proxyHost;
        private Integer proxyPort;
        private String OBTenant;
        private String sysTenantUsername;
        private String sysTenantPassword;
    }

}
