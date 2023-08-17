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

import org.springframework.aop.Pointcut;
import org.springframework.aop.framework.autoproxy.AbstractBeanFactoryAwareAdvisingPostProcessor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;

import com.oceanbase.odc.core.authority.util.Authenticated;

/**
 * Post processor for authentication module, used to add an advisor for Authentication
 *
 * @author yh263208
 * @date 2021-07-06 16:34
 * @since ODC_release_3.2.0
 */
public class MethodAuthorizedPostProcessor extends AbstractBeanFactoryAwareAdvisingPostProcessor
        implements InitializingBean {

    private final ApplicationContext context;

    public MethodAuthorizedPostProcessor(ApplicationContext context) {
        this.context = context;
    }

    @Override
    public void afterPropertiesSet() {
        Pointcut pointcut = new AnnotationMatchingPointcut(Authenticated.class, true);
        this.advisor = new DefaultPointcutAdvisor(pointcut, new MethodAuthorizedInterceptor(context));
    }

}
