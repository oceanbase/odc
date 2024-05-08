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

        String namespace = path.substring(0, nameIndex).replace("/", "");
        return DefaultExecutorIdentifier.builder().host(uriComponents.getHost())
                .port(uriComponents.getPort())
                .protocol(uriComponents.getScheme())
                .namespace(namespace.length() == 0 ? null : namespace)
                .executorName(path.substring(nameIndex).replace("/", ""))
                .build();
    }
}
