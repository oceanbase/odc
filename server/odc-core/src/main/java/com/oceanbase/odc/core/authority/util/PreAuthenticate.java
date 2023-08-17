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
package com.oceanbase.odc.core.authority.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.oceanbase.odc.core.authority.model.SecurityResource;

/**
 * Permission system annotation, used to mark an object to be included in permission access control
 * This is an entrance to the entire permission framework. A simple usage example is shown below
 *
 * <code>
 *   &#64;Authenticate(actions = {"read", "write"}, resourceType="ODC_RESOURCE_GROUP", indexOdIdParam=0)
 *   public returnValue methodName(Object resourceId, Object param2 ...);
 * </code>
 *
 * {@code actions} represents the corresponding operation of the method call, which needs to
 * correspond to the action field in the {@code iam_permission} table in the database
 *
 * {@code resourceType} represent the resource type corresponding to the method call, which needs to
 * correspond to the resource type in the {@code resource_identifier} field in the
 * {@code iam_permission} table in the database
 *
 * {@code indexOfIdParam} If the ID of the resource exists in the parameter list of the method, the
 * {@code indexOfIdParam} attribute is required to express the specific position of the ID in the
 * parameter list, and the index starts from 0
 *
 * Some operations are for the whole resource, so it is impossible to specify the specific resource
 * ID. For example, creating a resource cannot target a specific resource. In this case, you need to
 * set {@code isForALL} to true.
 *
 * @author yh263208
 * @date 2021-07-06 16:38
 * @since ODC_release_3.2.0
 */
@Target(value = {ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(PreAuthenticates.class)
public @interface PreAuthenticate {
    /**
     * Just for the annotation on method, Used to indicate the action corresponding to the method call
     */
    String[] actions() default {};

    /**
     * Just for the annotation on method. This field will not take effect when the annotation is defined
     * on the {@link SecurityResource} method, because the resourceType at this time is obtained by the
     * method of the {@link SecurityResource} interface: {@link SecurityResource#resourceType()}
     */
    String resourceType() default "";

    /**
     * Used to indicate the index position of the method parameter list where the resourceId is located.
     * This attribute will not take effect when the annotation is defined on the
     * {@link SecurityResource} method, because the resourceId at this time is obtained by the method of
     * the {@link SecurityResource} interface: {@link SecurityResource#resourceId()}
     */
    int indexOfIdParam() default -1;

    /**
     * The flag value is used to indicate whether the authentication process is for all
     * {@link SecurityResource}. If the flag value is true, {@code indexOfIdParam} does not take effect
     */
    boolean isForAll() default false;

    /**
     * Used to indicate which resource roles can invoke thie method. For example, if hasResourceRole =
     * "OWNER", then this method should only be invoked by OWNER; and the index of the resource is also
     * indicated by indexOfIdParam. If you want to have multiple resource roles, use hasAnyResourceRole
     * instead.
     *
     */
    String hasResourceRole() default "";

    /**
     * Used to indicate the resource roles who could invoke this method for example, if
     * hasAnyRole={"ROLE", "OWNER"}, then this method should only be invoked by ROLE OR OWNER; and the
     * index of the resource is also indicated by indexOfIdParam
     */
    String[] hasAnyResourceRole() default {};

}
