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
package com.oceanbase.odc.service.audit;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.oceanbase.odc.common.util.CSVUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.authority.SecurityManager;
import com.oceanbase.odc.core.authority.model.DefaultSecurityResource;
import com.oceanbase.odc.core.authority.permission.Permission;
import com.oceanbase.odc.core.authority.util.Authenticated;
import com.oceanbase.odc.core.authority.util.PreAuthenticate;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.constant.AuditEventAction;
import com.oceanbase.odc.core.shared.constant.AuditEventResult;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.core.shared.exception.NotImplementedException;
import com.oceanbase.odc.metadb.audit.AuditEventEntity;
import com.oceanbase.odc.metadb.audit.AuditEventOperator;
import com.oceanbase.odc.metadb.audit.AuditEventRepository;
import com.oceanbase.odc.metadb.audit.AuditSpecs;
import com.oceanbase.odc.service.audit.model.AuditEvent;
import com.oceanbase.odc.service.audit.model.AuditEventExportReq;
import com.oceanbase.odc.service.audit.model.DownloadFormat;
import com.oceanbase.odc.service.audit.model.QueryAuditEventParams;
import com.oceanbase.odc.service.audit.util.AuditEventMapper;
import com.oceanbase.odc.service.common.FileManager;
import com.oceanbase.odc.service.common.model.FileBucket;
import com.oceanbase.odc.service.common.model.Stats;
import com.oceanbase.odc.service.common.util.FileConvertUtils;
import com.oceanbase.odc.service.common.util.OdcFileUtil;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.iam.UserService;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.iam.model.User;
import com.oceanbase.odc.service.objectstorage.cloud.CloudObjectStorageService;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2022/1/18 下午5:18
 * @Description: []
 */
@Slf4j
@Validated
@Service
@Authenticated
public class AuditEventService {
    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Autowired
    private CloudObjectStorageService cloudObjectStorageService;

    @Autowired
    private SecurityManager securityManager;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private UserService userService;

    private AuditEventMapper mapper = AuditEventMapper.INSTANCE;

    /**
     * List audit events of the current user
     */
    @SkipAuthorize("personal resource")
    public Page<AuditEvent> listPersonalAuditEvent(@NotNull QueryAuditEventParams params, @NotNull Pageable pageable) {
        params.setUserIds(Arrays.asList(authenticationFacade.currentUserId()));
        params.setOrganizationId(authenticationFacade.currentOrganizationId());
        return list(AuditSpecs.of(params), pageable);
    }

    /**
     * List audit events of the organization which needs authentication check
     */
    @PreAuthenticate(actions = "read", resourceType = "ODC_AUDIT_EVENT", isForAll = true)
    public Page<AuditEvent> listOrganizationAuditEvents(@NotNull QueryAuditEventParams params,
            @NotNull Pageable pageable) {
        params.setOrganizationId(authenticationFacade.currentOrganizationId());
        return list(AuditSpecs.of(params), pageable);
    }

    private Page<AuditEvent> list(Specification<AuditEventEntity> specs, Pageable pageable) {
        return auditEventRepository.findAll(specs, pageable)
                .map(entity -> {
                    AuditEvent event = mapper.entityToModel(entity);
                    event.setActionName(entity.getAction().getLocalizedMessage());
                    event.setTypeName(entity.getType().getLocalizedMessage());
                    return event;
                });
    }


    @PreAuthenticate(actions = "read", resourceType = "ODC_AUDIT_EVENT", isForAll = true)
    public Stats stats(@NotNull QueryAuditEventParams params) {
        return new Stats().andDistinct("userIds",
                auditEventRepository
                        .findAllOperatorsByOrganizationId(authenticationFacade.currentOrganizationId(),
                                params.getStartTime(), params.getEndTime())
                        .stream()
                        .map(AuditEventOperator::getUserId)
                        .map(userId -> String.valueOf(userId))
                        .distinct()
                        .collect(Collectors.toList()));
    }

