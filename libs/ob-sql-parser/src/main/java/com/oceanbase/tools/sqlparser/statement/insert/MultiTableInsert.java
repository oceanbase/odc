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
package com.oceanbase.tools.sqlparser.statement.insert;

import java.util.List;

import org.antlr.v4.runtime.ParserRuleContext;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

/**
 * {@link MultiTableInsert}
 *
 * @author yh263208
 * @date 2022-12-20 19:18
 * @since ODC_release_4.1.0
 * @see Insert
 */
@Getter
@EqualsAndHashCode(callSuper = false)
public class MultiTableInsert extends BaseStatement implements Insert {

    private final List<InsertBody> insertBodies;

    public MultiTableInsert(@NonNull ParserRuleContext context, @NonNull List<InsertBody> insertBodies) {
        super(context);
        this.insertBodies = insertBodies;
    }

    public MultiTableInsert(@NonNull List<InsertBody> insertBodies) {
        this.insertBodies = insertBodies;
    }

    @Override
    public int getType() {
        return Insert.MULTI_INSERT;
    }

    @Override
    public List<InsertBody> getInsertBodies() {
        return this.insertBodies;
    }

    @Override
    public boolean replace() {
        return false;
    }

    @Override
    public String toString() {
        return this.getText();
    }

}
