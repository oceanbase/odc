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
package com.oceanbase.tools.dbbrowser.model;

/**
 * ODC触发器触发的DML事件类型枚举
 *
 * @author yh263208
 * @date 2020-12-03 15：42
 * @since ODC_release_2.4.0
 */
public enum DBTriggerDMLEvent {
    /**
     * 触发事件为INSERT
     */
    INSERT,
    /**
     * 触发事件为delete
     */
    DELETE,
    /**
     * 触发事件为update，后面可以跟具体的列名
     */
    UPDATE

}
