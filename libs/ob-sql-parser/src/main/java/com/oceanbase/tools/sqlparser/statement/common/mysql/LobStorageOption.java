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
package com.oceanbase.tools.sqlparser.statement.common.mysql;

import java.util.List;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

/**
 * {@link LobStorageOption}
 *
 * @author yh263208
 * @date 2024-10-25 15:29
 * @since ODC_release_4.3.2
 */
@Getter
@EqualsAndHashCode(callSuper = false)
public class LobStorageOption extends BaseStatement {

    private final String columnName;
    private final List<String> lobChunkSizes;

    public LobStorageOption(@NonNull ParserRuleContext context,
            @NonNull String columnName, @NonNull List<String> lobChunkSizes) {
        super(context);
        this.columnName = columnName;
        this.lobChunkSizes = lobChunkSizes;
    }

    public LobStorageOption(@NonNull String columnName, @NonNull List<String> lobChunkSizes) {
        this.columnName = columnName;
        this.lobChunkSizes = lobChunkSizes;
    }

    @Override
    public String toString() {
        String lobChunkSize = this.lobChunkSizes.stream().map(s -> "CHUNK " + s).collect(Collectors.joining(" "));
        return "JSON(" + this.columnName + ")" + " STORE AS (" + lobChunkSize + ")";
    }

}
