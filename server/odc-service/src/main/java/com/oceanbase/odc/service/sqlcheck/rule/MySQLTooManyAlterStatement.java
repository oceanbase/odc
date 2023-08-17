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
package com.oceanbase.odc.service.sqlcheck.rule;

import java.util.Arrays;
import java.util.List;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.sqlcheck.SqlCheckUtil;

import lombok.NonNull;

/**
 * {@link MySQLTooManyAlterStatement}
 *
 * @author yh263208
 * @date 2023-06-27 19:45
 * @since ODC_release_4.2.0
 */
public class MySQLTooManyAlterStatement extends BaseTooManyAlterStatement {

    public MySQLTooManyAlterStatement(@NonNull Integer maxAlterCount) {
        super(maxAlterCount);
    }

    @Override
    protected String unquoteIdentifier(String identifier) {
        return SqlCheckUtil.unquoteMySQLIdentifier(identifier);
    }

    @Override
    public List<DialectType> getSupportsDialectTypes() {
        return Arrays.asList(DialectType.MYSQL, DialectType.OB_MYSQL, DialectType.ODP_SHARDING_OB_MYSQL);
    }

}
