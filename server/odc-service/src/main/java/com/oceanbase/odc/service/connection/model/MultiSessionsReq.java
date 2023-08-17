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
package com.oceanbase.odc.service.connection.model;

import java.util.Set;

import javax.validation.constraints.NotNull;

import com.oceanbase.odc.core.session.ConnectionSession;

import lombok.Data;

/**
 * Request for closing a {@link ConnectionSession}
 *
 * @author yh263208
 * @date 2022-01-10 15:18
 * @since ODC_release_3.3.0
 */
@Data
public class MultiSessionsReq {
    @NotNull
    private Set<String> sessionIds;
}
