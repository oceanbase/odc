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
package com.oceanbase.odc.service.objectstorage.cloud.model;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.odc.service.objectstorage.cloud.model.ObjectTagging.Tag;

import lombok.Data;

@Data
public class ObjectMetadata {
    private Map<String, String> userMetadata = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private Long contentLength;
    private Date expirationTime;
    private String contentMD5;
    private String contentType;
    private String eTag;
    /**
     * for mark object temp while put object, works with LifeCycle policy
     */
    private ObjectTagging tagging;

    public static ObjectMetadata temp() {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setTagging(ObjectTagging.temp());
        return metadata;
    }

    public boolean hasTag() {
        if (Objects.isNull(tagging)) {
            return false;
        }
        List<Tag> tagSet = tagging.getTagSet();
        return CollectionUtils.isNotEmpty(tagSet);
    }
}
