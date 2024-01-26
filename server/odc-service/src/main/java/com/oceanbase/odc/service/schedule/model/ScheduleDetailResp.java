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
package com.oceanbase.odc.service.schedule.model;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.oceanbase.odc.common.i18n.Internationalizable;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.OrganizationIsolated;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.metadb.flow.FlowInstanceEntity;
import com.oceanbase.odc.metadb.iam.UserEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleEntity;
import com.oceanbase.odc.service.common.model.InnerUser;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.dlm.model.DataArchiveParameters;
import com.oceanbase.odc.service.dlm.model.DataDeleteParameters;
import com.oceanbase.odc.service.dlm.model.RateLimitConfiguration;
import com.oceanbase.odc.service.quartz.model.MisfireStrategy;
import com.oceanbase.odc.service.quartz.util.QuartzCronExpressionUtils;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

/**
 * @Author：tinker
 * @Date: 2022/11/22 14:38
 * @Descripition:
 */

@Data
public class ScheduleDetailResp implements OrganizationIsolated {

    private Long id;
    private Long organizationId;
    private JobType type;
    private Long databaseId;
    private InnerConnection datasource;
    private Long projectId;
    private String databaseName;
    private InnerUser creator;
    private Date createTime;
    private Date updateTime;
    private ScheduleStatus status;
    @Internationalizable
    private String description;
    private boolean approvable;
    private Long approveInstanceId;
    private Boolean allowConcurrent;
    private MisfireStrategy misfireStrategy;
    private Set<InnerUser> candidateApprovers;

    private List<Date> nextFireTimes;
    private List<FlowInstanceEntity> jobs;

    @JsonRawValue
    private String jobParameters;
    @JsonRawValue
    private String triggerConfig;

    public static ScheduleDetailResp withId(@NonNull Long id) {
        ScheduleDetailResp resp = new ScheduleDetailResp();
        resp.setId(id);
        return resp;
    }

    @Override
    public String resourceType() {
        return ResourceType.ODC_SCHEDULE.name();
    }

    @Override
    public Long organizationId() {
        return this.organizationId;
    }

    @Override
    public Long id() {
        return this.id;
    }

    @Getter
    @ToString
    @EqualsAndHashCode
    public static class InnerConnection {
        private final Long id;
        private final String name;
        private final DialectType dbMode;

        public InnerConnection(@NonNull ConnectionConfig config) {
            this.id = config.getId();
            this.name = config.getName();
            this.dbMode = config.getDialectType();
        }
    }

    public static class ScheduleResponseMapper {

        private Function<Long, UserEntity> getUserById = null;
        private Function<Long, Long> getApproveInstanceIdById = null;

        private Function<Long, ConnectionConfig> getDatasourceById = null;

        private Function<Long, Set<UserEntity>> getCandidatesById = null;

        private Function<Long, RateLimitConfiguration> getDLMRateLimitConfigurationById = null;


        public ScheduleResponseMapper withGetUserById(@NonNull Function<Long, UserEntity> getUserById) {
            this.getUserById = getUserById;
            return this;
        }

        public ScheduleResponseMapper withGetApproveInstanceIdById(
                @NonNull Function<Long, Long> getApproveInstanceIdById) {
            this.getApproveInstanceIdById = getApproveInstanceIdById;
            return this;
        }

        public ScheduleResponseMapper withGetDatasourceById(
                @NonNull Function<Long, ConnectionConfig> getDatasourceById) {
            this.getDatasourceById = getDatasourceById;
            return this;
        }

        public ScheduleResponseMapper withGetCandidatesById(
                @NonNull Function<Long, Set<UserEntity>> getCandidatesById) {
            this.getCandidatesById = getCandidatesById;
            return this;
        }

        public ScheduleResponseMapper withGetDLMRateLimitConfigurationById(
                @NonNull Function<Long, RateLimitConfiguration> getCandidatesById) {
            this.getDLMRateLimitConfigurationById = getCandidatesById;
            return this;
        }


        public ScheduleDetailResp map(@NonNull ScheduleEntity entity) {

            ScheduleDetailResp resp = new ScheduleDetailResp();
            resp.setId(entity.getId());
            resp.setType(entity.getJobType());
            resp.setStatus(entity.getStatus());

            resp.setOrganizationId(entity.getOrganizationId());
            resp.setDatabaseId(entity.getDatabaseId());
            resp.setProjectId(entity.getProjectId());
            resp.setDatabaseName(entity.getDatabaseName());
            ConnectionConfig datasource = getDatasourceById.apply(entity.getConnectionId());
            if (datasource != null) {
                resp.setDatasource(new InnerConnection(datasource));
            }
            resp.setJobParameters(getJobParameters(entity));
            resp.setTriggerConfig(entity.getTriggerConfigJson());
            resp.setNextFireTimes(
                    QuartzCronExpressionUtils.getNextFireTimes(JsonUtils.fromJson(entity.getTriggerConfigJson(),
                            TriggerConfig.class).getCronExpression()));
            UserEntity user = getUserById.apply(entity.getCreatorId());
            if (user != null) {
                resp.setCreator(new InnerUser(user));
            }
            resp.setCreateTime(entity.getCreateTime());
            resp.setUpdateTime(entity.getUpdateTime());
            resp.setDescription(entity.getDescription());

            Long approveInstanceId = getApproveInstanceIdById.apply(entity.getId());
            resp.setApprovable(approveInstanceId != null);
            resp.setApproveInstanceId(approveInstanceId);

            resp.setMisfireStrategy(entity.getMisfireStrategy());
            resp.setAllowConcurrent(entity.getAllowConcurrent());
            Set<UserEntity> candidates = getCandidatesById.apply(entity.getId());
            if (CollectionUtils.isNotEmpty(candidates)) {
                resp.setCandidateApprovers(candidates.stream().map(InnerUser::new).collect(Collectors.toSet()));
            }

            return resp;
        }

        private String getJobParameters(ScheduleEntity entity) {
            RateLimitConfiguration rateLimitConfig = getDLMRateLimitConfigurationById.apply(entity.getId());
            if (rateLimitConfig == null) {
                return entity.getJobParametersJson();
            }
            switch (entity.getJobType()) {
                case DATA_ARCHIVE: {
                    DataArchiveParameters parameters =
                            JsonUtils.fromJson(entity.getJobParametersJson(), DataArchiveParameters.class);
                    parameters.setRateLimit(rateLimitConfig);
                    return JsonUtils.toJson(parameters);
                }
                case DATA_DELETE: {
                    DataDeleteParameters parameters =
                            JsonUtils.fromJson(entity.getJobParametersJson(), DataDeleteParameters.class);
                    parameters.setRateLimit(rateLimitConfig);
                    return JsonUtils.toJson(parameters);

                }
                default: {
                    return entity.getJobParametersJson();
                }
            }

        }

    }
}
