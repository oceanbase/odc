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
package com.oceanbase.odc.service.plugin;

import java.util.List;

import org.pf4j.ExtensionPoint;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.plugin.schema.api.DatabaseExtensionPoint;
import com.oceanbase.odc.plugin.schema.api.FunctionExtensionPoint;
import com.oceanbase.odc.plugin.schema.api.PackageExtensionPoint;
import com.oceanbase.odc.plugin.schema.api.ProcedureExtensionPoint;
import com.oceanbase.odc.plugin.schema.api.SequenceExtensionPoint;
import com.oceanbase.odc.plugin.schema.api.SynonymExtensionPoint;
import com.oceanbase.odc.plugin.schema.api.TableExtensionPoint;
import com.oceanbase.odc.plugin.schema.api.TriggerExtensionPoint;
import com.oceanbase.odc.plugin.schema.api.TypeExtensionPoint;
import com.oceanbase.odc.plugin.schema.api.ViewExtensionPoint;

/**
 * @author jingtian
 * @date 2023/7/21
 * @since 4.2.0
 */
public class SchemaPluginUtil {
    public static OdcPluginManager<DialectType> getOdcPluginManager() {
        PluginService pluginService = new PluginService();
        return pluginService.getSchemaPluginManager();
    }

    public static DatabaseExtensionPoint getDatabaseExtension(DialectType dialectType) {
        return getSingletonExtension(dialectType, DatabaseExtensionPoint.class);
    }

    public static TableExtensionPoint getTableExtension(DialectType dialectType) {
        return getSingletonExtension(dialectType, TableExtensionPoint.class);
    }

    public static ViewExtensionPoint getViewExtension(DialectType dialectType) {
        return getSingletonExtension(dialectType, ViewExtensionPoint.class);
    }

    public static FunctionExtensionPoint getFunctionExtension(DialectType dialectType) {
        return getSingletonExtension(dialectType, FunctionExtensionPoint.class);
    }

    public static PackageExtensionPoint getPackageExtension(DialectType dialectType) {
        return getSingletonExtension(dialectType, PackageExtensionPoint.class);
    }

    public static ProcedureExtensionPoint getProcedureExtension(DialectType dialectType) {
        return getSingletonExtension(dialectType, ProcedureExtensionPoint.class);
    }

    public static SequenceExtensionPoint getSequenceExtension(DialectType dialectType) {
        return getSingletonExtension(dialectType, SequenceExtensionPoint.class);
    }

    public static SynonymExtensionPoint getSynonymExtension(DialectType dialectType) {
        return getSingletonExtension(dialectType, SynonymExtensionPoint.class);
    }

    public static TriggerExtensionPoint getTriggerExtension(DialectType dialectType) {
        return getSingletonExtension(dialectType, TriggerExtensionPoint.class);
    }

    public static TypeExtensionPoint getTypeExtension(DialectType dialectType) {
        return getSingletonExtension(dialectType, TypeExtensionPoint.class);
    }

    public static <T extends ExtensionPoint> T getSingletonExtension(DialectType dialectType, Class<T> type) {
        return getOdcPluginManager().getSingletonExtension(dialectType, type);
    }

    public static <T extends ExtensionPoint> List<T> getExtensions(DialectType dialectType, Class<T> type) {
        return getOdcPluginManager().getExtensions(dialectType, type);
    }

    public static <T extends ExtensionPoint> List<Class<? extends T>> getExtensionClasses(
            DialectType dialectType, Class<T> type) {
        return getOdcPluginManager().getExtensionClasses(dialectType, type);
    }
}
