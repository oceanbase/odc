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
package com.oceanbase.odc.service.state;

import javax.validation.constraints.NotNull;

import org.aspectj.lang.ProceedingJoinPoint;

public interface StateManager {

    /**
     * @param stateId
     * @return return null means use current node
     */
    @NotNull
    RouteInfo getRouteInfo(Object stateId);

    /**
     * called before the node changed
     * 
     * @param targetRoute
     * @return true means do dispatch
     */
    default void preHandleBeforeNodeChange(ProceedingJoinPoint proceedingJoinPoint, RouteInfo targetRoute) {}

    /**
     * called after the node changed
     *
     * @param targetRoute
     * @return true means do dispatch
     */
    default void afterHandleBeforeNodeChange(ProceedingJoinPoint proceedingJoinPoint, RouteInfo targetRoute) {}

}
