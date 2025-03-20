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
package com.oceanbase.odc.service.schedule.export;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.common.security.PasswordUtils;
import com.oceanbase.odc.core.shared.OrganizationIsolated;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.OrganizationType;
import com.oceanbase.odc.core.shared.constant.ResourceRoleName;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.metadb.flow.FlowInstanceEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleRepository;
import com.oceanbase.odc.service.common.util.OdcFileUtil;
import com.oceanbase.odc.service.exporter.model.ExportProperties;
import com.oceanbase.odc.service.exporter.model.ExportedFile;
import com.oceanbase.odc.service.flow.FlowInstanceService;
import com.oceanbase.odc.service.iam.HorizontalDataPermissionValidator;
import com.oceanbase.odc.service.iam.ProjectPermissionValidator;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.objectstorage.ObjectStorageFacade;
import com.oceanbase.odc.service.objectstorage.model.ObjectMetadata;
import com.oceanbase.odc.service.schedule.ScheduleService;
import com.oceanbase.odc.service.schedule.export.model.FileExportResponse;
import com.oceanbase.odc.service.schedule.export.model.ScheduleTaskExportRequest;
import com.oceanbase.odc.service.schedule.model.Schedule;
import com.oceanbase.odc.service.schedule.model.ScheduleMapper;
import com.oceanbase.odc.service.schedule.model.ScheduleType;

import lombok.AllArgsConstructor;
import lombok.Data;

@Service
public class ScheduleExportService {
    public static final String ASYNC_TASK_BASE_BUCKET = "scheduleexport";
    private static final Logger log = LoggerFactory.getLogger(ScheduleExportService.class);

    @Autowired
    private ScheduleTaskExporter scheduleTaskExporter;

    @Autowired
    private ObjectStorageFacade objectStorageFacade;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Autowired
    private ProjectPermissionValidator projectPermissionValidator;

    @Autowired
    private FlowInstanceService flowInstanceService;

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private HorizontalDataPermissionValidator horizontalDataPermissionValidator;

    @Autowired
    private ScheduleRepository scheduleRepository;

    private static String getPersonalBucketName(String userIdStr) {
        PreConditions.notEmpty(userIdStr, "userIdStr");
        return ASYNC_TASK_BASE_BUCKET.concat(File.separator).concat(userIdStr);
    }

    public FileExportResponse export(ScheduleTaskExportRequest request) {
        checkRequestIdsPermission(request);

        ExportProperties properties = scheduleTaskExporter.generateExportProperties();
        String encryptKey = new BCryptPasswordEncoder().encode(PasswordUtils.random());

        ExportedFile exportedFile = null;
        if (request.getScheduleType().equals(ScheduleType.PARTITION_PLAN)) {
            exportedFile = scheduleTaskExporter.exportPartitionPlan(encryptKey, properties,
                    request.getIds());
        } else {
            exportedFile = scheduleTaskExporter.exportSchedule(request.getScheduleType(), encryptKey,
                    properties, request.getIds());
        }
        return mapToFileExportResponse(exportedFile);
    }


    private void checkRequestIdsPermission(ScheduleTaskExportRequest request) {
        Set<Long> projectIds;
        if (request.getScheduleType().equals(ScheduleType.PARTITION_PLAN)) {
            List<FlowInstanceEntity> flowInstanceEntities = flowInstanceService.listByIds(request.getIds());
            projectIds =
                    flowInstanceEntities.stream().map(FlowInstanceEntity::getProjectId).collect(Collectors.toSet());
            List<FlowOrganizationIsolated> flowOrganizationIsolateds = flowInstanceEntities.stream().map(
                    f -> new FlowOrganizationIsolated(f.getOrganizationId(), f.getId())).collect(
                            Collectors.toList());
            horizontalDataPermissionValidator.checkCurrentOrganization(flowOrganizationIsolateds);
        } else {
            List<Schedule> scheduleEntities = scheduleRepository.findByIdIn(request.getIds()).stream()
                    .map(ScheduleMapper.INSTANCE::entityToModel).collect(
                            Collectors.toList());
            horizontalDataPermissionValidator.checkCurrentOrganization(scheduleEntities);
            projectIds = scheduleEntities.stream().map(Schedule::getProjectId).collect(Collectors.toSet());
        }
        if (authenticationFacade.currentOrganization().getType().equals(OrganizationType.TEAM)) {
            projectPermissionValidator.checkProjectRole(projectIds, ResourceRoleName.all());
        }
    }


    private FileExportResponse mapToFileExportResponse(ExportedFile exportedFile) {
        FileExportResponse fileExportResponse = new FileExportResponse();
        fileExportResponse.setSecret(exportedFile.getSecret());
        try {
            String bucketName = getPersonalBucketName(authenticationFacade.currentUserIdStr());
            objectStorageFacade.createBucketIfNotExists(bucketName);
            ObjectMetadata metadata = objectStorageFacade.putTempObject(bucketName, exportedFile.getFile());
            String downloadUrl = objectStorageFacade.getDownloadUrl(metadata.getBucketName(), metadata.getObjectId());
            fileExportResponse.setDownloadUrl(downloadUrl);
        } finally {
            OdcFileUtil.deleteFiles(exportedFile.getFile());
        }
        return fileExportResponse;
    }

    @Data
    @AllArgsConstructor
    private final static class FlowOrganizationIsolated implements OrganizationIsolated {

        private Long organizationId;
        private Long id;

        @Override
        public String resourceType() {
            return ResourceType.ODC_FLOW_INSTANCE.name();
        }

        @Override
        public Long organizationId() {
            return organizationId;
        }

        @Override
        public Long id() {
            return id;
        }
    }
}