    @SkipAuthorize("inside method permission check")
    public AuditEvent findById(@NotNull Long id) {
        AuditEvent auditEvent = mapper.entityToModel(auditEventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ResourceType.ODC_AUDIT_EVENT, "id", id)));
        if (Objects.equals(auditEvent.getUserId(), authenticationFacade.currentUserId())) {
            return auditEvent;
        }
        if (authenticationFacade.currentOrganizationId() != auditEvent.getOrganizationId().longValue()) {
            throw new NotFoundException(ResourceType.ODC_AUDIT_EVENT, "id", id);
        }
        Permission requiredPermission = securityManager.getPermissionByActions(
                new DefaultSecurityResource("*", ResourceType.ODC_AUDIT_EVENT.name()),
                Collections.singletonList("read"));
        if (!securityManager.isPermitted(requiredPermission)) {
            throw new NotFoundException(ResourceType.ODC_AUDIT_EVENT, "id", id);
        }
        return auditEvent;
    }

    /**
     * for unit testing only
     */
    @Transactional(rollbackFor = Exception.class)
    void deleteAllAuditEvent() {
        auditEventRepository.deleteAll();
        log.info("Delete all records in audit_event successfully");
    }

    @Transactional(rollbackFor = Exception.class)
    @SkipAuthorize("odc internal usage")
    public AuditEvent record(AuditEvent auditEvent) {
        AuditEventEntity entity = auditEventRepository.saveAndFlush(mapper.modelToEntity(auditEvent));
        log.debug("Save the audit event successfully, event type={}, event action={}, result={}, userId={}",
                auditEvent.getType(),
                auditEvent.getAction(), auditEvent.getResult(), auditEvent.getUserId());
        return mapper.entityToModel(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    @SkipAuthorize("odc internal usage")
    public void saveAsyncTaskEvent(AuditEvent auditEvent, List<String> taskIds) {
        for (String taskId : taskIds) {
            auditEvent.setTaskId(taskId);
            auditEventRepository.saveAndFlush(mapper.modelToEntity(auditEvent));
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @SkipAuthorize("odc internal usage")
    public AuditEventEntity updateSqlExecuteEvent(String taskId, AuditEventAction action, String detail,
            AuditEventResult result) {
        Optional<AuditEventEntity> optional =
                auditEventRepository.findFirstByTaskIdAndResult(taskId, AuditEventResult.UNFINISHED);
        if (!optional.isPresent()) {
            return null;
        }
        AuditEventEntity entity = optional.get();
        entity.setEndTime(new Date());
        entity.setAction(action);
        entity.setDetail(detail);
        entity.setResult(result);
        return auditEventRepository.saveAndFlush(entity);
    }

    @PreAuthenticate(actions = "read", resourceType = "ODC_AUDIT_EVENT", isForAll = true)
    public String export(AuditEventExportReq req) throws IOException {
        QueryAuditEventParams params = QueryAuditEventParams.builder()
                .organizationId(authenticationFacade.currentOrganizationId())
                .actions(req.getEventActions())
                .types(req.getEventTypes())
                .startTime(req.getStartTime())
                .endTime(req.getEndTime())
                .results(req.getResults())
                .connectionIds(req.getConnectionIds())
                .userIds(req.getUserIds())
                .build();
        Page<AuditEvent> records = list(AuditSpecs.of(params), Pageable.unpaged());
        return getFileDownloadUrl(records, AuditEvent.class, req.getFormat());
    }

    @PreAuthenticate(actions = "read", resourceType = "ODC_AUDIT_EVENT", isForAll = true)
    public List<ConnectionConfig> relatedConnections() {
        return connectionService.listByOrganizationIdWithoutEnvironment(authenticationFacade.currentOrganizationId());
    }

    @PreAuthenticate(actions = "read", resourceType = "ODC_AUDIT_EVENT", isForAll = true)
    public List<User> relatedUsers() {
        return userService.getByOrganizationId(authenticationFacade.currentOrganizationId());
    }

    private <T> String getFileDownloadUrl(Iterable<T> records, Class<T> clazz, DownloadFormat format)
            throws IOException {
        String csvStr = CSVUtils.buildCSVFormatData(records, clazz);
        String fileId = StringUtils.uuid();

        /**
         * TODO: Fix file path traversal after merging code
         */
        String csvFilePath = String.format("%s/%s", FileManager.generateDir(FileBucket.AUDIT),
            fileId + DownloadFormat.CSV.getExtension());
        File file = new File(csvFilePath);
        FileUtils.write(file, csvStr);
        String downloadBaseUrl = FileManager.generateBaseDownloadUrl(FileBucket.AUDIT);
        String downloadUrl;
        if (DownloadFormat.CSV == format) {
            downloadUrl = downloadBaseUrl + fileId + DownloadFormat.CSV.getExtension();
        } else if (DownloadFormat.EXCEL == format) {
            String xlsFilePath =
                    FileConvertUtils.convertCsvToXls(csvFilePath,
                            FileManager.generateDir(FileBucket.AUDIT) + "/" + fileId
                                    + DownloadFormat.EXCEL.getExtension(),
                            null);
            file = new File(xlsFilePath);
            downloadUrl = downloadBaseUrl + fileId + DownloadFormat.EXCEL.getExtension();
        } else {
            throw new NotImplementedException("File format: " + format + " not supported");
        }
        if (cloudObjectStorageService.supported()) {
            try {
                String objectName = cloudObjectStorageService.uploadTemp(file.getName(), file);
                downloadUrl = cloudObjectStorageService.generateDownloadUrl(objectName).toString();
            } catch (Exception exception) {
                log.warn("upload audit event export file to OSS failed, file name={}", fileId);
                throw new RuntimeException(String
                        .format("upload audit event export file to OSS failed, file name: %s",
                                fileId),
                        exception.getCause());
            } finally {
                OdcFileUtil.deleteFiles(file);
            }
        }
        return downloadUrl;
    }
}
