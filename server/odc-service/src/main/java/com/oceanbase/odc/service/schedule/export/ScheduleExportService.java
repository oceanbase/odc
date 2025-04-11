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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.common.task.RouteLogCallable;
import com.oceanbase.odc.core.shared.OrganizationIsolated;
import com.oceanbase.odc.core.shared.constant.OrganizationType;
import com.oceanbase.odc.core.shared.constant.ResourceRoleName;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.metadb.flow.FlowInstanceEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleRepository;
import com.oceanbase.odc.metadb.task.TaskEntity;
import com.oceanbase.odc.service.common.FutureCache;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.flow.FlowInstanceService;
import com.oceanbase.odc.service.iam.HorizontalDataPermissionValidator;
import com.oceanbase.odc.service.iam.ProjectPermissionValidator;
import com.oceanbase.odc.service.iam.UserService;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.iam.model.User;
import com.oceanbase.odc.service.objectstorage.ObjectStorageFacade;
import com.oceanbase.odc.service.schedule.ScheduleService;
import com.oceanbase.odc.service.schedule.export.model.FileExportResponse;
import com.oceanbase.odc.service.schedule.export.model.ScheduleExportListView;
import com.oceanbase.odc.service.schedule.export.model.ScheduleTaskExportRequest;
import com.oceanbase.odc.service.schedule.model.Schedule;
import com.oceanbase.odc.service.schedule.model.ScheduleMapper;
import com.oceanbase.odc.service.schedule.model.ScheduleType;
import com.oceanbase.odc.service.state.StatefulUuidStateIdGenerator;
import com.oceanbase.odc.service.task.executor.logger.LogUtils;

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
    private UserService userService;

    @Autowired
    private HorizontalDataPermissionValidator horizontalDataPermissionValidator;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private StatefulUuidStateIdGenerator statefulUuidStateIdGenerator;

    @Autowired
    private ThreadPoolTaskExecutor scheduleImportExecutor;

    @Autowired
    private DatabaseService databaseService;

    @Autowired
    private FutureCache futureCache;

    @Value("${odc.log.directory:./log}")
    private String logPath;

    private String getPersonalBucketName() {
        return ASYNC_TASK_BASE_BUCKET.concat(File.separator).concat(authenticationFacade.currentUserIdStr());
    }

    public String startExport(ScheduleTaskExportRequest request) {
        checkRequestIdsPermission(request);
        String previewId = statefulUuidStateIdGenerator.generateCurrentUserIdStateId("scheduleExport");
        User user = authenticationFacade.currentUser();
        Future<FileExportResponse> future = scheduleImportExecutor.submit(
                new ScheduleTaskExportCallable(previewId, request, user, scheduleTaskExporter,
                        getPersonalBucketName(), objectStorageFacade));
        futureCache.put(previewId, future);
        return previewId;
    }

    public List<ScheduleExportListView> getExportListView(ScheduleTaskExportRequest request) {
        checkRequestIdsPermission(request);
        if (request.getScheduleType().equals(ScheduleType.PARTITION_PLAN)) {
            return getPartitionPlanView(request);
        }
        List<ScheduleEntity> scheduleEntities = scheduleRepository.findByIdIn(request.getIds());
        List<ScheduleExportListView> view = scheduleEntities.stream().map(
                ScheduleMapper.INSTANCE::toScheduleExportListView).collect(Collectors.toList());
        databaseService.assignDatabaseById(view, ScheduleExportListView::getDatabaseId,
                ScheduleExportListView::setDatabase);
        return view;
    }

    private List<ScheduleExportListView> getPartitionPlanView(ScheduleTaskExportRequest request) {
        Map<Long, TaskEntity> flowIn2TaskMap = flowInstanceService.getTaskByFlowInstanceIds(
                request.getIds());
        List<FlowInstanceEntity> flowInstanceEntities = flowInstanceService.listByIds(request.getIds());
        List<ScheduleExportListView> views = flowInstanceEntities.stream().map(f -> {
            TaskEntity taskEntity = flowIn2TaskMap.get(f.getId());
            ScheduleExportListView scheduleExportListView = new ScheduleExportListView();
            scheduleExportListView.setScheduleType(ScheduleType.PARTITION_PLAN);
            scheduleExportListView.setId(f.getId());
            scheduleExportListView.setCreatorId(f.getCreatorId());
            scheduleExportListView.setDatabaseId(taskEntity.getDatabaseId());
            scheduleExportListView.setCreateTime(f.getCreateTime());
            scheduleExportListView.setDescription(f.getDescription());
            return scheduleExportListView;
        }).collect(Collectors.toList());
        databaseService.assignDatabaseById(views, ScheduleExportListView::getDatabaseId,
                ScheduleExportListView::setDatabase);
        userService.assignInnerUserByCreatorId(views, ScheduleExportListView::getCreatorId,
                ScheduleExportListView::setCreator);
        return views;
    }

    public FileExportResponse getExportResult(String exportId) {
        statefulUuidStateIdGenerator.checkCurrentUserId(exportId);
        Future<?> future = futureCache.get(exportId);
        if (future == null) {
            return null;
        }
        if (!future.isDone()) {
            return FileExportResponse.exporting();
        }
        try {
            futureCache.invalid(exportId);
            return (FileExportResponse) future.get();
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public String getExportLog(String exportId) {
        statefulUuidStateIdGenerator.checkCurrentUserId(exportId);
        String filePath = String.format(RouteLogCallable.LOG_PATH_PATTERN, logPath,
                ScheduleTaskExportCallable.WORK_SPACE, exportId,
                ScheduleTaskExportCallable.LOG_NAME);
        File logFile = new File(filePath);
        return LogUtils.getLatestLogContent(logFile, 10000L, 1048576L);
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
