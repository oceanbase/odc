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

package com.oceanbase.odc.service.task.constants;

import com.oceanbase.odc.service.task.model.OdcTaskLogLevel;

/**
 * @author yaobin
 * @date 2023-12-25
 * @since 4.2.4
 */
public class JobAttributeKeyConstants {

    public static final String LOG_ALL_OBJECT_ID = OdcTaskLogLevel.ALL.name();

    public static final String LOG_WARN_OBJECT_ID = OdcTaskLogLevel.WARN.name();

    public static final String OSS_BUCKET_NAME = "OSS_BUCKET_NAME";
}
