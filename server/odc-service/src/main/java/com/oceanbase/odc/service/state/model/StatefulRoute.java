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
package com.oceanbase.odc.service.state.model;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface StatefulRoute {

    StateName stateName() default StateName.NONE;

    boolean multiState() default false;

    /**
     * Spring Expression Language (SpEL) expression for computing the StateId,which is used at
     * {@link StateManager#getRouteInfo(Object)}
     ** 
     * @return
     */
    String stateIdExpression();

    /**
     * If stateName is not specified, you have to specify a manager，or else overrides the manager
     * specified by stateName.
     *
     * @return state manager bean name
     */
    String stateManager() default "";

}
