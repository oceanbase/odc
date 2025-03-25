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

CREATE INDEX idx_iam_user_org_id_creator_id ON iam_user (organization_id, creator_id);
CREATE INDEX idx_iam_role_org_id_creator_id ON iam_role (organization_id, creator_id);
CREATE INDEX idx_connect_connection_org_id_creator_id ON connect_connection (organization_id, creator_id);

create index if not exists idx_task_task_connection_id on task_task(connection_id);
create index if not exists idx_schedule_schedule_connection_id on schedule_schedule(connection_id);
create index if not exists idx_data_security_sensitive_rule_organization_id on data_security_sensitive_rule(organization_id);
