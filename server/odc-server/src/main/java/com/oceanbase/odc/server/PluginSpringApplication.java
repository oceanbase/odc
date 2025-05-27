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
package com.oceanbase.odc.server;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import com.oceanbase.odc.server.module.Modules;
import com.oceanbase.odc.server.starter.Starters;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PluginSpringApplication extends SpringApplication {

    public PluginSpringApplication(Class<?>... primarySources) {
        super(primarySources);
    }

    public static ConfigurableApplicationContext run(Class<?> primarySource, String... args) {
        return run(new Class<?>[] {primarySource}, args);
    }

    public static ConfigurableApplicationContext run(Class<?>[] primarySources, String[] args) {
        return new PluginSpringApplication(primarySources).run(args);
    }

    /**
     * Different startup methods will use different default class loaders. For example, java -jar by
     * default uses LaunchedURLClassLoader, while when executing by specifying directories through
     * classPath or -cp parameters, it will directly use AppClassLoader.
     *
     * Since Spring 3.x, AppClassLoader no longer extends from URLClassLoader, so compatibility handling is performed here.
     * Note: must add --add-opens java.base/jdk.internal.loader=ALL-UNNAMED to startup command
     * @param addToPath
     * @param classLoader
     * @throws ClassNotFoundException
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     */
    public static void addUrlToClassLoader(List<URL> addToPath, ClassLoader classLoader)
            throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException, NoSuchMethodException,
            InvocationTargetException {

        if (classLoader instanceof URLClassLoader urlClassLoader) {
            Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            method.setAccessible(true);
            for (URL url : addToPath) {
                method.invoke(urlClassLoader, url);
                log.info("Jar has been added to classpath, url={}", url);
            }
            return;
        }

        Class<?> builtinClassLoaderClass = Class.forName("jdk.internal.loader.BuiltinClassLoader");
        if (builtinClassLoaderClass.isInstance(classLoader)) {
            Field ucpField = builtinClassLoaderClass.getDeclaredField("ucp");
            ucpField.setAccessible(true);
            Object ucp = ucpField.get(classLoader);
            Class<?> urlClassPathClass = Class.forName("jdk.internal.loader.URLClassPath");
            Method addURL = urlClassPathClass.getMethod("addURL", URL.class);
            addURL.setAccessible(true);
            for (URL url : addToPath) {
                addURL.invoke(ucp, url);
                log.info("Jar has been added to classpath, url={}", url);
            }
        }



    }

    @Override
    protected void logStartupProfileInfo(ConfigurableApplicationContext context) {
        super.logStartupProfileInfo(context);
        Modules.load();
        String[] activeProfiles = context.getEnvironment().getActiveProfiles();
        if (activeProfiles.length != 0) {
            Starters.load(new HashSet<>(Arrays.asList(activeProfiles)));
        }
    }

}
