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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import com.google.gson.Gson;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.objectstorage.cloud.model.ObjectStorageConfiguration;
import com.oceanbase.odc.service.task.constants.JobConstants;
import com.oceanbase.odc.service.task.constants.JobEnvKeyConstants;
import com.oceanbase.odc.service.task.enums.TaskRunMode;
import com.oceanbase.odc.service.task.schedule.JobIdentity;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-11-15
 * @since 4.2.4
 */
@Slf4j
public class JobUtils {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    public static String generateJobName(JobIdentity ji) {
        return JobConstants.TEMPLATE_JOB_NAME_PREFIX + ji.getId() + "-" + LocalDateTime.now().format(DTF);
    }

    public static String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        // todo replace by jackson ConnectionConfig serialize ignore by @JsonProperty(value = "password",
        // access = Access.WRITE_ONLY)
        return new Gson().toJson(obj);
    }


    public static int getOdcServerPort() {
        String port = SystemUtils.getEnvOrProperty(JobEnvKeyConstants.ODC_SERVER_PORT);
        return port != null ? Integer.parseInt(port) : 8989;
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
        String userDir = SystemUtils.getEnvOrProperty("user.dir");
        return userDir != null ? userDir : "./data";
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
        config.setHost(System.getProperty(JobEnvKeyConstants.ODC_DATABASE_HOST));
        String port = System.getProperty(JobEnvKeyConstants.ODC_DATABASE_PORT);
        config.setPort(port != null ? Integer.parseInt(port) : 8989);
        config.setDefaultSchema(System.getProperty(JobEnvKeyConstants.ODC_DATABASE_NAME));
        config.setUsername(System.getProperty(JobEnvKeyConstants.ODC_DATABASE_USERNAME));
        config.setPassword(System.getProperty(JobEnvKeyConstants.ODC_DATABASE_PASSWORD));
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
}
