/*
 * Copyright (c) 2025 OceanBase.
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
create table if not exists database_access_history(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    create_time      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
    update_time      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Last Update time',
    last_access_time DATETIME NOT NULL COMMENT 'Last access time',
    user_id          BIGINT NOT NULL COMMENT 'User id，FK refer to iam_user.id',
    database_id      BIGINT NOT NULL COMMENT 'Database id，FK refer to connect_database.id',
    connection_id    BIGINT NOT NULL COMMENT 'Datasource id，FK refer to connect_connection.id',
    INDEX `index_dbid` (`user_id`),
    UNIQUE KEY uq_user_dbid_cid (`user_id`, `database_id`)
  ) COMMENT = 'user access history of database table';