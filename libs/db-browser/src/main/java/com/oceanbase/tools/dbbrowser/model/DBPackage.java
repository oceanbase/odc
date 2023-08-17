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

import com.oceanbase.tools.dbbrowser.util.StringUtils;

import lombok.Data;

@Data
public class DBPackage implements DBObject {

    private String packageName;
    private String packageType;
    private DBPackageDetail packageHead;
    private DBPackageDetail packageBody;
    private String status = "VALID";
    private String errorMessage;

    public static DBPackage ofPackage(String name) {
        DBPackage dbPackage = new DBPackage();
        dbPackage.packageName = name;
        dbPackage.packageType = DBObjectType.PACKAGE.getName();
        return dbPackage;
    }

    public static DBPackage ofPackageBody(String name) {
        DBPackage dbPackage = ofPackage(name);
        dbPackage.packageType = DBObjectType.PACKAGE_BODY.getName();
        return dbPackage;
    }

    @Override
    public String name() {
        return this.packageName;
    }

    @Override
    public DBObjectType type() {
        if (StringUtils.equalsAnyIgnoreCase(this.packageType, "packageBody")) {
            return DBObjectType.PACKAGE_BODY;
        }
        return DBObjectType.getEnumByName(this.packageType);
    }

}
