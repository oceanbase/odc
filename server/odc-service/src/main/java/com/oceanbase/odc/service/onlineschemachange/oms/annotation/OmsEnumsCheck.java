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
package com.oceanbase.odc.service.onlineschemachange.oms.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import javax.validation.ReportAsSingleViolation;

import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-06-01
 * @since 4.2.0
 */
@Target({FIELD, PARAMETER})
@Retention(RUNTIME)
@Constraint(validatedBy = OmsEnumsCheck.OmsEnumsValidator.class)
@Documented
@ReportAsSingleViolation
public @interface OmsEnumsCheck {

    String message() default "{fieldName} Only Support {enumValues}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    String fieldName();

    Class<?> enumClass();

    @Slf4j
    class OmsEnumsValidator implements ConstraintValidator<OmsEnumsCheck, Object> {

        private String fieldName;

        private Class enumClass;

        @Override
        public void initialize(OmsEnumsCheck constraintAnnotation) {
            this.fieldName = constraintAnnotation.fieldName();
            this.enumClass = constraintAnnotation.enumClass();
        }

        @Override
        public boolean isValid(Object value, ConstraintValidatorContext constraintContext) {

            // 添加参数 校验失败的时候可用
            HibernateConstraintValidatorContext hibernateContext =
                    constraintContext.unwrap(HibernateConstraintValidatorContext.class);

            if (value == null || value.toString().isEmpty()) {
                return true;
            }

            try {
                if (enumClass.isEnum()) {
                    if (value instanceof String) {
                        // 枚举类验证
                        Object[] objs = enumClass.getEnumConstants();
                        Method method = enumClass.getMethod("name");
                        for (Object obj : objs) {
                            Object name = method.invoke(obj, null);
                            if (value.equals(name.toString())) {
                                return true;
                            }
                        }
                    } else if (value instanceof List || value instanceof String[]) {
                        Object[] objs = enumClass.getEnumConstants();
                        Method method = enumClass.getMethod("name");
                        Set<String> enumNameList = new HashSet<>(objs.length);
                        for (Object obj : objs) {
                            enumNameList.add((String) method.invoke(obj, null));
                        }
                        List enumNames = value instanceof List ? (List) value : Arrays.asList(((String[]) value));
                        if (enumNameList.containsAll(enumNames)) {
                            return true;
                        }
                    }
                }

            } catch (Exception e) {
                log.error("OmsEnumsValidator Execute Failed: {}", e.getMessage());
            }

            hibernateContext.addMessageParameter("fieldName", fieldName);
            List<Object> enumConstants =
                    enumClass.getEnumConstants() == null ? new ArrayList()
                            : Arrays.asList(enumClass.getEnumConstants());
            hibernateContext.addMessageParameter("enumValues",
                    enumConstants.stream().map(e -> e.toString()).collect(Collectors.joining(",")));
            return false;
        }

    }

}
