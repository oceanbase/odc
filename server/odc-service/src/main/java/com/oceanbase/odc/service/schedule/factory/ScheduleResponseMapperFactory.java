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
package com.oceanbase.odc.service.schedule.factory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.metadb.iam.UserRepository;
import com.oceanbase.odc.metadb.schedule.LatestTaskMappingEntity;
import com.oceanbase.odc.metadb.schedule.LatestTaskMappingRepository;
import com.oceanbase.odc.metadb.schedule.ScheduleEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskRepository;
import com.oceanbase.odc.service.common.model.InnerUser;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.dlm.DlmLimiterService;
import com.oceanbase.odc.service.dlm.model.DataArchiveParameters;
import com.oceanbase.odc.service.dlm.model.DataArchiveTableConfig;
import com.oceanbase.odc.service.dlm.model.DataDeleteParameters;
import com.oceanbase.odc.service.quartz.util.QuartzCronExpressionUtils;
import com.oceanbase.odc.service.schedule.model.DataArchiveAttributes;
import com.oceanbase.odc.service.schedule.model.DataDeleteAttributes;
import com.oceanbase.odc.service.schedule.model.Schedule;
import com.oceanbase.odc.service.schedule.model.ScheduleDetailResp;
import com.oceanbase.odc.service.schedule.model.ScheduleOverview;
import com.oceanbase.odc.service.schedule.model.ScheduleOverviewAttributes;
import com.oceanbase.odc.service.schedule.model.ScheduleTaskParameters;
import com.oceanbase.odc.service.schedule.model.ScheduleType;
import com.oceanbase.odc.service.schedule.model.TriggerConfig;

import lombok.NonNull;

/**
 * @Author：tinker
 * @Date: 2022/12/6 11:31
 * @Descripition:
 */

@Component
public class ScheduleResponseMapperFactory {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DatabaseService databaseService;

    @Autowired
    private ConnectionService dataSourceService;

    @Autowired
    private ScheduleTaskRepository scheduleTaskRepository;

    @Autowired
    private LatestTaskMappingRepository latestTaskMappingRepository;
    @Autowired
    private DlmLimiterService limiterService;



    public ScheduleDetailResp generateScheduleDetailResp(@NonNull Schedule schedule) {
        ScheduleDetailResp scheduleDetailResp = new ScheduleDetailResp();

        scheduleDetailResp.setScheduleId(schedule.getId());
        scheduleDetailResp.setTriggerConfig(schedule.getTriggerConfig());
        scheduleDetailResp.setMisfireStrategy(schedule.getMisfireStrategy());
        scheduleDetailResp.setAllowConcurrent(schedule.getAllowConcurrent());

        scheduleDetailResp.setType(schedule.getType());
        scheduleDetailResp.setStatus(schedule.getStatus());
        scheduleDetailResp.setCreateTime(schedule.getCreateTime());
        scheduleDetailResp.setUpdateTime(schedule.getUpdateTime());
        scheduleDetailResp.setProjectId(schedule.getProjectId());
        scheduleDetailResp.setDescription(schedule.getDescription());

        scheduleDetailResp.setNextFireTimes(
                QuartzCronExpressionUtils.getNextFiveFireTimes(schedule.getTriggerConfig()));
        userRepository.findById(schedule.getCreatorId())
                .ifPresent(o -> scheduleDetailResp.setCreator(new InnerUser(o, null)));

        scheduleDetailResp.setType(schedule.getType());
        scheduleDetailResp.setParameters(detailParameters(schedule));

        return scheduleDetailResp;
    }

    public Map<Long, ScheduleOverview> generateScheduleOverviewListMapper(
            @NonNull Collection<ScheduleEntity> schedules) {

        if (schedules.isEmpty()) {
            return Collections.emptyMap();
        }

        Set<Long> scheduleIds = schedules.stream().map(ScheduleEntity::getId).collect(Collectors.toSet());

        Map<Long, Long> scheduleId2ScheduleTaskId =
                latestTaskMappingRepository.findByScheduleIdIn(scheduleIds).stream().collect(
                        Collectors.toMap(LatestTaskMappingEntity::getScheduleId,
                                LatestTaskMappingEntity::getLatestScheduleTaskId));

        Map<Long, ScheduleTaskEntity> scheduleId2ScheduleTask;
        if (scheduleId2ScheduleTaskId.isEmpty()) {
            scheduleId2ScheduleTask = new HashMap<>();
        } else {
            scheduleId2ScheduleTask =
                    scheduleTaskRepository.findByIdIn(new HashSet<>(scheduleId2ScheduleTaskId.values()))
                            .stream().collect(Collectors.toMap(o -> Long.parseLong(o.getJobName()), o -> o));
        }

        Map<Long, ScheduleOverviewAttributes> id2Attributes = generateAttributes(schedules);
        return schedules.stream().map(o -> {
            ScheduleOverview overview = new ScheduleOverview();
            overview.setScheduleId(o.getId());
            overview.setStatus(o.getStatus());
            overview.setTriggerConfig(JsonUtils.fromJson(o.getTriggerConfigJson(), TriggerConfig.class));
            overview.setAttributes(JSON.parseObject(JSON.toJSONString(id2Attributes.get(o.getId()))));
            if (scheduleId2ScheduleTaskId.containsKey(o.getId())) {
                ScheduleTaskEntity scheduleTask = scheduleId2ScheduleTask.get(o.getId());
                overview.setLatestFireTime(scheduleTask.getFireTime());
                overview.setLatestExecutionStatus(scheduleTask.getStatus());
            }
            return overview;
        }).collect(Collectors.toMap(ScheduleOverview::getScheduleId, o -> o));
    }

