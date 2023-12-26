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

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.oceanbase.odc.common.json.SensitiveInput;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.datamasking.masker.AbstractDataMasker;
import com.oceanbase.odc.core.flow.model.TaskParameters;
import com.oceanbase.odc.core.shared.model.TableIdentity;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;

import lombok.Data;
import lombok.ToString;

/**
 * Data transfer configuration entity
 *
 * @author liuyizhuo.lyz
 * @date 2023-09-15
 */
@Data
@ToString(exclude = {"sysPassword", "connectionInfo"})
public class DataTransferConfig implements TaskParameters, Serializable {
    private Long databaseId;
    private Long connectionId;

    private String schemaName;
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
    private String fileType;
    /**
     * only for ob-loader-dumper
     */
    private String sysUser;
    @SensitiveInput
    @JsonProperty(access = Access.WRITE_ONLY)
    private String sysPassword;
    /**
     * for internal usage
     */
    @JsonIgnore
    private ConnectionInfo connectionInfo;
    @JsonIgnore
    private Map<TableIdentity, Map<String, AbstractDataMasker>> maskConfig;
    @JsonIgnore
    private Long maxDumpSizeBytes;
    @JsonIgnore
    private boolean usePrepStmts;
    @JsonIgnore
    private int cursorFetchSize;
    @JsonIgnore
    private transient List<DBTableColumn> columns;

    public boolean isCompressed() {
        return StringUtils.equalsIgnoreCase("ZIP", fileType);
    }

}
