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
package com.oceanbase.odc.plugin.task.oboracle.partitionplan.util;

import com.oceanbase.odc.common.util.VersionUtils;
import com.oceanbase.tools.dbbrowser.editor.DBTablePartitionEditor;
import com.oceanbase.tools.dbbrowser.editor.oracle.OBOracleLessThan400DBTablePartitionEditor;
import com.oceanbase.tools.dbbrowser.editor.oracle.OracleDBTablePartitionEditor;

import lombok.NonNull;

/**
 * {@link DBTablePartitionEditors}
 *
 * @author yh263208
 * @date 2024-01-29 16:43
 * @since ODC_release_4.2.4
 */
public class DBTablePartitionEditors {

    public static DBTablePartitionEditor generate(@NonNull String dbVersion) {
        if (VersionUtils.isLessThan(dbVersion, "4.0.0")) {
            return new OBOracleLessThan400DBTablePartitionEditor();
        }
        return new OracleDBTablePartitionEditor();
    }

}
