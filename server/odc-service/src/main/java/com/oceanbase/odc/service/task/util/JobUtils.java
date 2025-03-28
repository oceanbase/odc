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

package com.oceanbase.odc.service.task.util;

import java.io.File;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.core.alarm.AlarmUtils;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.metadb.resource.ResourceEntity;
import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.objectstorage.cloud.model.ObjectStorageConfiguration;
import com.oceanbase.odc.service.task.caller.DefaultExecutorIdentifier;
import com.oceanbase.odc.service.task.caller.ExecutorIdentifier;
import com.oceanbase.odc.service.task.caller.ExecutorIdentifierParser;
import com.oceanbase.odc.service.task.caller.JobEnvironmentEncryptor;
import com.oceanbase.odc.service.task.config.JobConfiguration;
import com.oceanbase.odc.service.task.constants.JobConstants;
import com.oceanbase.odc.service.task.constants.JobEnvKeyConstants;
import com.oceanbase.odc.service.task.enums.JobStatus;
import com.oceanbase.odc.service.task.enums.TaskRunMode;
import com.oceanbase.odc.service.task.exception.TaskRuntimeException;
import com.oceanbase.odc.service.task.executor.TaskResult;
import com.oceanbase.odc.service.task.jasypt.AccessEnvironmentJasyptEncryptorConfigProperties;
import com.oceanbase.odc.service.task.jasypt.DefaultJasyptEncryptor;
import com.oceanbase.odc.service.task.jasypt.JasyptEncryptorConfigProperties;
import com.oceanbase.odc.service.task.schedule.JobIdentity;
import com.oceanbase.odc.service.task.service.TaskFrameworkService;
import com.oceanbase.odc.service.task.supervisor.endpoint.ExecutorEndpoint;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-11-15
 * @since 4.2.4
 */
@Slf4j
public class JobUtils {

