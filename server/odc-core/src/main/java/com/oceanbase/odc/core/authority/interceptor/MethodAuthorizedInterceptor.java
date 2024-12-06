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
package com.oceanbase.odc.core.authority.interceptor;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;

import com.oceanbase.odc.core.authority.SecurityManager;
import com.oceanbase.odc.core.authority.exception.AccessDeniedException;
import com.oceanbase.odc.core.authority.model.DefaultSecurityResource;
import com.oceanbase.odc.core.authority.model.SecurityResource;
import com.oceanbase.odc.core.authority.permission.Permission;
import com.oceanbase.odc.core.authority.util.PostReturnValueFilter;
import com.oceanbase.odc.core.authority.util.PreAuthenticate;

/**
 * Interceptor for authentication module, used to intercept the invocation of a method for
 * Authentication
 *
 * @author yh263208
 * @date 2021-07-06 16:32
 * @since ODC_release_3.2.0
 */
public class MethodAuthorizedInterceptor implements MethodInterceptor {

    private final ApplicationContext context;

    public MethodAuthorizedInterceptor(ApplicationContext context) {
        this.context = context;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Method method = invocation.getMethod();
        PreAuthenticate[] preAuthenticates = method.getAnnotationsByType(PreAuthenticate.class);
        PostReturnValueFilter filter = method.getDeclaredAnnotation(PostReturnValueFilter.class);
        if (preAuthenticates.length == 0 && filter == null) {
            return invocation.proceed();
        }
        for (PreAuthenticate preAuthenticate : preAuthenticates) {
            Permission permission = beforeAuthentication(preAuthenticate, invocation.getThis(),
                    invocation.getMethod(), invocation.getArguments());
            getSecurityManager().checkPermission(permission);
        }
        Object returnVal = invocation.proceed();
        if (returnVal == null) {
            return returnVal;
        }
        if (filter != null) {
            return getSecurityManager().decide(returnVal);
        }
        return returnVal;
    }

    private Permission beforeAuthentication(PreAuthenticate annotation, Object target, Method method,
            Object[] objects) {
        if (target == null) {
            throw new AccessDeniedException(new NullPointerException("Target object is null"));
        }
        String resourceType;
        String resourceId;
        if (SecurityResource.class.isAssignableFrom(target.getClass())) {
            /**
             * If you are here, which means that the object is the implements for <code>SecurityResource</code>
             */
            resourceType = ((SecurityResource) target).resourceType();
            resourceId = ((SecurityResource) target).resourceId();
        } else {
            resourceType = annotation.resourceType();
            if (!annotation.isForAll()) {
                int index = annotation.indexOfIdParam();
                Parameter[] parameters = method.getParameters();
                if (parameters == null || parameters.length == 0) {
                    throw new NullPointerException("Parameter is null or empty");
                }
                if (index < 0 || index > parameters.length - 1) {
                    throw new ArrayIndexOutOfBoundsException(
                            "ResourceId's parameter index is out of range [0," + parameters.length + "]");
                }
                Parameter parameter = parameters[index];
                Object argument = objects[index];
                if (argument == null) {
                    throw new NullPointerException("The value of parameter " + parameter.getName() + " is null");
                }
                resourceId = argument.toString();
            } else {
                resourceId = "*";
            }
        }
        List<String> actions = Arrays.asList(annotation.actions());
        List<String> resourceRoles = Arrays.asList(annotation.hasAnyResourceRole());
        String resourceRole = annotation.hasResourceRole();

        if (CollectionUtils.isNotEmpty(actions)
                && (CollectionUtils.isNotEmpty(resourceRoles) || StringUtils.isNotBlank(resourceRole))) {
            return getSecurityManager().getPermissionByActionsAndResourceRoles(
                    new DefaultSecurityResource(resourceId, resourceType), actions, resourceRoles);
        }

        if (CollectionUtils.isNotEmpty(actions)) {
            return getSecurityManager().getPermissionByActions(new DefaultSecurityResource(resourceId, resourceType),
                    actions);
        } else if (CollectionUtils.isNotEmpty(resourceRoles)) {
            return getSecurityManager().getPermissionByResourceRoles(
                    new DefaultSecurityResource(resourceId, resourceType),
                    resourceRoles);
        } else if (StringUtils.isNotBlank(resourceRole)) {
            return getSecurityManager().getPermissionByResourceRoles(
                    new DefaultSecurityResource(resourceId, resourceType),
                    Collections.singleton(resourceRole));
        } else {
            throw new NullPointerException("The actions and hasAnyRole are both empty");
        }

    }

    private SecurityManager getSecurityManager() {
        return context.getBean(SecurityManager.class);
    }

}
