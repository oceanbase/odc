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
package com.oceanbase.odc.service.iam.auth.oauth2;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.MapOAuth2AccessTokenResponseConverter;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;

import com.oceanbase.odc.common.util.StringUtils;

import lombok.NonNull;

public class CasPlainConversionService implements ConversionService {

    private static final String AMPERSAND_PATTERN = "&";

    private static final String EQUAL_PATTERN = "=";

    private final Converter<Map<String, String>, OAuth2AccessTokenResponse> tokenResponseConverter =
            new MapOAuth2AccessTokenResponseConverter();

    @Override
    public boolean canConvert(Class<?> sourceType, @NonNull Class<?> targetType) {
        TypeDescriptor source = sourceType == null ? null : TypeDescriptor.valueOf(sourceType);
        return canConvert(source, TypeDescriptor.valueOf(targetType));
    }

    @Override
    public boolean canConvert(TypeDescriptor sourceType, @NonNull TypeDescriptor targetType) {
        return sourceType != null && sourceType.isAssignableTo(TypeDescriptor.valueOf(String.class))
                && targetType.isAssignableTo(TypeDescriptor.valueOf(OAuth2AccessTokenResponse.class));
    }

    @Override
    public <T> T convert(Object source, @NonNull Class<T> targetType) {
        return targetType.cast(convert(source));
    }

    @Override
    public Object convert(Object source, TypeDescriptor sourceType, @NonNull TypeDescriptor targetType) {
        return targetType.getObjectType().cast(convert(source));
    }


    private OAuth2AccessTokenResponse convert(Object source) {
        if (!source.getClass().isAssignableFrom(String.class)) {
            throw new HttpMessageConversionException(
                    "unsupported source object class'" + source.getClass() + "'");
        }
        Map<String, String> responseParameterMap = convertToParameter((String) source);
        return tokenResponseConverter.convert(responseParameterMap);
    }

    private Map<String, String> convertToParameter(String plainText) {
        Map<String, String> res = new HashMap<>();
        if (StringUtils.isBlank(plainText)) {
            return res;
        }
        try {
            Arrays.stream(plainText.split(AMPERSAND_PATTERN)).forEach(t -> {
                String[] split = t.split(EQUAL_PATTERN);
                String parameterKey = split[0];
                String parameterValue = split[1];
                res.computeIfAbsent(parameterKey, (key) -> parameterValue);
            });

        } catch (Exception e) {
            throw new HttpMessageConversionException(
                    "Unexpected cas text/plain format, plainText is '" + plainText + "'", e);
        }
        // text/plain may not have token_type, but it needs;
        // @see <a target="_blank" href= * "https://tools.ietf.org/html/rfc6749#section-7.1">Section 7.1
        res.computeIfAbsent(OAuth2ParameterNames.TOKEN_TYPE,
                (key) -> OAuth2AccessToken.TokenType.BEARER.getValue());
        return res;
    }

}
