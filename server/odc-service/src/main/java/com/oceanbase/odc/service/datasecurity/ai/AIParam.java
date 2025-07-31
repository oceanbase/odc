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
package com.oceanbase.odc.service.datasecurity.ai;

public class AIParam {

    /**
     * Default values for AI configuration
     */
    public static final Boolean DEFAULT_ENABLE_THINKING = false;
    public static final Double DEFAULT_TEMPERATURE = 0.1;
    public static final Double DEFAULT_TOP_P = 1.0;
    public static final Integer DEFAULT_TOP_K = 0;
    public static final Integer DEFAULT_MIN_P = 0;

    public static final Integer DEFAULT_BATCH_SIZE_IN_TABLE = 30;
}