    private Map<Long, ScheduleOverviewAttributes> generateAttributes(Collection<ScheduleEntity> schedules) {
        Map<ScheduleType, List<ScheduleEntity>> type2Entity = schedules.stream().collect(
                Collectors.groupingBy(ScheduleEntity::getType));
        Map<Long, ScheduleOverviewAttributes> id2Attributes = new HashMap<>();
        type2Entity.forEach((k, v) -> {
            switch (k) {
                case DATA_DELETE: {
                    Set<Long> databaseIds = new HashSet<>();
                    v.forEach(o -> {
                        DataDeleteParameters dataDeleteParameters = JsonUtils.fromJson(o.getJobParametersJson(),
                                DataDeleteParameters.class);
                        databaseIds.add(dataDeleteParameters.getDatabaseId());
                    });
                    Map<Long, Database> id2Database = getDatabaseByIds(databaseIds).stream().collect(
                            Collectors.toMap(Database::getId, o -> o));

                    v.forEach(o -> {
                        DataDeleteAttributes attributes = new DataDeleteAttributes();
                        attributes.setSourceDataBaseInfo(id2Database.get(o.getDatabaseId()));
                        DataDeleteParameters dataDeleteParameters =
                                JsonUtils.fromJson(o.getJobParametersJson(), DataDeleteParameters.class);
                        attributes.setTableNames(dataDeleteParameters.getTables().stream().map(
                                DataArchiveTableConfig::getTableName).collect(Collectors.toList()));
                        id2Attributes.put(o.getId(), attributes);
                    });
                    break;
                }
                case DATA_ARCHIVE: {
                    Set<Long> databaseIds = new HashSet<>();
                    v.forEach(o -> {
                        DataArchiveParameters parameters = JsonUtils.fromJson(o.getJobParametersJson(),
                                DataArchiveParameters.class);
                        databaseIds.add(parameters.getSourceDatabaseId());
                        databaseIds.add(parameters.getTargetDataBaseId());
                    });
                    Map<Long, Database> id2Database = getDatabaseByIds(databaseIds).stream().collect(
                            Collectors.toMap(Database::getId, o -> o));

                    v.forEach(o -> {
                        DataArchiveParameters parameters = JsonUtils.fromJson(o.getJobParametersJson(),
                                DataArchiveParameters.class);
                        DataArchiveAttributes attributes = new DataArchiveAttributes();
                        attributes.setSourceDataBaseInfo(id2Database.get(parameters.getSourceDatabaseId()));
                        attributes.setTargetDataBaseInfo(id2Database.get(parameters.getTargetDataBaseId()));
                        id2Attributes.put(o.getId(), attributes);
                    });
                    break;
                }
                default:
                    break;
            }
        });
        return id2Attributes;
    }

    private List<Database> getDatabaseByIds(Set<Long> ids) {
        List<Database> databases = databaseService.listDatabasesByIds(ids);
        Set<Long> connectionIds =
                databases.stream().filter(e -> e.getDataSource() != null && e.getDataSource().getId() != null)
                        .map(e -> e.getDataSource().getId()).collect(Collectors.toSet());
        Map<Long, ConnectionConfig> id2Connection = dataSourceService.innerListByIds(connectionIds)
                .stream().collect(Collectors.toMap(ConnectionConfig::getId, o -> o));
        databases.forEach(database -> {
            if (id2Connection.containsKey(database.getDataSource().getId())) {
                database.setDataSource(id2Connection.get(database.getDataSource().getId()));
            }
        });
        return databases;
    }

    private ScheduleTaskParameters detailParameters(Schedule schedule) {
        switch (schedule.getType()) {
            case DATA_ARCHIVE: {
                DataArchiveParameters parameters = (DataArchiveParameters) schedule.getParameters();
                Map<Long, Database> id2Database = getDatabaseByIds(
                        Stream.of(parameters.getSourceDatabaseId(), parameters.getTargetDataBaseId()).collect(
                                Collectors.toSet())).stream().collect(Collectors.toMap(Database::getId, o -> o));
                parameters.setSourceDatabase(id2Database.get(parameters.getSourceDatabaseId()));
                parameters.setTargetDatabase(id2Database.get(parameters.getTargetDataBaseId()));
                limiterService.findByScheduleId(schedule.getId()).ifPresent(parameters::setRateLimit);
                return parameters;
            }
            case DATA_DELETE: {
                DataDeleteParameters parameters = (DataDeleteParameters) schedule.getParameters();
                Set<Long> databaseIds = new HashSet<>();
                databaseIds.add(parameters.getDatabaseId());
                if (parameters.getTargetDatabaseId() != null) {
                    databaseIds.add(parameters.getTargetDatabaseId());
                }
                Map<Long, Database> id2Database = getDatabaseByIds(databaseIds)
                        .stream().collect(Collectors.toMap(Database::getId, o -> o));
                parameters.setDatabase(id2Database.get(parameters.getDatabaseId()));
                parameters.setTargetDatabase(id2Database.get(parameters.getTargetDatabaseId()));
                limiterService.findByScheduleId(schedule.getId()).ifPresent(parameters::setRateLimit);
                return parameters;
            }
            default:
                return schedule.getParameters();
        }
    }
}
