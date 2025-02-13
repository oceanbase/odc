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
package com.oceanbase.odc.service.flow;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;

import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.common.util.HashUtils;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.metadb.task.TaskEntity;
import com.oceanbase.odc.metadb.task.TaskRepository;
import com.oceanbase.odc.service.datatransfer.LocalFileManager;
import com.oceanbase.odc.service.flow.model.BinaryDataResult;
import com.oceanbase.odc.service.task.model.ExecutorInfo;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @author gaoda.xy
 * @date 2022/10/17 21:57
 */
@Service
@Slf4j
@SkipAuthorize("odc internal usage")
public class OdcInternalFileService {
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private LocalFileManager localFileManager;
    @Autowired
    private FlowTaskInstanceService flowTaskInstanceService;

    public List<BinaryDataResult> downloadImportFile(@NonNull Long taskId, @NonNull String targetFileName,
            @NonNull String checkCode) throws IOException {
        Optional<TaskEntity> entityOptional = taskRepository.findById(taskId);
        PreConditions.validExists(ResourceType.ODC_TASK, "taskId", taskId, entityOptional::isPresent);
        TaskEntity taskEntity = entityOptional.get();
        PreConditions.validArgumentState(taskEntity.getTaskType() == TaskType.IMPORT, ErrorCodes.Unsupported, null,
                "Unsupported task type: " + taskEntity.getTaskType());
        /**
         * 根据元数据库中的 taskEntity 信息计算 MD5 校验码，如果与参数 checkCode 一致，则通过校验
         */
        String checkString = String.valueOf(taskId)
                + taskEntity.getCreatorId()
                + taskEntity.getCreateTime()
                + taskEntity.getSubmitter()
                + taskEntity.getOrganizationId()
                + targetFileName;
        PreConditions.validArgumentState(checkCode.equals(HashUtils.md5(checkString)), ErrorCodes.AccessDenied, null,
                "Request parameter verification failed");
        return flowTaskInstanceService.internalDownload(taskEntity, targetFileName);
    }

    public void getExternalImportFiles(TaskEntity taskEntity, ExecutorInfo creatorInfo, List<String> importFileNames,
            int requestTimeoutMillis) throws Exception {
        String checkString = String.valueOf(taskEntity.getId())
                + taskEntity.getCreatorId()
                + taskEntity.getCreateTime()
                + taskEntity.getSubmitter()
                + taskEntity.getOrganizationId();
        String url = new URIBuilder().setScheme("http")
                .setHost(creatorInfo.getHost())
                .setPort(creatorInfo.getPort())
                .setPath("/api/v2/internal/file/downloadImportFile")
                .setParameter("taskId", String.valueOf(taskEntity.getId()))
                .build().toString();
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet();
            RequestConfig config = RequestConfig.custom()
                    .setConnectTimeout(requestTimeoutMillis)
                    .setConnectionRequestTimeout(requestTimeoutMillis)
                    .setSocketTimeout(requestTimeoutMillis)
                    .build();
            httpGet.setConfig(config);
            for (String fileName : importFileNames) {
                /**
                 * 根据元数据库中的 taskEntity 信息计算 MD5 校验码
                 */
                String md5Code = HashUtils.md5(checkString + fileName);
                URI uri = new URIBuilder(url)
                        .addParameter("fileName", fileName)
                        .addParameter("checkCode", md5Code)
                        .build();
                httpGet.setURI(uri);
                try (CloseableHttpResponse httpResponse = httpClient.execute(httpGet)) {
                    if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                        int fileSize = localFileManager.copy(TaskType.IMPORT, LocalFileManager.UPLOAD_BUCKET,
                                httpResponse.getEntity().getContent(), () -> fileName);
                        log.info("Get import file successfully, fileName={}, fileSize={} Bytes", fileName, fileSize);
                    }
                }
            }
        } catch (Exception e) {
            log.warn(String.format("Get import file filed, taskId=%d", taskEntity.getId()), e);
        }
    }
}
