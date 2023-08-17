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

import org.apache.commons.lang3.StringUtils;

import com.oceanbase.odc.core.shared.exception.NotImplementedException;

import lombok.NonNull;

/**
 * @Author: Lebie
 * @Date: 2023/8/6 16:32
 * @Description: []
 */
public class OperatorFactory {
    public static Operator createOperator(@NonNull String operatorType) {
        if (StringUtils.equalsIgnoreCase(operatorType, "equals")) {
            return new EqualsOperator();
        } else if (StringUtils.equalsIgnoreCase(operatorType, "not_equals")) {
            return new NotEqualsOperator();
        } else if (StringUtils.equalsIgnoreCase(operatorType, "contains")) {
            return new ContainsOperator();
        } else if (StringUtils.equalsIgnoreCase(operatorType, "not_contains")) {
            return new NotContainsOperator();
        } else if (StringUtils.equalsIgnoreCase(operatorType, "in")) {
            return new InOperator();
        } else if (StringUtils.equalsIgnoreCase(operatorType, "not_in")) {
            return new NotInOperator();
        } else {
            throw new NotImplementedException();
        }
    }
}
