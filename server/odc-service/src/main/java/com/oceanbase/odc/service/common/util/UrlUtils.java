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
package com.oceanbase.odc.service.common.util;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.common.base.MoreObjects;

import lombok.SneakyThrows;

public class UrlUtils {

    public static String appendQueryParameter(String url, String paramName, String paramValue) {
        String encodedValue = com.oceanbase.odc.common.util.StringUtils.urlEncode(paramValue);
        if (!url.contains("?")) {
            url += "?";
        } else {
            url += "&";
        }
        url += paramName + "=" + encodedValue;
        return url;
    }

    @SneakyThrows
    public static List<String> getQueryParameter(String url, String parameterName) {
        List<String> objects = MoreObjects.firstNonNull(
                UriComponentsBuilder.fromUriString(url).build().getQueryParams().get(parameterName),
                new ArrayList<String>());
        return objects.stream().map(UrlUtils::decode).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public static String decode(String encode) {
        try {
            return URLDecoder.decode(encode, "UTF-8");
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    public static String getQueryParameterFirst(String url, String parameterName) {
        List<String> strings = getQueryParameter(url, parameterName);
        return CollectionUtils.isEmpty(strings) ? null : strings.get(0);
    }

    @SneakyThrows
    public static String getPath(String url) {
        return UriComponentsBuilder.fromUriString(url).build().getPath();
    }

}
