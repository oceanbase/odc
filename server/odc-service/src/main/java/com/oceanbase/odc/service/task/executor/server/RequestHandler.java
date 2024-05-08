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
package com.oceanbase.odc.service.task.executor.server;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.oceanbase.odc.common.util.ExceptionUtils;
import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.common.util.UrlUtils;
import com.oceanbase.odc.service.task.constants.JobUrlConstants;
import com.oceanbase.odc.service.task.executor.logger.LogBiz;
import com.oceanbase.odc.service.task.executor.logger.LogBizImpl;
import com.oceanbase.odc.service.task.executor.logger.LogUtils;
import com.oceanbase.odc.service.task.executor.task.Task;
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
public class RequestHandler {

    private final Pattern logUrlPattern = Pattern.compile(String.format(JobUrlConstants.LOG_QUERY, "([0-9]+)"));
    private final Pattern stopTaskPattern = Pattern.compile(String.format(JobUrlConstants.STOP_TASK, "([0-9]+)"));
    private final Pattern modifyParametersPattern =
            Pattern.compile(String.format(JobUrlConstants.MODIFY_JOB_PARAMETERS, "([0-9]+)"));
    private final LogBiz executorBiz;

    public RequestHandler() {
        this.executorBiz = new LogBizImpl();
    }

    public SuccessResponse<Object> process(HttpMethod httpMethod, String uri, String requestData) {

        if (uri == null || uri.trim().length() == 0) {
            return Responses.single("request error: uri is empty.");
        }

        try {
            // services mapping
            String path = UrlUtils.getPath(uri);
            Matcher matcher = logUrlPattern.matcher(path);
            if (matcher.find()) {
                String maxLine = UrlUtils.getQueryParameterFirst(uri, "fetchMaxLine");
                String maxSize = UrlUtils.getQueryParameterFirst(uri, "fetchMaxByteSize");

                return Responses.single(executorBiz.getLog(Long.parseLong(matcher.group(1)),
                        UrlUtils.getQueryParameterFirst(uri, "logType"),
                        (maxLine == null ? LogUtils.MAX_LOG_LINE_COUNT : Long.parseLong(maxLine)),
                        (maxSize == null ? LogUtils.MAX_LOG_BYTE_COUNT : Long.parseLong(maxSize))));
            }
            matcher = stopTaskPattern.matcher(path);
            if (matcher.find()) {
                JobIdentity ji = JobIdentity.of(Long.parseLong(matcher.group(1)));
                return Responses.ok(ThreadPoolTaskExecutor.getInstance().cancel(ji));
            }

            matcher = modifyParametersPattern.matcher(path);
            if (matcher.find()) {
                JobIdentity ji = JobIdentity.of(Long.parseLong(matcher.group(1)));
                Task<?> task = ThreadPoolTaskExecutor.getInstance().getTask(ji);
                task.modify(JobUtils.fromJsonToMap(requestData));
            }

            return Responses.single("invalid request, uri-mapping(" + uri + ") not found.");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Responses.single("request error:" + ExceptionUtils.getRootCauseReason(e));
        }
    }

}
