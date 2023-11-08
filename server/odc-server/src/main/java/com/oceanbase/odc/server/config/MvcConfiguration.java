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
package com.oceanbase.odc.server.config;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.web.servlet.ConditionalOnMissingFilterBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.filter.OrderedRequestContextFilter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.filter.RequestContextFilter;
import org.springframework.web.filter.ShallowEtagHeaderFilter;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.util.UrlPathHelper;
import org.thymeleaf.spring5.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oceanbase.odc.config.BeanConfiguration;
import com.oceanbase.odc.server.web.highavailable.RateLimitInterceptor;
import com.oceanbase.odc.server.web.trace.TraceHandlerInterceptor;
import com.oceanbase.odc.service.iam.auth.OrganizationAuthenticationInterceptor;

import lombok.extern.slf4j.Slf4j;

/**
 * @auther kuiseng.zhb
 */
@Slf4j
@Configuration
public class MvcConfiguration implements WebMvcConfigurer {
    private static final String DEFAULT_INDEX_PAGE = "index.html";
    private static final String DEFAULT_WEB_STATIC_LOCATION = "classpath:/static/";

    @Value("${odc.web.static-resource.cache-timeout-seconds:60}")
    private Integer CACHE_TIMEOUT_SECONDS;

    @Value("${ODC_INDEX_PAGE_URI:}")
    private String indexPage;
    @Value("${ODC_WEB_STATIC_LOCATION:}")
    private String webStaticLocation;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private TraceHandlerInterceptor traceHandlerInterceptor;
    @Autowired
    private RateLimitInterceptor rateLimitInterceptor;
    @Autowired
    private StringToDateConverter stringToDateConverter;
    @Autowired
    private OrganizationAuthenticationInterceptor organizationAuthenticationInterceptor;

