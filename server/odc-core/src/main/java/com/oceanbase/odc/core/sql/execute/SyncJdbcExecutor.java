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
package com.oceanbase.odc.core.sql.execute;

import org.springframework.jdbc.core.JdbcOperations;

/**
 * {@code JdbcExecutor}, used for actual sql execution. {@code JdbcExecutor} can be new according to
 * different business scenarios
 *
 * @author yh263208
 * @date 2021-11-01 20:47
 * @since ODC_release_3.2.2
 */
public interface SyncJdbcExecutor extends JdbcOperations {
}
