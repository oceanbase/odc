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
package com.oceanbase.odc.service.db.model;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;

import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.model.DBView;

import lombok.NonNull;

public class DBViewResponse extends DBView {

    public DBViewResponse(@NonNull DBView dbView) {
        BeanUtils.copyProperties(dbView, this);
    }

    @Override
    public List<DBTableColumn> getColumns() {
        if (CollectionUtils.isEmpty(super.getColumns())) {
            return super.getColumns();
        }
        return super.getColumns().stream().map(OdcDBTableColumn::new).collect(Collectors.toList());
    }

}
