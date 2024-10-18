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
package com.oceanbase.odc.service.task.processor;

/**
 * @author longpeng.zlp
 * @date 2024/10/10 11:29
 */
public interface ProcessorMatcher {
    /**
     * if type is interested for current matcher remaining processor method will be called if interested
     * 
     * @param type
     * @return
     */
    boolean interested(String type);
}
