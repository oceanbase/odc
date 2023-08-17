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
package com.oceanbase.tools.dbbrowser.editor;

import java.util.Collection;

import javax.validation.constraints.NotNull;

import com.oceanbase.tools.dbbrowser.model.DBObject;

public interface DBObjectEditor<T extends DBObject> {
    /**
     * true if T is editable; false if not
     *
     */
    boolean editable();

    /**
     * DDL in SQL for creating a specific type of DBObject as an individual<br>
     * For example, if dbObject is type of DBTableIndex, then some MySQL implementation may return
     * something like<br>
     * ALTER TABLE `table_name` ADD INDEX `index_name` (col1, col2);<br>
     *
     * @param dbObject {@link DBObject}
     * @return create ddl as an individual {@link String}
     *
     */
    String generateCreateObjectDDL(@NotNull T dbObject);

    /**
     * DDL in SQL for creating a specific type of DBObject as a child of parent object<br>
     * For example, if dbObject is type of DBTableIndex, then some MySQL implementation may return
     * something like<br>
     * UNIQUE INDEX `index_name`(col1 ASC);<br>
     *
     * @param dbObject {@link DBObject}
     * @return create ddl as a child of parent object {@link String}
     */
    String generateCreateDefinitionDDL(@NotNull T dbObject);

    /**
     * DDL in SQL for updating a specific type of DBObject from oldObject to newObject<br>
     * For example, if dbObject is type of DBTableIndex and we are going to change the name of the
     * index, then some MySQL implementation<br>
     * could return something like<br>
     * ALTER TABLE `table_name` DROP INDEX `old_index_name`, ADD INDEX `new_index_name` (col1, col2);
     *
     * @param oldObject {@link DBObject}
     * @param newObject {@link DBObject}
     * @return update ddl from oldObject to newObject {@link String}
     */
    String generateUpdateObjectDDL(@NotNull T oldObject, @NotNull T newObject);

    String generateUpdateObjectListDDL(Collection<T> oldObjects, Collection<T> newObjects);

    /**
     * DDL in SQL for renaming a specific type of DBObject<br>
     * For example, if dbObject is type of DBTableIndex, then some MySQL implementation may return
     * something like<br>
     * ALTER TABLE `table_name` RENAME INDEX `old_index_name` to `new_index_name`
     *
     * @param oldObject {@link DBObject}
     * @param newObject {@link DBObject}
     * @return create ddl as a child of parent object {@link String}
     */
    String generateRenameObjectDDL(@NotNull T oldObject, @NotNull T newObject);

    /**
     * DDL in SQL for dropping a specific type of DBObject<br>
     * For example, if dbObject is type of DBTableIndex, then MySQL implementation may return something
     * like<br>
     * DROP INDEX `index_name`;
     *
     * @param dbObject {@link DBObject}
     * @return drop ddl {@link String}
     */
    String generateDropObjectDDL(@NotNull T dbObject);

}