    public MvcConfiguration() {
        log.info("mvc configuration initialized");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedHeaders("*")
                .allowedMethods("*")
                .allowedOriginPatterns("*")
                .allowCredentials(true);
    }

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer config) {
        config.defaultContentType(MediaType.APPLICATION_JSON, MediaType.ALL);
    }

    /**
     * 配置时间序列化格式为 ISO， 用于 query parameter 时间类型的格式 json response 基于 ObjectMapper 实现序列化, refer
     * {@link BeanConfiguration#objectMapper}
     *
     * @param registry
     */
    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(stringToDateConverter);
        DateTimeFormatterRegistrar registrar = new DateTimeFormatterRegistrar();
        registrar.setUseIsoFormat(true);
        registrar.registerFormatters(registry);
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        int xmlConverterIndex = -1;
        int jacksonConverterIndex = -1;
        for (int i = 0; i < converters.size(); i++) {
            HttpMessageConverter<?> converter = converters.get(i);
            if (converter instanceof MappingJackson2XmlHttpMessageConverter) {
                xmlConverterIndex = i;
            }
            if (converter instanceof MappingJackson2HttpMessageConverter) {
                jacksonConverterIndex = i;
                MappingJackson2HttpMessageConverter jacksonConverter = (MappingJackson2HttpMessageConverter) converter;
                jacksonConverter.setObjectMapper(objectMapper);
                log.info("update objectMapper for jackson http message converter");
            }
        }
        if (jacksonConverterIndex == -1) {
            throw new RuntimeException("no jackson http message converter found!");
        }
        // swap xml converter and jackson converter if the xmlConverter exists and is before
        // jacksonConverter
        if (xmlConverterIndex != -1 && xmlConverterIndex < jacksonConverterIndex) {
            HttpMessageConverter<?> jacksonConverter = converters.get(jacksonConverterIndex);
            converters.set(jacksonConverterIndex, converters.get(xmlConverterIndex));
            converters.set(xmlConverterIndex, jacksonConverter);
        }
    }


    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String webStaticLocation = getWebStaticLocation();

        // handle index.html for CDN integration
        registry.addResourceHandler("/index.html")
                .addResourceLocations(getIndexPageLocation()).setCachePeriod(CACHE_TIMEOUT_SECONDS);

        // handle common static resource location
        registry.addResourceHandler("/css/**")
                .addResourceLocations(webStaticLocation + "css/").setCachePeriod(CACHE_TIMEOUT_SECONDS);

        registry.addResourceHandler("/img/**")
                .addResourceLocations(webStaticLocation + "img/").setCachePeriod(CACHE_TIMEOUT_SECONDS);
        registry.addResourceHandler("/img/en-us/**")
                .addResourceLocations(webStaticLocation + "img/en-us/").setCachePeriod(CACHE_TIMEOUT_SECONDS);
        registry.addResourceHandler("/img/zh-cn/**")
                .addResourceLocations(webStaticLocation + "img/zh-cn/").setCachePeriod(CACHE_TIMEOUT_SECONDS);

        registry.addResourceHandler("/template/en-us/**")
                .addResourceLocations(webStaticLocation + "template/en-us/").setCachePeriod(CACHE_TIMEOUT_SECONDS);
        registry.addResourceHandler("/template/zh-cn/**")
                .addResourceLocations(webStaticLocation + "template/zh-cn/").setCachePeriod(CACHE_TIMEOUT_SECONDS);
        registry.addResourceHandler("/template/zh-tw/**")
                .addResourceLocations(webStaticLocation + "template/zh-tw/").setCachePeriod(CACHE_TIMEOUT_SECONDS);

        registry.addResourceHandler("/help/**")
                .addResourceLocations(webStaticLocation + "help/").setCachePeriod(CACHE_TIMEOUT_SECONDS);
        registry.addResourceHandler("/help-client/**")
                .addResourceLocations(webStaticLocation + "help-client/").setCachePeriod(CACHE_TIMEOUT_SECONDS);
        registry.addResourceHandler("/help-web/**")
                .addResourceLocations(webStaticLocation + "help-web/").setCachePeriod(CACHE_TIMEOUT_SECONDS);
        registry.addResourceHandler("/help-pdf/**")
                .addResourceLocations(webStaticLocation + "help-pdf/").setCachePeriod(CACHE_TIMEOUT_SECONDS);
        registry.addResourceHandler("/help-doc/**")
                .addResourceLocations(webStaticLocation + "help-doc/").setCachePeriod(CACHE_TIMEOUT_SECONDS);
        registry.addResourceHandler("/help/data/json/**")
                .addResourceLocations(webStaticLocation + "help/data/json/").setCachePeriod(CACHE_TIMEOUT_SECONDS);
        registry.addResourceHandler("/help/data/img/**")
                .addResourceLocations(webStaticLocation + "help/data/img/").setCachePeriod(CACHE_TIMEOUT_SECONDS);

        registry.addResourceHandler("/workers/**")
                .addResourceLocations(webStaticLocation + "workers/").setCachePeriod(CACHE_TIMEOUT_SECONDS);

        registry.addResourceHandler(
                "/index.body.html",
                "/index.head.html",
                "/404.html",
                "/umi.**.js",
                "/**.async.js",
                "/editor.worker.js",
                "/**.chunk.css",
                "/umi.**.css",
                "/bigfish.json",
                "/basement.config.json",
                "/map.json")
                .addResourceLocations(webStaticLocation).setCachePeriod(CACHE_TIMEOUT_SECONDS);

        registry.addResourceHandler("/static/**")
                .addResourceLocations(webStaticLocation + "static/").setCachePeriod(CACHE_TIMEOUT_SECONDS);

        registry.setOrder(Ordered.HIGHEST_PRECEDENCE);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(traceHandlerInterceptor).addPathPatterns("/**");
        registry.addInterceptor(rateLimitInterceptor).addPathPatterns("/api/**");
        registry.addInterceptor(organizationAuthenticationInterceptor).addPathPatterns("/api/**");
    }

    /**
     * for handle path variables contains slash '/' char <br>
     * 1. the complete solution require below system property set: <br>
     * System.setProperty("org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH", "true"); <br>
     * 2. urlPathHelper.setAlwaysUseFullPath make no difference here <br>
     * 
     * refer more from
     * https://stackoverflow.com/questions/13482020/encoded-slash-2f-with-spring-requestmapping-path-param-gives-http-400
     */
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        UrlPathHelper urlPathHelper = new UrlPathHelper();
        urlPathHelper.setUrlDecode(false);
        urlPathHelper.setAlwaysUseFullPath(true);// 设置总使用完整路径
        configurer.setUseSuffixPatternMatch(false);
        configurer.setUseRegisteredSuffixPatternMatch(true);
        configurer.setUrlPathHelper(urlPathHelper);
    }

    @Bean
    public SpringResourceTemplateResolver defaultTemplateResolver(ApplicationContext applicationContext) {
        SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
        resolver.setApplicationContext(applicationContext);
        resolver.setPrefix(getWebStaticLocation());
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCacheable(true);
        resolver.setCheckExistence(true);
        resolver.setCacheTTLMs(CACHE_TIMEOUT_SECONDS * 1000L);
        return resolver;
    }

    @Bean
    @ConditionalOnMissingBean({RequestContextListener.class, RequestContextFilter.class})
    @ConditionalOnMissingFilterBean(RequestContextFilter.class)
    public static RequestContextFilter requestContextFilter() {
        return new OrderedRequestContextFilter();
    }

    @Bean
    public FilterRegistrationBean<ShallowEtagHeaderFilter> shallowEtagHeaderFilter() {
        FilterRegistrationBean<ShallowEtagHeaderFilter> filterRegistrationBean =
                new FilterRegistrationBean<>(new ShallowEtagHeaderFilter());
        filterRegistrationBean.addUrlPatterns("/index.html", "/");
        filterRegistrationBean.setName("etagFilter");
        return filterRegistrationBean;
    }

    private String getIndexPageLocation() {
        if (StringUtils.isNotBlank(indexPage)) {
            log.info("ODC_INDEX_PAGE_URI set, will use cdn version, indexPage={}", indexPage);
            return "[charset=UTF-8]" + indexPage;
        }
        return getWebStaticLocation() + DEFAULT_INDEX_PAGE;
    }

    private String getWebStaticLocation() {
        if (StringUtils.isNotBlank(indexPage)) {
            String staticLocation = StringUtils.substringBeforeLast(indexPage, "/") + "/";
            log.info("ODC_INDEX_PAGE_URI set, will use cdn static location, staticLocation={}", staticLocation);
            return staticLocation;
        }
        if (StringUtils.isNotBlank(this.webStaticLocation)) {
            log.info("ODC_WEB_STATIC_LOCATION set, will use external static resource, webStaticLocation={}",
                    webStaticLocation);
            return this.webStaticLocation;
        }
        return DEFAULT_WEB_STATIC_LOCATION;
    }
}
