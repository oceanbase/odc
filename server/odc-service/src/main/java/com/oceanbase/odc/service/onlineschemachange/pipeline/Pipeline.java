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
package com.oceanbase.odc.service.onlineschemachange.pipeline;

/**
 * @author yaobin
 * @date 2023-06-10
 * @since 4.2.0
 */
public interface Pipeline {
    /**
     * basic valve which be invoked latest
     *
     * @param basic basic valve
     */
    void setBasic(Valve basic);

    /**
     * add a new valve into pipeline
     *
     * @param valve new valve
     */
    void addValve(Valve valve);

    /**
     * pipeline invoke method
     *
     * @param context valve execute context
     */
    void invoke(ValveContext context);
}
