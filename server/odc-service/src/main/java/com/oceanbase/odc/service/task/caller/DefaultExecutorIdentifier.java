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
package com.oceanbase.odc.service.task.caller;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.service.task.util.JobUtils;

import lombok.Builder;
import lombok.Data;

/**
 * @author yaobin
 * @date 2023-12-25
 * @since 4.2.4
 */
@Builder
@Data
public class DefaultExecutorIdentifier implements ExecutorIdentifier {

    public static final String DEFAULT_PROTOCOL = "http";
    public static final String DEFAULT_HOST = "odc";
    public static final Integer DEFAULT_PORT = JobUtils.getPort();

    @Builder.Default()
    private String protocol = DEFAULT_PROTOCOL;

    @Builder.Default()
    private String host = DEFAULT_HOST;

    @Builder.Default()
    private int port = DEFAULT_PORT;

    private String namespace;

    private String executorName;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(protocol)
                .append("://")
                .append(host == null ? "" : host)
                .append(":")
                .append(port);
        if (StringUtils.isNotBlank(namespace)) {
            sb.append("/");
            sb.append(namespace);
        }
        sb.append("/")
                .append(executorName);
        return sb.toString();
    }

}
