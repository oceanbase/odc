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
package com.oceanbase.odc.core.authority.permission;

/**
 * The highest authority, has all operation authority, be extra careful when granting this authority
 *
 * @author yh263208
 * @date 2021-07-20 17:23
 * @since ODC_release_3.2.0
 * @see Permission
 */
public class AllPermission implements Permission {
    /**
     * Checks if the specified permission is "implied" by this object. This method always returns true.
     *
     * @param permission the permission to check against.
     * @return return
     */
    @Override
    public boolean implies(Permission permission) {
        return true;
    }

    /**
     * Checks two AllPermission objects for equality. Two AllPermission objects are always equal.
     *
     * @param obj the object we are testing for equality with this object.
     * @return true if <i>obj</i> is an AllPermission, false otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        return (obj instanceof AllPermission);
    }

    /**
     * Returns the hash code value for this object.
     *
     * @return a hash code value for this object.
     */
    @Override
    public int hashCode() {
        return 1;
    }

    @Override
    public String toString() {
        return "<all permission>";
    }

}
