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
package com.oceanbase.odc.core.migrate.resource.checker;

import java.util.Objects;

import lombok.NonNull;

/**
 * {@link StringCheck}
 *
 * @author yh263208
 * @date 2022-06-23 21:00
 * @since ODC_release_3.3.2
 * @see ExpressionChecker
 */
public class StringCheck implements ExpressionChecker {

    @Override
    public boolean supports(@NonNull String expression) {
        return true;
    }

    @Override
    public boolean contains(@NonNull String expression, @NonNull Object value) {
        return Objects.equals(value.toString(), expression);
    }
}
