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
package com.oceanbase.odc.service.info;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;

import org.apache.catalina.connector.RequestFacade;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.ServiceTestEnv;

import lombok.SneakyThrows;

public class OdcInfoServiceTest extends ServiceTestEnv {

    @Autowired
    private OdcInfoService odcInfoService;

    private HttpServletRequest request;

    @Before
    public void before() {

    }

    @Test
    @SneakyThrows
    public void info_notAlipay_false() {
        request = mock(RequestFacade.class);
        when(request.getRequestURL()).thenReturn(new StringBuffer("1232"));
        when(request.getRequestURI()).thenReturn("http://odc.example.com");
        when(request.getHeaderNames()).thenReturn(new Enumeration<String>() {
            @Override
            public boolean hasMoreElements() {
                return false;
            }

            @Override
            public String nextElement() {
                return "";
            }
        });
        Field declaredField = OdcInfoService.class.getDeclaredField("request");
        declaredField.setAccessible(true);
        declaredField.set(odcInfoService, request);
        OdcInfo info = odcInfoService.info();
        Assert.assertFalse(info.isPasswordLoginEnabled());
    }
}
