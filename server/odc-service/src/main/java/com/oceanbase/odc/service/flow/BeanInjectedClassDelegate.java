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
package com.oceanbase.odc.service.flow;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Stack;
import java.util.function.Consumer;

import javax.annotation.PostConstruct;

import org.flowable.bpmn.model.MapExceptionEntry;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.engine.impl.bpmn.helper.ClassDelegate;
import org.flowable.engine.impl.bpmn.parser.FieldDeclaration;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.oceanbase.odc.service.common.util.SpringContextUtil;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link BeanInjectedClassDelegate}
 *
 * @author yh263208
 * @date 2022-09-21 09:25
 * @since ODC_release_3.4.0
 */
@Slf4j
public class BeanInjectedClassDelegate extends ClassDelegate {

    public BeanInjectedClassDelegate(String id, String className, List<FieldDeclaration> fieldDeclarations,
            boolean triggerable, Expression skipExpression, List<MapExceptionEntry> mapExceptions) {
        super(id, className, fieldDeclarations, triggerable, skipExpression, mapExceptions);
    }

    public BeanInjectedClassDelegate(String className, List<FieldDeclaration> fieldDeclarations) {
        super(className, fieldDeclarations);
    }

    @Override
    protected Object instantiateDelegate(String className, List<FieldDeclaration> fieldDeclarations) {
        Object obj = super.instantiateDelegate(className, fieldDeclarations);
        if (obj == null) {
            return obj;
        }
        Class<?> target = obj.getClass();
        forEachClass(target, c -> injectAutowiredBeans(obj, c));
        forEachClass(target, c -> invokePostConstructMethod(obj, c));
        return obj;
    }

    public static <T> T instantiateDelegate(Class<T> beanClass) throws Exception {
        T beanInstance = beanClass.getDeclaredConstructor().newInstance();
        forEachClass(beanClass, c -> injectAutowiredBeans(beanInstance, c));
        forEachClass(beanClass, c -> invokePostConstructMethod(beanInstance, c));
        return beanInstance;
    }

    private static void forEachClass(@NonNull Class<?> begin, Consumer<Class<?>> consumer) {
        Stack<Class<?>> stack = new Stack<>();
        stack.push(begin);
        Class<?> target = begin.getSuperclass();
        while (target != null && !target.equals(Object.class)) {
            stack.push(target);
            target = target.getSuperclass();
        }
        while (stack.size() > 0) {
            consumer.accept(stack.pop());
        }
    }

    private static void invokePostConstructMethod(Object thisObj, Class<?> target) {
        Method[] methods = target.getDeclaredMethods();
        for (Method method : methods) {
            PostConstruct postConstruct = findAnnotation(method, PostConstruct.class);
            if (postConstruct == null) {
                continue;
            }
            Parameter[] parameters = method.getParameters();
            Object[] paramValue = new Object[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                Parameter parameter = parameters[i];
                String beanName = null;
                Qualifier qualifier = parameter.getDeclaredAnnotation(Qualifier.class);
                if (qualifier != null) {
                    beanName = qualifier.value();
                }
                paramValue[i] = getBean(beanName, parameter.getType());
            }
            try {
                method.setAccessible(true);
                method.invoke(thisObj, paramValue);
            } catch (Exception e) {
                throw new BeanCreationException("Invocation of init method failed", e);
            }
        }
    }

    private static void injectAutowiredBeans(Object thisObj, Class<?> target) {
        Field[] fields = target.getDeclaredFields();
        for (Field field : fields) {
            Autowired autowired = findAnnotation(field, Autowired.class);
            if (autowired == null) {
                continue;
            }
            Qualifier qualifier = findAnnotation(field, Qualifier.class);
            String beanName = null;
            if (qualifier != null) {
                beanName = qualifier.value();
            }
            Object bean = getBean(beanName, field.getType());
            if (bean != null) {
                try {
                    field.setAccessible(true);
                    field.set(thisObj, bean);
                } catch (IllegalAccessException e) {
                    log.warn("Failed to inject field value", e);
                    throw new IllegalStateException(e);
                }
            } else if (autowired.required()) {
                throw new NoSuchBeanDefinitionException(beanName == null ? field.getName() : beanName);
            }
        }
    }

    private static Object getBean(String beanName, @NonNull Class<?> clazz) {
        try {
            if (beanName == null) {
                return SpringContextUtil.getBean(clazz);
            }
            return SpringContextUtil.getBean(beanName, clazz);
        } catch (Exception e) {
            log.warn("Failed to get bean", e);
        }
        return null;
    }

    private static <T extends Annotation> T findAnnotation(@NonNull Field field, Class<T> clazz) {
        T[] annotations = field.getDeclaredAnnotationsByType(clazz);
        if (annotations == null || annotations.length == 0) {
            return null;
        }
        return annotations[0];
    }

    private static <T extends Annotation> T findAnnotation(@NonNull Method method, Class<T> clazz) {
        T[] annotations = method.getDeclaredAnnotationsByType(clazz);
        if (annotations == null || annotations.length == 0) {
            return null;
        }
        return annotations[0];
    }

}
