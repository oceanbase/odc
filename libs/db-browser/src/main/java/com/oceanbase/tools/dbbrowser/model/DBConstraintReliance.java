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
 * RELY and NORELY are valid only when you are modifying an existing constraint (that is, in the
 * ALTER TABLE ... MODIFY constraint syntax). These parameters specify whether a constraint in
 * NOVALIDATE mode is to be taken into account for query rewrite. Specify RELY to activate an
 * existing constraint in NOVALIDATE mode for query rewrite in an unenforced query rewrite integrity
 * mode. The constraint is in NOVALIDATE mode, so Oracle does not enforce it. The default is NORELY.
 * <br>
 *
 * for metadata in oracle ALL_CONSTRAINTS：<br>
 * - When VALIDATED = NOT VALIDATED, this column indicates whether the constraint is to be taken
 * into account for query rewrite (RELY) or not (NULL). <br>
 * - When VALIDATED = VALIDATED, this column is not meaningful. <br>
 *
 * Restriction on the RELY Clause <br>
 * You cannot set a nondeferrable NOT NULL constraint to RELY.
 */
public enum DBConstraintReliance {
    RELY,
    NORELY,
    UNKNOWN
}
