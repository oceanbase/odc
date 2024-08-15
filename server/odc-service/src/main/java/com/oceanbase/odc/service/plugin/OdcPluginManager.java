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

import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Collectors;

import org.pf4j.ExtensionPoint;
import org.pf4j.PluginManager;

import com.oceanbase.odc.core.shared.exception.UnsupportedException;

import lombok.NonNull;

public class OdcPluginManager<V> {

    private final PluginManager pluginManager;
    private final PluginFinder<V> pluginFinder;

    public OdcPluginManager(@NonNull PluginManager pluginManager, PluginFinder<V> pluginFinder) {
        this.pluginManager = pluginManager;
        this.pluginFinder = pluginFinder;
    }

    /**
     * 获取指定对象的指定类型的扩展点列表
     *
     * @param object 指定对象
     * @param type 指定类型
     * @param <T> 泛型，表示扩展点的类型
     * @return 指定对象的指定类型的扩展点列表
     */
    public <T extends ExtensionPoint> List<T> getExtensions(
            @NonNull V object, @NonNull Class<T> type) {
        // 通过指定对象获取对应的插件ID
        String pluginId = pluginFinder.findPluginIdBy(object);
        // 返回指定插件ID和指定类型的扩展点列表
        return pluginManager.getExtensions(type, pluginId);
    }

    public <T extends ExtensionPoint> List<Class<? extends T>> getExtensionClasses(
            @NonNull V object, @NonNull Class<T> type) {
        String pluginId = pluginFinder.findPluginIdBy(object);
        return this.pluginManager.getExtensionClasses(type, pluginId);
    }

    public <T extends ExtensionPoint> T getSingletonExtension(@NonNull V object, @NonNull Class<T> type) {
        return getSingleton(getExtensions(object, type), object);
    }

    private <T extends ExtensionPoint> T getSingleton(List<T> collection, V object) {
        if (collection.size() > 1) {
            String message = MessageFormat.format("Expect single extension for {}, but got "
                    + "{}, extension : {}", object, collection.size(),
                    collection.stream().map(t -> t.getClass().getSimpleName()).collect(Collectors.toList()));
            throw new IllegalStateException(message);
        } else if (collection.isEmpty()) {
            throw new UnsupportedException(String.format("Feature extension point is not supported for %s", object));
        }
        return collection.get(0);
    }

}
