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
package com.oceanbase.odc.service.task.exception;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * @author yaobin
 * @date 2023-11-15
 * @since 4.2.4
 */
public class JobException extends Exception {

    /**
     * @param messagePattern message pattern, eg "id={0},name={1}"
     * @param arguments message pattern arguments
     */
    public JobException(String messagePattern, Object... arguments) {
        super(MessageFormat.format(messagePattern,
                Arrays.asList(arguments).stream().map(String::valueOf).collect(
                        Collectors.toList())));
    }

    public JobException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param messagePattern message pattern, eg "id={0},name={1}"
     * @param cause origin exception
     * @param arguments message pattern arguments
     */
    public JobException(String messagePattern, Throwable cause, Object... arguments) {
        super(MessageFormat.format(messagePattern,
                Arrays.asList(arguments).stream().map(String::valueOf).collect(
                        Collectors.toList())),
                cause);
    }

}
