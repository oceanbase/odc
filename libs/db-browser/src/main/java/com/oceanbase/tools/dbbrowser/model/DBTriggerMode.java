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
 * ODC触发器触发模式，简单模式下只有before以及after两种
 *
 * @author yh263208
 * @date 2020-12-03 15:32
 * @since ODC_release_2.4.0
 */
public enum DBTriggerMode {
    /**
     * before的触发模式
     */
    BEFORE,
    /**
     * after的触发模式
     */
    AFTER

}
