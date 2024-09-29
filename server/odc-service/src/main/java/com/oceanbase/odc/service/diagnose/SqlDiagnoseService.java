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
package com.oceanbase.odc.service.diagnose;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.StatementCallback;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.core.shared.model.SqlExecDetail;
import com.oceanbase.odc.core.shared.model.TraceSpan;
import com.oceanbase.odc.core.sql.execute.cache.model.BinaryContentMetaData;
import com.oceanbase.odc.plugin.connect.model.diagnose.SqlExplain;
import com.oceanbase.odc.service.common.model.ResourceSql;
import com.oceanbase.odc.service.diagnose.fulllinktrace.JaegerConverter;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.objectstorage.ObjectStorageFacade;
import com.oceanbase.odc.service.objectstorage.model.ObjectMetadata;
import com.oceanbase.odc.service.plugin.ConnectionPluginUtil;
import com.oceanbase.odc.service.queryprofile.OBQueryProfileManager;

@Service
@SkipAuthorize("inside connect session")
public class SqlDiagnoseService {
    @Autowired
    private ObjectStorageFacade objectStorageFacade;
    @Autowired
    private AuthenticationFacade authenticationFacade;
    @Autowired
    private OBQueryProfileManager profileManager;

    public SqlExplain explain(ConnectionSession session, ResourceSql odcSql) {
        return session.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY)
                .execute((StatementCallback<SqlExplain>) stmt -> ConnectionPluginUtil
                        .getDiagnoseExtension(session.getDialectType()).getExplain(stmt, odcSql.getSql()));
    }

    public SqlExplain getPhysicalPlan(ConnectionSession session, ResourceSql odcSql) {
        return StringUtils.isBlank(odcSql.getTag())
                ? session.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY)
                        .execute((ConnectionCallback<SqlExplain>) con -> ConnectionPluginUtil
                                .getDiagnoseExtension(session.getDialectType()).getPhysicalPlanBySql(con,
                                        odcSql.getSql()))
                : session.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY)
                        .execute((ConnectionCallback<SqlExplain>) con -> ConnectionPluginUtil
                                .getDiagnoseExtension(session.getDialectType()).getPhysicalPlanBySqlId(con,
                                        odcSql.getTag()));
    }

    /**
     * 获取SQL执行详情
     *
     * @param session 数据库会话
     * @param odcSql  ODC SQL对象
     * @return SQL执行详情
     */
    public SqlExecDetail getExecutionDetail(ConnectionSession session, ResourceSql odcSql) {
        // 如果ODC SQL对象的标签为空，则通过SQL语句获取执行详情，否则通过标签获取执行详情
        return StringUtils.isBlank(odcSql.getTag())
            ? session.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY)
            .execute((ConnectionCallback<SqlExecDetail>) con -> ConnectionPluginUtil
                .getDiagnoseExtension(session.getDialectType()).getExecutionDetailBySql(con,
                    odcSql.getSql()))
            : session.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY)
                .execute((ConnectionCallback<SqlExecDetail>) con -> ConnectionPluginUtil
                    .getDiagnoseExtension(session.getDialectType()).getExecutionDetailById(con,
                        odcSql.getTag()));
    }

    /**
     * 获取全链路跟踪信息
     *
     * @param session 数据库连接会话
     * @param odcSql  ODC SQL对象
     * @return 全链路跟踪信息
     * @throws IOException IO异常
     */
    public TraceSpan getFullLinkTrace(ConnectionSession session, ResourceSql odcSql) throws IOException {
        // 从ODC SQL对象中获取traceId
        String traceId = odcSql.getTag();
        // 通过traceId获取二进制元数据
        BinaryContentMetaData metaData = ConnectionSessionUtil.getBinaryContentMetadata(session, traceId);
        // 如果元数据为空，则抛出NotFoundException异常
        if (metaData == null) {
            throw new NotFoundException(ErrorCodes.NotFound, new Object[] {"Trace info", "traceId", traceId},
                "Trace info not found with traceId=" + traceId);
        }
        // 通过二进制数据管理器读取流
        InputStream stream = ConnectionSessionUtil.getBinaryDataManager(session).read(metaData);
        // 将流转换为TraceSpan对象并返回
        return JsonUtils.fromJson(StreamUtils.copyToString(stream, StandardCharsets.UTF_8), TraceSpan.class);
    }

    public String getFullLinkTraceDownloadUrl(ConnectionSession session, ResourceSql odcSql) throws IOException {
        TraceSpan traceSpan = getFullLinkTrace(session, odcSql);
        String json = new JaegerConverter().convert(traceSpan);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(json.getBytes());

        String bucketName = "trace".concat(File.separator).concat(authenticationFacade.currentUserIdStr());
        objectStorageFacade.createBucketIfNotExists(bucketName);
        ObjectMetadata metadata = objectStorageFacade.putTempObject(bucketName, odcSql.getTag(),
                inputStream.available(), inputStream);
        return objectStorageFacade.getDownloadUrl(metadata.getBucketName(), metadata.getObjectId());
    }

    public SqlExplain getQueryProfile(ConnectionSession session, String traceId) throws IOException {
        return profileManager.getProfile(traceId, session);
    }

}
