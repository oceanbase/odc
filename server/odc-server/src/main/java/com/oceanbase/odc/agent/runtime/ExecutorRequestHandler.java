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
package com.oceanbase.odc.agent.runtime;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections4.MapUtils;

import com.oceanbase.odc.common.util.ExceptionUtils;
import com.oceanbase.odc.common.util.ObjectUtil;
import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.common.util.UrlUtils;
import com.oceanbase.odc.service.task.constants.JobExecutorUrls;
import com.oceanbase.odc.service.task.executor.TaskResult;
import com.oceanbase.odc.service.task.executor.logger.LogBiz;
import com.oceanbase.odc.service.task.executor.logger.LogBizImpl;
import com.oceanbase.odc.service.task.executor.logger.LogUtils;
import com.oceanbase.odc.service.task.schedule.JobIdentity;
import com.oceanbase.odc.service.task.util.JobUtils;

import io.netty.handler.codec.http.HttpMethod;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2024-01-17
 * @since 4.2.4
 */
@Slf4j
class ExecutorRequestHandler {

    private final Pattern queryLogUrlPattern = Pattern.compile(String.format(JobExecutorUrls.QUERY_LOG, "([0-9]+)"));
    private final Pattern stopTaskPattern = Pattern.compile(String.format(JobExecutorUrls.STOP_TASK, "([0-9]+)"));
    private final Pattern getResultPattern = Pattern.compile(String.format(JobExecutorUrls.GET_RESULT, "([0-9]+)"));
    private final Pattern modifyParametersPattern =
            Pattern.compile(String.format(JobExecutorUrls.MODIFY_JOB_PARAMETERS, "([0-9]+)"));

    private final LogBiz executorBiz;

    public ExecutorRequestHandler() {
        this.executorBiz = new LogBizImpl();
    }

    public SuccessResponse<Object> process(HttpMethod httpMethod, String uri, String requestData) {
        if (uri == null || uri.trim().isEmpty()) {
            return Responses.single("request error: uri is empty.");
        }

        try {
            // services mapping
            String path = UrlUtils.getPath(uri);
            Matcher matcher = queryLogUrlPattern.matcher(path);
            if (matcher.find()) {
                String maxLine = UrlUtils.getQueryParameterFirst(uri, "fetchMaxLine");
                String maxSize = UrlUtils.getQueryParameterFirst(uri, "fetchMaxByteSize");
                Long jobId = Long.parseLong(matcher.group(1));
                String logContent = executorBiz.getLog(jobId,
                        UrlUtils.getQueryParameterFirst(uri, "logType"),
                        (maxLine == null ? LogUtils.CONTENT_MAX_LINES : Long.parseLong(maxLine)),
                        (maxSize == null ? LogUtils.CONTENT_MAX_SIZE : Long.parseLong(maxSize)));
                return Responses.single(logContent);
            }
            matcher = stopTaskPattern.matcher(path);
            if (matcher.find()) {
                JobIdentity ji = getJobIdentity(matcher);
                boolean result = ThreadPoolTaskExecutor.getInstance().cancel(ji);
                return Responses.ok(result);
            }

            matcher = modifyParametersPattern.matcher(path);
            if (matcher.find()) {
                JobIdentity ji = getJobIdentity(matcher);
                TaskRuntimeInfo runtimeInfo = ThreadPoolTaskExecutor.getInstance().getTaskRuntimeInfo(ji);
                boolean result = runtimeInfo.getTaskContainer().modify(JobUtils.fromJsonToMap(requestData));
                return Responses.ok(result);
            }

            matcher = getResultPattern.matcher(path);
            if (matcher.find()) {
                JobIdentity ji = getJobIdentity(matcher);
                TaskRuntimeInfo runtimeInfo = ThreadPoolTaskExecutor.getInstance().getTaskRuntimeInfo(ji);
                TaskMonitor taskMonitor = runtimeInfo.getTaskMonitor();
                TaskResult result = DefaultTaskResultBuilder.build(runtimeInfo.getTaskContainer());
                if (taskMonitor != null && MapUtils.isNotEmpty(taskMonitor.getLogMetadata())) {
                    result.setLogMetadata(taskMonitor.getLogMetadata());
                    // assign final error message
                    DefaultTaskResultBuilder.assignErrorMessage(result, runtimeInfo.getTaskContainer().getError());
                    taskMonitor.markLogMetaCollected();
                    log.info("Task log metadata collected, ji={}.", ji.getId());
                }
                TaskResult copiedResult = ObjectUtil.deepCopy(result, TaskResult.class);
                return Responses.ok(copiedResult);
            }

            return Responses.single("invalid request, uri-mapping(" + uri + ") not found.");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Responses.single("request error:" + ExceptionUtils.getRootCauseReason(e));
        }
    }

    private static JobIdentity getJobIdentity(Matcher matcher) {
        return JobIdentity.of(Long.parseLong(matcher.group(1)));
    }

}
