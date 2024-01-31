/*
 * Copyright (c) 2024 OceanBase.
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

package com.oceanbase.odc.common.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

/**
 * @author liuyizhuo.lyz
 * @date 2024/1/30
 */
public class ReflectionUtils {

    public static <T> T getProxiedFieldValue(Object any, Class<?> type, String fieldName) {
        if (!Proxy.isProxyClass(any.getClass()) || Proxy.getInvocationHandler(any).getClass() != type) {
            return null;
        }
        InvocationHandler handler = Proxy.getInvocationHandler(any);
        return getFieldValue(handler, fieldName);
    }

    public static <T> T getFieldValue(Object any, String fieldName) {
        try {
            Field field = any.getClass().getDeclaredField(fieldName);
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }
            return (T) field.get(any);
        } catch (NoSuchFieldException | IllegalAccessException | ClassCastException e) {
            return null;
        }
    }

}
