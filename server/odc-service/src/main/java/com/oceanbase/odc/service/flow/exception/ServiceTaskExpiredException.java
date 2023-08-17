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
package com.oceanbase.odc.service.flow.exception;

import com.oceanbase.odc.core.flow.exception.BaseFlowException;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;

/**
 * Task execution timeout exception
 *
 * @author yh263208
 * @date 2022-03-01 13:44
 * @since ODC_release_3.3.0
 * @see BaseFlowException
 */
public class ServiceTaskExpiredException extends BaseFlowException {

    public ServiceTaskExpiredException(String message) {
        super(ErrorCodes.FlowTaskInstanceExpired, new Object[] {}, message);
    }

    public ServiceTaskExpiredException() {
        super(ErrorCodes.FlowTaskInstanceExpired, new Object[] {}, null, null);
    }

}
