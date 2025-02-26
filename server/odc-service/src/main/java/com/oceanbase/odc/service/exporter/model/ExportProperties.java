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
package com.oceanbase.odc.service.exporter.model;

import static com.oceanbase.odc.service.exporter.model.ExportConstants.ARCHIVE_TYPE;
import static com.oceanbase.odc.service.exporter.model.ExportConstants.CREATE_TIME;
import static com.oceanbase.odc.service.exporter.model.ExportConstants.FILE_NAME;
import static com.oceanbase.odc.service.exporter.model.ExportConstants.FILE_PATH;
import static com.oceanbase.odc.service.exporter.model.ExportConstants.FILE_ZIP_SUFFER;
import static com.oceanbase.odc.service.exporter.model.ExportConstants.ODC_VERSION;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.boot.info.BuildProperties;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.service.common.util.SpringContextUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * Ensure that the keys are in the same order before and after serialization, so use linkedHashMap
 */
@Slf4j
public class ExportProperties extends LinkedHashMap<String, Object> {

    /**
     * runtime Properties，no need to add a metaData field
     */
    @JsonIgnore
    private transient Map<String, Object> transientProperties = new HashMap<>();

    public String acquireOdcVersion() {
        return (String) getValue(ODC_VERSION);
    }

    public String acquireCreateTime() {
        return (String) getValue(CREATE_TIME);
    }

    public String acquireType() {
        return (String) getValue(ARCHIVE_TYPE);
    }

    public String acquireFilePath() {
        return (String) getValue(FILE_PATH);
    }

    public String acquireFileUrl() {
        return (String) getValue(FILE_PATH) + File.separator + (String) get(FILE_NAME);
    }

    public String acquireZipFileUrl() {
        String fileName = (String) get(FILE_NAME);
        if (!fileName.contains(".")) {
            fileName = fileName + ".zip";
        } else {
            Verify.verify(fileName.endsWith(FILE_ZIP_SUFFER), "Not zip file");
        }
        return (String) getValue(FILE_PATH) + File.separator + fileName;
    }

    public String acquireConfigJsonFileUrl() {
        return (String) getValue(FILE_PATH) + File.separator + "config.json";
    }

    public String acquireConfigTxtFileUrl() {
        return (String) getValue(FILE_PATH) + File.separator + "config.txt";
    }

    public void addDefaultMetaData(String defaultArchivePath) {
        if (this.acquireOdcVersion() == null) {
            try {
                BuildProperties buildProperties = SpringContextUtil.getBean(BuildProperties.class);
                String version = buildProperties.getVersion();
                this.put(ExportConstants.ODC_VERSION, version);
            } catch (Exception e) {
                log.warn("Failed to load build properties", e);
            }
        }
        putIfAbsent(ExportConstants.CREATE_TIME, new Date());
        addDefaultTransientProperties(defaultArchivePath);

    }

    public void addDefaultTransientProperties(String defaultArchivePath) {
        // FilePath does not need to be persisted
        transientProperties.putIfAbsent(ExportConstants.FILE_PATH,
                defaultArchivePath + File.separator + UUID.randomUUID());
    }

    public Object getValue(String key) {
        Object o = get(key);
        if (o == null) {
            return transientProperties.get(key);
        }
        return o;
    }


}
