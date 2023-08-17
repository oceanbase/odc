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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.oceanbase.odc.core.shared.constant.ErrorCode;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.service.datatransfer.dumper.AbstractOutputFile;
import com.oceanbase.odc.service.datatransfer.dumper.BinaryFile;
import com.oceanbase.odc.service.datatransfer.dumper.DumpDBObject;
import com.oceanbase.odc.service.datatransfer.dumper.DumperOutput;
import com.oceanbase.tools.loaddump.common.enums.DataFormat;
import com.oceanbase.tools.loaddump.common.enums.ObjectType;
import com.oceanbase.tools.loaddump.common.model.Manifest;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

/**
 * {@link UploadFileResult}
 *
 * @author yh263208
 * @date 2022-07-29 17:13
 * @since ODC_release_3.4.0
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
public class UploadFileResult {
    private String fileName;
    private String fileType;
    private DataFormat format;
    /**
     * 当文件不合法时，这里给出具体不合法的原因
     */
    private ErrorCode errorCode = null;
    private String errorMessage = null;
    private boolean containsSchema;
    private boolean containsData;
    /**
     * 导入数据元信息。
     */
    private Map<ObjectType, Set<String>> importObjects = new HashMap<>();

    public static UploadFileResult ofDumperOutput(@NonNull String fileName, @NonNull DumperOutput dumperOutput) {
        return new UploadFileResult(fileName, dumperOutput);
    }

    public static UploadFileResult ofFail(@NonNull ErrorCode errorCode, Object[] args) {
        UploadFileResult metaInfo = new UploadFileResult();
        metaInfo.errorCode = errorCode;
        if (args != null) {
            metaInfo.errorMessage = errorCode.getLocalizedMessage(args);
        } else {
            metaInfo.errorMessage = errorCode.getLocalizedMessage(new Object[] {});
        }
        return metaInfo;
    }

    public static UploadFileResult ofSql(@NonNull String fileName) {
        UploadFileResult metaInfo = new UploadFileResult();
        metaInfo.format = DataFormat.SQL;
        metaInfo.fileType = "SQL";
        metaInfo.fileName = fileName;
        metaInfo.containsSchema = false;
        metaInfo.containsData = true;
        return metaInfo;
    }

    public static UploadFileResult ofCsv(@NonNull String fileName) {
        UploadFileResult metaInfo = new UploadFileResult();
        metaInfo.format = DataFormat.CSV;
        metaInfo.fileType = "CSV";
        metaInfo.fileName = fileName;
        metaInfo.containsSchema = false;
        metaInfo.containsData = true;
        return metaInfo;
    }

    private UploadFileResult(String fileName, DumperOutput dumperOutput) {
        if (!dumperOutput.isLegal()) {
            this.errorCode = ErrorCodes.ImportInvalidZip;
            this.errorMessage = this.errorCode.getLocalizedMessage(new Object[] {});
            return;
        }
        BinaryFile<Manifest> binaryFile = dumperOutput.getManifest();
        /**
         * only CSV format would save the manifest {@link com.oceanbase.tools.loaddump.client.DumpClient}
         */
        this.format = binaryFile == null ? DataFormat.SQL : binaryFile.getTarget().getDataFormat();
        for (DumpDBObject dbObject : dumperOutput.getDumpDbObjects()) {
            Set<String> names = importObjects.computeIfAbsent(dbObject.getObjectType(), t -> new HashSet<>());
            names.addAll(dbObject.getOutputFiles().stream()
                    .map(AbstractOutputFile::getObjectName).collect(Collectors.toList()));
        }
        this.fileType = "ZIP";
        this.containsSchema = dumperOutput.isContainsSchema();
        this.containsData = dumperOutput.isContainsData();
        this.fileName = fileName;
    }

}
