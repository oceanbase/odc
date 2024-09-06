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
package com.oceanbase.odc.service.resource;

import org.apache.hadoop.classification.InterfaceStability.Evolving;

/**
 * resource config
 * 
 * @author longpeng.zlp
 * @date 2024/8/12 14:42
 */
@Evolving
public interface ResourceContext {
    /**
     * type of this resource context, to match {@link ResourceOperator}
     * 
     * @return
     */
    String type();

    /**
     * name of the resource, default is task name of this job
     */
    String resourceName();
}
