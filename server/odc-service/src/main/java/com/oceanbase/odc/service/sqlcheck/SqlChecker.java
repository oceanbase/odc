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
package com.oceanbase.odc.service.sqlcheck;

import java.util.List;

import com.oceanbase.odc.service.sqlcheck.model.CheckViolation;

import lombok.NonNull;

/**
 * {@link SqlChecker}
 *
 * @author yh263208
 * @date 2022-11-16 17:10
 * @since ODC_release_4.1.0
 */
public interface SqlChecker {
    /**
     * check if exists potential issue
     *
     * @param sqlScript a script of sql
     * @return list of {@link CheckViolation}
     */
    List<CheckViolation> check(@NonNull String sqlScript);

}
