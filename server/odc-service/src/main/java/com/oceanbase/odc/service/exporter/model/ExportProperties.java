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

import static com.oceanbase.odc.service.exporter.model.ExportConstants.CREATE_TIME;
import static com.oceanbase.odc.service.exporter.model.ExportConstants.EXPORT_TYPE;
import static com.oceanbase.odc.service.exporter.model.ExportConstants.FILE_NAME;
import static com.oceanbase.odc.service.exporter.model.ExportConstants.FILE_PATH;
import static com.oceanbase.odc.service.exporter.model.ExportConstants.FILE_ZIP_EXTENSION;
import static com.oceanbase.odc.service.exporter.model.ExportConstants.GIT_BRANCH;
import static com.oceanbase.odc.service.exporter.model.ExportConstants.GIT_COMMIT_ID;
import static com.oceanbase.odc.service.exporter.model.ExportConstants.ODC_VERSION;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.MoreObjects;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.service.common.util.SpringContextUtil;

import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class ExportProperties {

    /**
     * Ensure that the keys are in the same order before and after serialization, so use linkedHashMap
     */
    private final LinkedHashMap<String, Object> metaData;
    /**
     * runtime Properties，no need to add a metaData field
     */
    @JsonIgnore
    private final transient Map<String, Object> transientProperties;

    public ExportProperties(@NonNull LinkedHashMap<String, Object> metaData, Map<String, Object> transientProperties) {
        this.metaData = metaData;
        this.transientProperties = MoreObjects.firstNonNull(transientProperties, new HashMap<>());
    }

    public ExportProperties() {
        this.metaData = new LinkedHashMap<>();
        this.transientProperties = new HashMap<>();
    }

    public String acquireOdcVersion() {
        return (String) getValue(ODC_VERSION);
    }

    public String acquireGitCommit() {
        return (String) getValue(GIT_COMMIT_ID);
    }

    public String acquireCreateTime() {
        return (String) getValue(CREATE_TIME);
    }

    public String acquireExportType() {
        return (String) getValue(EXPORT_TYPE);
    }

    public String acquireFilePath() {
        return (String) getValue(FILE_PATH);
    }

    public String acquireZipFileUrl() {
        String fileName = (String) getValue(FILE_NAME);
        if (!fileName.contains(".")) {
            fileName = fileName + ".zip";
        } else {
            Verify.verify(fileName.endsWith(FILE_ZIP_EXTENSION), "Not zip file");
        }
        return (String) getValue(FILE_PATH) + File.separator + fileName;
    }

    public String acquireConfigJsonFileUrl() {
        return (String) getValue(FILE_PATH) + File.separator + "config.json";
    }

    public void addDefaultMetaData() {
        if (this.acquireOdcVersion() == null) {
            try {
                BuildProperties buildProperties = SpringContextUtil.getBean(BuildProperties.class);
                String version = buildProperties.getVersion();
                metaData.put(ExportConstants.ODC_VERSION, version);
            } catch (Exception e) {
                log.warn("Failed to load build properties", e);
            }
        }
        if (this.acquireGitCommit() == null) {
            try {
                GitProperties gitProperties = SpringContextUtil.getBean(GitProperties.class);
                metaData.putIfAbsent(GIT_COMMIT_ID, gitProperties.getCommitId());
                metaData.putIfAbsent(GIT_BRANCH, gitProperties.getBranch());
            } catch (Exception e) {
                log.warn("Failed to load git properties", e);
            }
        }
        if (this.acquireCreateTime() == null) {
            metaData.putIfAbsent(ExportConstants.CREATE_TIME, new Date());
        }
    }

    @SneakyThrows
    public void addFilePathProperties(String defaultArchivePath) {
        File path = new File(defaultArchivePath);
        if (!path.exists()) {
            path.mkdirs();
        }
        Path randomDir = Files.createTempDirectory(new File(defaultArchivePath).toPath(), "exportFile-");
        // FilePath does not need to be persisted to metadata
        transientProperties.putIfAbsent(ExportConstants.FILE_PATH,
                randomDir.toString());
    }

    public void putToMetaData(String key, Object value) {
        metaData.put(key, value);
    }

    public void putTransientProperties(String key, Object value) {
        transientProperties.put(key, value);
    }

    public Object getValue(String key) {
        Object o = metaData.get(key);
        if (o == null) {
            return transientProperties.get(key);
        }
        return o;
    }

    public String getStringValue(String key) {
        Object o = metaData.get(key);
        if (o == null) {
            return (String) transientProperties.get(key);
        }
        return (String) o;
    }

    public ExportProperties deepClone() {
        LinkedHashMap<String, Object> metadata = JsonUtils.fromJson(JsonUtils.toJson(this.getMetaData()),
                new TypeReference<LinkedHashMap<String, Object>>() {});
        HashMap<String, Object> transientProperties =
                JsonUtils.fromJson(JsonUtils.toJson(this.getTransientProperties()),
                        new TypeReference<HashMap<String, Object>>() {});
        // Ensure all archive file in same path
        return new ExportProperties(metadata, transientProperties);
    }


}
