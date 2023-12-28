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

import org.springframework.beans.BeanUtils;

import com.oceanbase.tools.dbbrowser.model.DBTableColumn;

import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * @author jingtian
 */
@NoArgsConstructor
public class OdcDBTableColumn extends DBTableColumn {

    public OdcDBTableColumn(@NonNull DBTableColumn column) {
        BeanUtils.copyProperties(column, this);
    }

    public void setColumnName(String columnName) {
        super.setName(columnName);
    }

    public String getColumnName() {
        return this.name();
    }

    public void setDataType(String dataType) {
        super.setTypeName(dataType);
    }

    public String getDataType() {
        return this.getTypeName();
    }

    public String getNativeDataType() {
        return this.getTypeName();
    }

    public Long getLength() {
        return this.getMaxLength();
    }

    public Long getWidth() {
        return this.getMaxLength();
    }

    public void setAllowNull(Boolean allowNull) {
        super.setNullable(allowNull);
    }

    public Boolean getAllowNull() {
        return this.getNullable();
    }

    public void setAutoIncreament(Boolean autoIncreament) {
        super.setAutoIncrement(autoIncreament);
    }

    public Boolean getAutoIncreament() {
        return this.getAutoIncrement();
    }

    public void setDefaultValue(Object defaultValue) {
        if (defaultValue != null) {
            super.setDefaultValue(defaultValue.toString());
        }
    }

    public Boolean getPrimaryKey() {
        return KeyType.PRI.equals(this.getKeyType());
    }

    public void setCharacter(String character) {
        super.setCharsetName(character);
    }

    public String getCharacter() {
        return this.getCharsetName();
    }

    public void setCollation(String collation) {
        super.setCollationName(collation);
    }

    public String getCollation() {
        return this.getCollationName();
    }

    @Override
    public Long getPrecision() {
        String[] numberType = {"decimal", "float", "double", "bit", "int", "tinyint", "smallint", "mediumint", "bigint",
                "FLOAT", "NUMBER"};
        for (String type : numberType) {
            if (type.equals(this.getTypeName())) {
                return super.getPrecision();
            }
        }
        return null;
    }

}
