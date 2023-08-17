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
package com.oceanbase.odc.common.validate;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import org.apache.commons.collections4.CollectionUtils;

import lombok.NonNull;

/**
 * @author yaobin
 * @date 2023-06-12
 * @since 4.2.0
 */
public class ValidatorUtils {
    private static final Validator VALIDATOR = ValidatorBuilder.buildFastFailValidator();

    public static <T> void verifyField(@NonNull T target) {
        Set<ConstraintViolation<T>> result = VALIDATOR.validate(target);
        if (CollectionUtils.isEmpty(result)) {
            return;
        }
        ConstraintViolation<T> violation = result.iterator().next();
        throw new IllegalArgumentException(violation.getPropertyPath().toString() + " " + violation.getMessage());
    }

}
