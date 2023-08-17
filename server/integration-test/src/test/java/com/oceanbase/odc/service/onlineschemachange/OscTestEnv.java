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
package com.oceanbase.odc.service.onlineschemachange;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Import;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.service.onlineschemachange.OscTestEnv.OnlineSchemaChangePropertiesBeanPostProcessor;
import com.oceanbase.odc.service.onlineschemachange.configuration.OnlineSchemaChangeProperties;
import com.oceanbase.odc.service.onlineschemachange.configuration.OnlineSchemaChangeProperties.OmsProperties;

/**
 * @author yaobin
 * @date 2023-07-17
 * @since 4.2.0
 */
@Import(OnlineSchemaChangePropertiesBeanPostProcessor.class)
public class OscTestEnv extends ServiceTestEnv {

    public static class OnlineSchemaChangePropertiesBeanPostProcessor implements BeanPostProcessor {
        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
            if (bean instanceof OnlineSchemaChangeProperties) {
                OnlineSchemaChangeProperties onlineSchemaChangeProperties = (OnlineSchemaChangeProperties) bean;
                OmsProperties omsProperties = new OmsProperties();
                onlineSchemaChangeProperties.setOms(omsProperties);
            }
            return BeanPostProcessor.super.postProcessAfterInitialization(bean, beanName);
        }
    }
}
