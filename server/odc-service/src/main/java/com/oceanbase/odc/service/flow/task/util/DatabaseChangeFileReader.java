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
package com.oceanbase.odc.service.flow.task.util;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.sql.split.OffsetString;
import com.oceanbase.odc.service.common.util.SqlUtils;
import com.oceanbase.odc.service.flow.task.model.DatabaseChangeParameters;
import com.oceanbase.odc.service.objectstorage.ObjectStorageFacade;
import com.oceanbase.odc.service.objectstorage.model.ObjectMetadata;

import lombok.extern.slf4j.Slf4j;

/**
 * @author jingtian
 * @date 2023/5/18
 * @since ODC_release_4.2.0
 */
@Slf4j
@Component
public class DatabaseChangeFileReader {
    @Autowired
    private ObjectStorageFacade storageFacade;

    public List<OffsetString> loadSqlContents(DatabaseChangeParameters params, DialectType dialectType,
            String bucketName,
            long maxSizeBytes)
            throws IOException {
        List<OffsetString> sqls = new LinkedList<>();
        String sqlContent = params.getSqlContent();
        if (StringUtils.isNotBlank(sqlContent)) {
            sqls.addAll(SqlUtils.splitWithOffset(dialectType, sqlContent, params.getDelimiter()));
        }
        List<String> sqlObjectIds = params.getSqlObjectIds();
        if (CollectionUtils.isEmpty(sqlObjectIds)) {
            return sqls;
        }
        long totalSize = 0;
        for (String objectId : sqlObjectIds) {
            ObjectMetadata metadata = storageFacade.loadMetaData(bucketName, objectId);
            totalSize += metadata.getTotalLength();
            if (maxSizeBytes > 0 && totalSize > maxSizeBytes) {
                log.info("The file size is too large and will not be read later, totalSize={} Byte", totalSize);
                return sqls;
            }
            sqlContent = storageFacade.loadObjectContentAsString(bucketName, objectId);
            if (sqlContent == null) {
                continue;
            }
            // TODO:全量文件加载到内存存在风险，需做内存优化处理
            sqls.addAll(SqlUtils.splitWithOffset(dialectType, sqlContent, params.getDelimiter()));
        }
        return sqls;
    }
}
