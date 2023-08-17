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
package com.oceanbase.odc.core.shared.constant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;
import org.reflections.Reflections;

import com.oceanbase.odc.common.i18n.Translatable;
import com.oceanbase.odc.common.util.StringUtils;

public class TranslatableTest {

    /**
     * check i18n value missed, skip for Audit related
     */
    @Test
    public void translate_AllTranslatable_HasI18nValue() {
        Reflections reflections = new Reflections("com.oceanbase.odc");
        List<String> missI18nValues = new ArrayList<>();
        reflections.getSubTypesOf(Translatable.class).stream()
                .filter(Class::isEnum)
                .forEach(translatable -> Arrays.stream(translatable.getEnumConstants()).forEach(enumValue -> {
                    Arrays.asList(Locale.US, Locale.SIMPLIFIED_CHINESE, Locale.TRADITIONAL_CHINESE).forEach(
                            locale -> {
                                String i18Value = enumValue.translate(null, locale);
                                if (StringUtils.startsWith(i18Value, Translatable.I18N_KEY_PREFIX)) {
                                    String missI18nValue = "Enum " + enumValue.getClass().getSimpleName() + "."
                                            + enumValue.code() + " miss " + locale.getDisplayName()
                                            + " i18n value";
                                    missI18nValues.add(missI18nValue);
                                }
                            });
                }));
        Assert.assertTrue(String.format("There exits %d translatable enum(s) miss i18n value!\n%s",
                missI18nValues.size(), String.join("\t\n", missI18nValues)), missI18nValues.isEmpty());
    }
}
