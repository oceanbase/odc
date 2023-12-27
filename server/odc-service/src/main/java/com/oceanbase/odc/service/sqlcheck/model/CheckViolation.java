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
package com.oceanbase.odc.service.sqlcheck.model;

import org.apache.commons.lang3.Validate;
import org.springframework.context.i18n.LocaleContextHolder;

import com.oceanbase.odc.common.i18n.Translatable;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

/**
 * {@link CheckViolation}
 *
 * @author yh263208
 * @date 2022-11-16 17:13
 * @since ODC_release_4.1.0
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false, exclude = {"offset"})
public class CheckViolation implements Translatable {

    private Integer offset;
    private String text;
    private int row;
    private int col;
    private int start;
    private int stop;
    private Integer level;
    private SqlCheckRuleType type;
    private Object[] args;

    public CheckViolation(@NonNull String text, int row, int col,
            int start, int stop, @NonNull SqlCheckRuleType type, @NonNull Object[] args) {
        Validate.isTrue(row >= 0, "Row can not be negative");
        Validate.isTrue(col >= 0, "Col can not be negative");
        Validate.isTrue(start >= 0, "Start can not be negative");
        Validate.isTrue(stop >= 0, "Stop can not be negative");
        this.text = text;
        this.row = row;
        this.col = col;
        this.start = start;
        this.stop = stop;
        this.type = type;
        this.args = args;
    }

    public CheckViolation(@NonNull String text, int row, int col,
            int start, int stop, @NonNull SqlCheckRuleType type, Integer offset, @NonNull Object[] args) {
        Validate.isTrue(row >= 0, "Row can not be negative");
        Validate.isTrue(col >= 0, "Col can not be negative");
        Validate.isTrue(start >= 0, "Start can not be negative");
        Validate.isTrue(stop >= 0, "Stop can not be negative");
        this.text = text;
        this.row = row;
        this.col = col;
        this.start = start;
        this.stop = stop;
        this.type = type;
        this.offset = offset;
        this.args = args;
    }

    @Override
    public String code() {
        return "LocalizedMessage";
    }

    public String getLocalizedMessage() {
        return this.type.getLocalizedMessage(args);
    }

    public String getMessage() {
        String message = this.type.getLocalizedMessage(args);
        Object[] args = new Object[] {row, col, message};
        return translate(args, LocaleContextHolder.getLocale());
    }

}
