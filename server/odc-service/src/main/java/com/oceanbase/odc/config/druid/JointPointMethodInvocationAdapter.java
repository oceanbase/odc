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

package com.oceanbase.odc.config.druid;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInvocation;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

class JointPointMethodInvocationAdapter implements MethodInvocation {
    private final ProceedingJoinPoint joinPoint;
    private final MethodSignature methodSignature;
    private final Method method;

    public JointPointMethodInvocationAdapter(ProceedingJoinPoint joinPoint) {
        this.joinPoint = joinPoint;
        this.methodSignature = (MethodSignature) joinPoint.getSignature();
        this.method = methodSignature.getMethod();
    }

    @Override
    public Object proceed() throws Throwable {
        return joinPoint.proceed();
    }

    @Override
    public Object getThis() {
        return joinPoint.getThis();
    }

    @Override
    public Object[] getArguments() {
        return joinPoint.getArgs();
    }

    @Override
    public Method getMethod() {
        return method;
    }

    @Override
    public AccessibleObject getStaticPart() {
        return method;
    }
}
