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

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.executable.ExecutableValidator;

import org.hibernate.validator.HibernateValidator;

/**
 * Validator Builder
 *
 * @author yh263208
 * @date 2021-05-28 17:40
 * @since ODC_release_2.4.2
 */
public class ValidatorBuilder {
    /**
     * Get Validator instance
     *
     * @return Validator object
     */
    public static Validator buildFastFailValidator() {
        return Validation
                .byProvider(HibernateValidator.class)
                .configure()
                .failFast(true)
                .buildValidatorFactory()
                .getValidator();
    }

    /**
     * Get Validator instance
     *
     * @return Validator object
     */
    public static Validator buildValidator() {
        return Validation
                .byDefaultProvider()
                .configure()
                .buildValidatorFactory()
                .getValidator();
    }

    /**
     * Get fast fail executable validator instance
     *
     * @return Validator object
     */
    public static ExecutableValidator buildFastFailExecutableValidator() {
        return buildFastFailValidator().forExecutables();
    }

    /**
     * Get fast fail executable validator instance
     *
     * @return Validator object
     */
    public static ExecutableValidator buildExecutableValidator() {
        return buildValidator().forExecutables();
    }
}