    public static String generateExecutorName(JobIdentity ji, Date jobCreateTime) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        String dateString = formatter.format(jobCreateTime);
        return JobConstants.TEMPLATE_JOB_NAME_PREFIX + ji.getId() + "-" + dateString;
    }

    public static String generateExecutorSelectorOnProcess(String executorName) {
        return JobConstants.ODC_EXECUTOR_PROCESS_PROPERTIES_KEY + "=" + executorName;
    }

    public static String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        // todo replace by jackson ConnectionConfig serialize ignore by @JsonProperty(value = "password",
        // access = Access.WRITE_ONLY)
        return new Gson().toJson(obj);
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null) {
            return null;
        }
        return new Gson().fromJson(json, clazz);
    }

    public static <T> T fromJson(String json, Type type) {
        if (json == null) {
            return null;
        }
        return new Gson().fromJson(json, type);
    }

    public static Map<String, String> fromJsonToMap(String json) {
        if (json == null) {
            return null;
        }
        Type mapType = new TypeToken<Map<String, String>>() {}.getType();
        return new Gson().fromJson(json, mapType);
    }


    public static Optional<Integer> getExecutorPort() {
        String port = SystemUtils.getEnvOrProperty(JobEnvKeyConstants.ODC_EXECUTOR_PORT);
        return port != null ? Optional.of(Integer.parseInt(port)) : Optional.empty();
    }

    public static void setExecutorPort(int port) {
        System.setProperty(JobEnvKeyConstants.ODC_EXECUTOR_PORT, port + "");
    }

    public static String getExecutorPoint() {
        return "http://" + SystemUtils.getLocalIpAddress() + ":" + JobUtils.getExecutorPort().get();
    }

    public static String getExecutorDataPath() {
        String userDir = SystemUtils.getEnvOrProperty("user.home");
        Verify.notBlank(userDir, "user.home is blank");
        return userDir.concat(File.separator).concat("data").concat(File.separator).concat("files");
    }

    public static boolean isK8sRunMode(TaskRunMode runMode) {
        return runMode == TaskRunMode.K8S;
    }

    public static boolean isProcessRunMode(TaskRunMode runMode) {
        return runMode == TaskRunMode.PROCESS;
    }


    public static boolean isK8sRunModeOfEnv() {
        return TaskRunMode.K8S.name().equals(SystemUtils.getEnvOrProperty(JobEnvKeyConstants.ODC_TASK_RUN_MODE));
    }

    public static boolean isProcessRunModeOfEnv() {
        return TaskRunMode.PROCESS.name()
                .equals(SystemUtils.getEnvOrProperty(JobEnvKeyConstants.ODC_TASK_RUN_MODE));
    }

    public static ConnectionConfig getMetaDBConnectionConfig() {
        ConnectionConfig config = new ConnectionConfig();
        config.setHost(System.getProperty(JobEnvKeyConstants.ODC_EXECUTOR_DATABASE_HOST));
        config.setPort(Integer.parseInt(System.getProperty(JobEnvKeyConstants.ODC_EXECUTOR_DATABASE_PORT)));
        config.setDefaultSchema(System.getProperty(JobEnvKeyConstants.ODC_EXECUTOR_DATABASE_NAME));
        config.setUsername(System.getProperty(JobEnvKeyConstants.ODC_EXECUTOR_DATABASE_USERNAME));
        JasyptEncryptorConfigProperties properties = new AccessEnvironmentJasyptEncryptorConfigProperties();
        config.setPassword(new DefaultJasyptEncryptor(properties)
                .decrypt(System.getProperty(JobEnvKeyConstants.ODC_EXECUTOR_DATABASE_PASSWORD)));
        config.setType(ConnectType.OB_MYSQL);

        log.info("get MetaDB configuration, config={}", config);

        // TODO: avoid hardcode here
        config.setId(1L);
        return config;
    }

    public static Optional<ObjectStorageConfiguration> getObjectStorageConfiguration() {
        String osc;
        if ((osc = System.getProperty(JobEnvKeyConstants.ODC_OBJECT_STORAGE_CONFIGURATION)) != null) {
            ObjectStorageConfiguration storageConfig = JsonUtils.fromJson(osc, ObjectStorageConfiguration.class);
            return Optional.of(storageConfig);
        }
        return Optional.empty();
    }

    public static Long getUserId() {
        String userId = SystemUtils.getEnvOrProperty(JobEnvKeyConstants.ODC_EXECUTOR_USER_ID);
        return Long.parseLong(userId);
    }

    public static boolean isReportEnabled() {
        return !isReportDisabled();
    }

    public static boolean isReportDisabled() {
        String reportEnabledValue = SystemUtils.getEnvOrProperty(JobEnvKeyConstants.REPORT_ENABLED);
        return reportEnabledValue != null && Objects.equals(Boolean.valueOf(reportEnabledValue), Boolean.FALSE);
    }

    public static void putEnvToSysProperties(String environmentKey) {
        if (System.getenv(environmentKey) != null) {
            System.setProperty(environmentKey, System.getenv(environmentKey));
        }
    }

    public static void encryptEnvironments(Map<String, String> environments) {
        new JobEnvironmentEncryptor().encrypt(environments);
    }

    public static String encrypt(String key, String salt, String raw) {
        return new JobEnvironmentEncryptor().encrypt(key, salt, raw);
    }

    public static String decrypt(String key, String salt, String encrypted) {
        return new JobEnvironmentEncryptor().decrypt(key, salt, encrypted);
    }

    public static String retrieveJobResultStr(JobEntity jobEntity) {
        TaskResult taskResult = JsonUtils.fromJson(jobEntity.getResultJson(), TaskResult.class);
        return null == taskResult ? null : taskResult.getResultJson();
    }

    public static void updateStatusAndCheck(Long jobId, JobStatus oldStatus, JobStatus newStatus,
            TaskFrameworkService taskFrameworkService) {
        int rows = taskFrameworkService.updateStatusByIdOldStatus(jobId, oldStatus, newStatus);
        if (rows <= 0) {
            throw new TaskRuntimeException(
                    "Update job status from " + oldStatus + "to " + newStatus + " failed, jobId=" + jobId);
        }
    }

    public static void alarmJobEvent(JobEntity jobEntity, String eventName, String message) {
        AlarmUtils.alarm(eventName,
                AlarmUtils.createAlarmMapBuilder()
                        .item(AlarmUtils.ORGANIZATION_NAME, Optional.ofNullable(jobEntity.getOrganizationId()).map(
                                Object::toString).orElse(StrUtil.EMPTY))
                        .item(AlarmUtils.TASK_JOB_ID_NAME, String.valueOf(jobEntity.getId()))
                        .item(AlarmUtils.MESSAGE_NAME, message)
                        .build());
    }

    public static void alarmResourceEvent(ResourceEntity resourceEntity, String eventName, String message) {
        Map<String, String> eventMessage = AlarmUtils.createAlarmMapBuilder()
                .item(AlarmUtils.ORGANIZATION_NAME, AlarmUtils.ODC_RESOURCE)
                .item(AlarmUtils.RESOURCE_ID_NAME, String.valueOf(resourceEntity.getId()))
                .item(AlarmUtils.RESOURCE_TYPE, resourceEntity.getResourceType())
                .item(AlarmUtils.MESSAGE_NAME, message)
                .build();
        AlarmUtils.alarm(eventName, eventMessage);
    }

    public static String getLogBasePath(String mountPath) {
        return StringUtils.isNotBlank(mountPath) ? mountPath
                : JobConstants.ODC_EXECUTOR_DEFAULT_MOUNT_PATH;
    }

    public static Integer getODCServerPort(JobConfiguration configuration) {
        String portString = Optional.ofNullable(configuration.getHostProperties().getPort())
                .orElse(DefaultExecutorIdentifier.DEFAULT_PORT + "");
        return Integer.valueOf(portString);
    }

    // for process mode, correct real listen port to odc server port, for log request route
    public static ExecutorIdentifier getCorrectedExecutorIdentifier(ExecutorEndpoint endpoint,
            JobConfiguration configuration) {
        ExecutorIdentifier executorIdentifier = ExecutorIdentifierParser.parser(endpoint.getIdentifier());
        if (configuration.getTaskFrameworkProperties().getRunMode() == TaskRunMode.PROCESS) {
            return DefaultExecutorIdentifier.builder().host(executorIdentifier.getHost())
                    .port(endpoint.getSupervisorOwnerPort())
                    .protocol(executorIdentifier.getProtocol())
                    .namespace(executorIdentifier.getNamespace())
                    .executorName(executorIdentifier.getExecutorName())
                    .build();
        } else {
            return executorIdentifier;
        }
    }
}
