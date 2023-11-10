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
package com.oceanbase.tools.sqlparser.statement.select;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

/**
 * {@link RelatedSelectBody}
 *
 * @author yh263208
 * @date 2022-12-13 01:44
 * @since ODC_release_4.1.0
 */
@Getter
@EqualsAndHashCode
public class RelatedSelectBody {

    private final SelectBody select;
    private final RelationType relation;

    public RelatedSelectBody(@NonNull SelectBody selectBody, @NonNull RelationType relation) {
        this.select = selectBody;
        this.relation = relation;
    }

    @Override
    public String toString() {
        return this.relation.name().replace("_", " ") + " " + this.select.toString();
    }

}
