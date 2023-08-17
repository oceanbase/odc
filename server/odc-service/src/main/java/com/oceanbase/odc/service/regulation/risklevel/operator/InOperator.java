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
package com.oceanbase.odc.service.regulation.risklevel.operator;

import java.util.List;
import java.util.Objects;

import javax.validation.constraints.NotNull;

import com.oceanbase.odc.core.shared.exception.UnexpectedException;

/**
 * @Author: Lebie
 * @Date: 2023/8/6 16:26
 * @Description: []
 */
public class InOperator implements Operator {
    @Override
    public boolean evaluate(String expression, @NotNull Object value) {
        if (Objects.isNull(expression)) {
            return false;
        }
        if (!(value instanceof List)) {
            throw new UnexpectedException("value should be a string list in InOperator");
        }
        return ((List<?>) value).contains(expression);
    }
}
