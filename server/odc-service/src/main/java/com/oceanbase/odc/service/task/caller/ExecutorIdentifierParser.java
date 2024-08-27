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

import org.springframework.web.util.UriComponents;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.service.common.util.UrlUtils;
import com.oceanbase.odc.service.task.exception.TaskRuntimeException;

/**
 * @author yaobin
 * @date 2023-12-25
 * @since 4.2.4
 */
public class ExecutorIdentifierParser {

    public static ExecutorIdentifier parser(String identifierString) {
        UriComponents uriComponents = UrlUtils.getUriComponents(identifierString);
        String path = uriComponents.getPath();
        int nameIndex = path.lastIndexOf("/");
        if (nameIndex == -1) {
            throw new TaskRuntimeException("Illegal executor name : " + path);
        }

        String tmpStr = path.substring(0, nameIndex);
        String[] regionAndNamespace = StringUtils.split(tmpStr, "/");
        String namespace = null;
        String region = null;
        String group = null;
        // new version
        if (regionAndNamespace.length == 3) {
            // url as "/region/group/namespace/name"
            region = regionAndNamespace[0];
            group = regionAndNamespace[1];
            namespace = regionAndNamespace[2];
        } else if (regionAndNamespace.length == 1) {
            // old version
            namespace = regionAndNamespace[0];
        }
        return DefaultExecutorIdentifier.builder().host(uriComponents.getHost())
                .port(uriComponents.getPort())
                .protocol(uriComponents.getScheme())
                .region(StringUtils.isEmpty(region) ? null : UrlUtils.decode(region))
                .group(StringUtils.isEmpty(group) ? null : UrlUtils.decode(group))
                .namespace(StringUtils.isEmpty(namespace) ? null : UrlUtils.decode(namespace))
                .executorName(UrlUtils.decode(path.substring(nameIndex).replace("/", "")))
                .build();
    }
}
