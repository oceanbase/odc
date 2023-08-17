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
package com.oceanbase.odc.core.authority.auth;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.security.auth.Subject;

import com.oceanbase.odc.core.authority.exception.AccessDeniedException;
import com.oceanbase.odc.core.authority.model.SecurityResource;
import com.oceanbase.odc.core.authority.permission.Permission;
import com.oceanbase.odc.core.authority.permission.ResourcePermission;

import lombok.NonNull;

/**
 * {@link DefaultReturnValueProvider}
 *
 * @author yh263208
 * @date 2023-04-04 14:22
 * @since ODC_release_4.2.0
 * @see ReturnValueProvider
 */
public class DefaultReturnValueProvider implements ReturnValueProvider {

    private final AuthorizerManager authorizerManager;
    private final PermissionStrategy strategy;

    public DefaultReturnValueProvider(@NonNull AuthorizerManager authorizerManager,
            @NonNull PermissionStrategy strategy) {
        this.authorizerManager = authorizerManager;
        this.strategy = strategy;
    }

    @Override
    public Object decide(Subject subject, Object returnValue, SecurityContext context)
            throws AccessDeniedException {
        if (returnValue instanceof SecurityResource) {
            authorizerManager.checkPermission(subject, getPermissions(returnValue), strategy);
        }
        try {
            rewriteReturnValue(subject, context, returnValue.getClass(), returnValue, 3);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return returnValue;
    }

    private void rewriteReturnValue(Subject subject, SecurityContext context,
            Class<?> clazz, Object objectVal, int maxDepth) throws IllegalAccessException {
        if (objectVal == null || maxDepth <= 0) {
            return;
        }
        if (Iterable.class.isAssignableFrom(clazz)) {
            Iterator<?> iterator = ((Iterable<?>) objectVal).iterator();
            while (iterator.hasNext()) {
                Object subValue = iterator.next();
                if (subValue == null) {
                    continue;
                }
                if (isResource(subValue)) {
                    if (!isPermitted(subject, subValue)) {
                        iterator.remove();
                        continue;
                    }
                }
                rewriteReturnValue(subject, context, subValue.getClass(), subValue, maxDepth - 1);
            }
            return;
        } else if (Map.class.isAssignableFrom(clazz)) {
            Iterator<? extends Entry<?, ?>> iterator = ((Map<?, ?>) objectVal).entrySet().iterator();
            while (iterator.hasNext()) {
                Entry<?, ?> entry = iterator.next();
                Object subValue = entry.getValue();
                if (subValue == null) {
                    continue;
                }
                if (isResource(subValue)) {
                    if (!isPermitted(subject, subValue)) {
                        iterator.remove();
                        continue;
                    }
                }
                rewriteReturnValue(subject, context, subValue.getClass(), subValue, maxDepth - 1);
            }
            return;
        } else if (clazz.isPrimitive() || clazz.getClassLoader() == null || clazz.isAnnotation() || clazz.isEnum()) {
            return;
        }
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            Object fieldVal = field.get(objectVal);
            if (isResource(field.getType())) {
                if (!isPermitted(subject, fieldVal)) {
                    field.set(objectVal, null);
                    continue;
                }
            }
            rewriteReturnValue(subject, context, field.getType(), fieldVal, maxDepth - 1);
        }
    }

    private boolean isPermitted(Subject subject, Object obj) {
        return strategy.decide(authorizerManager.permit(subject, getPermissions(obj)));
    }

    private boolean isResource(Class<?> clazz) {
        if (clazz == null) {
            return false;
        }
        return SecurityResource.class.isAssignableFrom(clazz);
    }

    private boolean isResource(Object obj) {
        if (obj == null) {
            return false;
        }
        return isResource(obj.getClass());
    }

    private List<Permission> getPermissions(Object obj) {
        if (!(obj instanceof SecurityResource)) {
            throw new IllegalArgumentException("Input parameter's type is illegal, " + obj);
        }
        return Collections.singletonList(new ResourcePermission((SecurityResource) obj, ResourcePermission.READ));
    }

}
