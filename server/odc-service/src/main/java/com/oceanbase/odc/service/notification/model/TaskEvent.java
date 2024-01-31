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
package com.oceanbase.odc.service.notification.model;

/**
 * @author liuyizhuo.lyz
 * @date 2024/1/17
 */
public enum TaskEvent {

    EXECUTION_SUCCEEDED,
    EXECUTION_FAILED,
    EXECUTION_TIMEOUT,
    PENDING_APPROVAL,
    APPROVED,
    APPROVAL_REJECTION,
    SCHEDULING_FAILED,
    SCHEDULING_TIMEOUT

    ;

}
