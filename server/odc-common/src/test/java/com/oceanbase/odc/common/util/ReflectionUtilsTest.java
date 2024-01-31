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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author liuyizhuo.lyz
 * @date 2024/1/30
 */
public class ReflectionUtilsTest {

    @Test
    public void test_getFieldValue() throws NoSuchFieldException, IllegalAccessException {
        Pojo pojo = new Pojo("fuzzyname");
        Object name = ReflectionUtils.getFieldValue(pojo, "name");
        Assert.assertEquals("fuzzyname", name);
    }

    @Test
    public void test_getProxiedFieldValue() throws NoSuchFieldException, IllegalAccessException {
        Object instance = Proxy.newProxyInstance(Pojo.class.getClassLoader(), new Class[] {FooInterface.class},
                new PojoInvocationHandler(new FooInterface() {}));
        Object target = ReflectionUtils.getProxiedFieldValue(instance, PojoInvocationHandler.class, "target");
        Assert.assertTrue(target instanceof FooInterface);
    }

    private static class Pojo {
        private final String name;

        public Pojo(String name) {
            this.name = name;
        }
    }

    private interface FooInterface {
    }

    private static class PojoInvocationHandler implements InvocationHandler {

        private FooInterface target;

        public PojoInvocationHandler(FooInterface target) {
            this.target = target;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return null;
        }
    }

}
