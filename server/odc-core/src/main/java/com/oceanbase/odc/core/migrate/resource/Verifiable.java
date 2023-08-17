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
package com.oceanbase.odc.core.migrate.resource;

import java.util.Set;

import javax.validation.ConstraintViolation;

import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.odc.common.validate.ValidatorBuilder;
import com.oceanbase.odc.core.shared.exception.VerifyException;

import lombok.NonNull;

/**
 * {@link Verifiable}
 *
 * @author yh263208
 * @date 2022-04-20 15:06
 * @since ODC_release_3.3.1
 */
public interface Verifiable {

    default void verify() throws VerifyException {}

    static <T> void verifyField(@NonNull T target) throws VerifyException {
        Set<ConstraintViolation<T>> result = ValidatorBuilder.buildFastFailValidator().validate(target);
        if (CollectionUtils.isEmpty(result)) {
            return;
        }
        ConstraintViolation<T> violation = result.iterator().next();
        throw new VerifyException(violation.getPropertyPath().toString() + " " + violation.getMessage());
    }

}
