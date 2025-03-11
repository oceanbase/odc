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

package com.oceanbase.tools.dbbrowser.template.oracle;

import com.oceanbase.tools.dbbrowser.model.DBMView;
import com.oceanbase.tools.dbbrowser.template.BaseMViewTemplate;
import com.oceanbase.tools.dbbrowser.template.BaseViewTemplate;
import com.oceanbase.tools.dbbrowser.template.DBObjectTemplate;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2025/3/11 20:50
 * @since: 4.3.4
 */
public class OracleMViewTemplate implements DBObjectTemplate<DBMView> {

    @Override
    public String generateCreateObjectTemplate(DBMView dbObject) {
        return null;
    }
}
