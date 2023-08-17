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
package com.oceanbase.odc.config;

import static org.springframework.web.servlet.DispatcherServlet.LOCALE_RESOLVER_BEAN_NAME;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import com.oceanbase.odc.common.i18n.I18n;

import lombok.extern.slf4j.Slf4j;

/**
 * refer validation message configuration from
 * https://www.baeldung.com/spring-custom-validation-message-source
 * 
 * @author yizhou.xw
 * @version : I18nConfiguration.java, v 0.1 2021-02-19 11:54
 */
@Configuration
public class I18nConfiguration implements WebMvcConfigurer {

    @Bean
    public MessageSource messageSource() {
        return I18n.getMessageSource();
    }

    @Bean
    public LocalValidatorFactoryBean getValidator() {
        LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();
        bean.setValidationMessageSource(messageSource());
        return bean;
    }

    @Bean
    public LocalResolverPostProcessor localResolverPostProcessor() {
        return new LocalResolverPostProcessor();
    }

}


class LocalResolverPostProcessor implements BeanDefinitionRegistryPostProcessor {

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry beanDefinitionRegistry) throws BeansException {
        // 替换默认实现
        if (beanDefinitionRegistry.containsBeanDefinition(LOCALE_RESOLVER_BEAN_NAME)) {
            beanDefinitionRegistry.removeBeanDefinition(LOCALE_RESOLVER_BEAN_NAME);
            GenericBeanDefinition odcLocalResolver = new GenericBeanDefinition();
            odcLocalResolver.setBeanClass(CustomLocaleResolver.class);
            ConstructorArgumentValues constructorArgumentValues = new ConstructorArgumentValues();
            constructorArgumentValues.addGenericArgumentValue("lang");
            odcLocalResolver.setConstructorArgumentValues(constructorArgumentValues);
            beanDefinitionRegistry.registerBeanDefinition(LOCALE_RESOLVER_BEAN_NAME, odcLocalResolver);
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory)
            throws BeansException {

    }
}


@Slf4j
class CustomLocaleResolver extends AcceptHeaderLocaleResolver {

    private final String localeParametername;

    public CustomLocaleResolver(String localeParametername) {
        this.localeParametername = localeParametername;
    }

    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        String newLocale = request.getParameter(localeParametername);
        if (newLocale != null) {
            try {
                return parseLocaleValue(newLocale);
            } catch (IllegalArgumentException ex) {
                log.warn(
                        "Failed to parse locale value, requestURI={}, parameterName={}, parameterValue={}",
                        request.getRequestURI(), localeParametername, newLocale);
            }
        }
        return super.resolveLocale(request);
    }

    @Nullable
    private Locale parseLocaleValue(String localeValue) {
        return StringUtils.parseLocale(localeValue);
    }

}
