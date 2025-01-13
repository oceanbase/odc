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

package com.oceanbase.odc.service.task.executor.logger;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Optional;

import org.apache.commons.io.input.ReversedLinesFileReader;

import com.oceanbase.odc.common.unit.BinarySizeUnit;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.service.task.constants.JobEnvKeyConstants;
import com.oceanbase.odc.service.task.model.OdcTaskLogLevel;

import cn.hutool.core.io.FileUtil;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-12-13
 * @since 4.2.4
 */
@Slf4j
public class LogUtils {

    public final static String DEFAULT_LOG_CONTENT = "read log failed, may be log file not exist";
    public final static String TASK_LOG_PATH_PATTERN = "%s/task/%s/task-log.%s";
    public final static String SCHEDULE_LOG_FILE_NAME_PATTERN = "odc_schedule_%d_task_%d.log";
    public final static long CONTENT_MAX_LINES = 10000L;
    public final static long CONTENT_MAX_SIZE = 1024L * 1024;

    public static String getLatestLogContent(String file, Long fetchMaxLine, Long fetchMaxByteSize) {
        File logFile = new File(file);
        if (!logFile.exists()) {
            return ErrorCodes.TaskLogNotFound.getLocalizedMessage(new Object[] {logFile.getAbsoluteFile()});
        }
        return getLatestLogContent(logFile, fetchMaxLine, fetchMaxByteSize);
    }

    public static String getLatestLogContent(@NonNull File file, Long fetchMaxLine, Long fetchMaxByteSize) {
        if (!FileUtil.exist(file)) {
            log.warn(ErrorCodes.TaskLogNotFound.getLocalizedMessage(new Object[] {file.getAbsolutePath()}));
            return DEFAULT_LOG_CONTENT;
        }
        LinkedList<String> logContent = new LinkedList<>();
        try (ReversedLinesFileReader reader = new ReversedLinesFileReader(file, StandardCharsets.UTF_8)) {
            int bytes = 0;
            String line;
            while ((line = reader.readLine()) != null) {
                bytes += line.getBytes().length;
                if (logContent.size() >= fetchMaxLine || bytes >= fetchMaxByteSize) {
                    logContent.add(String.format("[ODC INFO]: \n"
                            + "Logs exceed max limitation (%s rows or %s), only the latest part is displayed.\n"
                            + "Please download the log file for the full content. ", fetchMaxLine,
                            BinarySizeUnit.B.of(fetchMaxByteSize).convert(BinarySizeUnit.MB)));
                    break;
                }
                logContent.addFirst(line + "\n");
            }

        } catch (Exception ex) {
            log.warn("Read task log file failed, details={}", ex.getMessage());
            throw new UnexpectedException("Read task log file failed, details: " + ex.getMessage(), ex);
        }
        StringBuilder logBuilder = new StringBuilder();
        logContent.forEach(logBuilder::append);
        return logBuilder.toString();
    }

    public static String getTaskLogFileWithPath(Long jobId, OdcTaskLogLevel logType) {
        return String.format(TASK_LOG_PATH_PATTERN, getBaseLogPath(), jobId, logType.getName().toLowerCase());
    }

    public static String getBaseLogPath() {
        return Optional.ofNullable(System.getProperty(JobEnvKeyConstants.ODC_LOG_DIRECTORY)).orElse("./log");
    }

    public static String generateScheduleTaskLogFileName(@NonNull Long scheduleId, Long scheduleTaskId) {
        return String.format(SCHEDULE_LOG_FILE_NAME_PATTERN, scheduleId, scheduleTaskId);
    }

}
