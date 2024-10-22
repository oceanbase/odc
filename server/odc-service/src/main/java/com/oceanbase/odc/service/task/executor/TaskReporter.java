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
package com.oceanbase.odc.service.task.executor;

import java.text.MessageFormat;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.task.util.HttpClientUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @author gaoda.xy
 * @date 2023/11/30 19:41
 */
@Slf4j
public class TaskReporter {

    private final List<String> hostUrls;


    public TaskReporter(List<String> hostUrls) {
        this.hostUrls = hostUrls;
    }


    public <T> boolean report(String url, T result) {
        if (CollectionUtils.isEmpty(hostUrls)) {
            log.warn("host url is empty");
            return false;
        }
        for (String host : hostUrls) {
            try {
                String hostWithUrl = host + url;
                SuccessResponse<String> response =
                        HttpClientUtils.request("POST", hostWithUrl, JsonUtils.toJson(result),
                                new TypeReference<SuccessResponse<String>>() {});
                if (response != null && response.getSuccessful()) {
                    log.info("Report to host {} success, result is {}, response is {}.", host, JsonUtils.toJson(result),
                            JsonUtils.toJson(response));
                    return true;
                }
            } catch (Exception e) {
                log.warn(MessageFormat.format("Report to host {0} failed, result is {1}, error is: ", host,
                        JsonUtils.toJson(result)), e);
            }
        }
        return false;
    }

}
