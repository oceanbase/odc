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
package com.oceanbase.odc.service.datasecurity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.datamasking.algorithm.Algorithm;
import com.oceanbase.odc.core.datamasking.algorithm.AlgorithmEnum;
import com.oceanbase.odc.core.datamasking.algorithm.AlgorithmFactory;
import com.oceanbase.odc.core.datamasking.data.Data;
import com.oceanbase.odc.core.datamasking.data.metadata.MetadataFactory;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.core.sql.execute.model.JdbcColumnMetaData;
import com.oceanbase.odc.core.sql.parser.AbstractSyntaxTreeFactories;
import com.oceanbase.odc.core.sql.parser.AbstractSyntaxTreeFactory;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.datasecurity.accessor.DatasourceColumnAccessor;
import com.oceanbase.odc.service.datasecurity.extractor.ColumnExtractor;
import com.oceanbase.odc.service.datasecurity.extractor.OBColumnExtractor;
import com.oceanbase.odc.service.datasecurity.extractor.model.DBColumn;
import com.oceanbase.odc.service.datasecurity.extractor.model.LogicalTable;
import com.oceanbase.odc.service.datasecurity.model.DataMaskingProperties;
import com.oceanbase.odc.service.datasecurity.model.MaskingAlgorithm;
import com.oceanbase.odc.service.datasecurity.model.SensitiveColumn;
import com.oceanbase.odc.service.datasecurity.util.DataMaskingUtil;
import com.oceanbase.odc.service.datasecurity.util.MaskingAlgorithmUtil;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.session.model.SqlExecuteResult;
import com.oceanbase.odc.service.task.base.databasechange.QuerySensitiveColumnReq;
import com.oceanbase.odc.service.task.base.databasechange.QuerySensitiveColumnResp;
import com.oceanbase.tools.sqlparser.statement.Statement;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @author gaoda.xy
 * @date 2023/6/14 10:51
 */
@Slf4j
@Service
@Validated
public class DataMaskingService {

    @Autowired
    private MaskingAlgorithmService algorithmService;

    @Autowired
    private SensitiveColumnService columnService;

    @Autowired
    private DataMaskingProperties maskingProperties;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    @SkipAuthorize("odc internal usages")
    public QuerySensitiveColumnResp querySensitiveColumn(@NotNull @Valid QuerySensitiveColumnReq req) {
        List<Set<SensitiveColumn>> sensitiveColumns =
                columnService.filterSensitiveColumns(req.getDataSourceId(), req.getTableRelatedDBColumns());
        QuerySensitiveColumnResp resp = new QuerySensitiveColumnResp();
        if (DataMaskingUtil.isSensitiveColumnExists(sensitiveColumns)) {
            resp.setContainsSensitiveColumn(true);
            resp.setMaskingAlgorithms(getResultSetMaskingAlgorithms(sensitiveColumns, req.getOrganizationId()));
        }
        return resp;
    }

    @SkipAuthorize("odc internal usages")
    public List<Set<SensitiveColumn>> getResultSetSensitiveColumns(@NotBlank String sql, ConnectionSession session) {
        List<Set<SensitiveColumn>> result = new ArrayList<>();
        Statement stmt;
        try {
            AbstractSyntaxTreeFactory factory = AbstractSyntaxTreeFactories.getAstFactory(session.getDialectType(), 0);
            if (factory == null) {
                throw new UnsupportedException("Unsupported dialect type: " + session.getDialectType());
            }
            stmt = factory.buildAst(sql).getStatement();
        } catch (Exception e) {
            log.warn("Parse sql failed, sql={}", sql, e);
            throw new IllegalStateException("Parse sql failed, details=" + e.getMessage());
        }
        LogicalTable table;
        try {
            DatasourceColumnAccessor accessor =
                    (DatasourceColumnAccessor) ConnectionSessionUtil.getColumnAccessor(session);
            ColumnExtractor extractor = new OBColumnExtractor(session.getDialectType(),
                    ConnectionSessionUtil.getCurrentSchema(session), accessor);
            table = extractor.extract(stmt);
        } catch (Exception e) {
            log.warn("Extract sensitive columns failed, stmt={}", stmt, e);
            return result;
        }
        if (Objects.isNull(table) || table.getColumnList().isEmpty()) {
            return result;
        }
        List<Set<DBColumn>> tableRelatedDBColumns = table.getTableRelatedDBColumns();
        Long datasourceId = ((ConnectionConfig) ConnectionSessionUtil.getConnectionConfig(session)).getId();
        return columnService.filterSensitiveColumns(datasourceId, tableRelatedDBColumns);
    }

    @SkipAuthorize("odc internal usages")
    public List<MaskingAlgorithm> getResultSetMaskingAlgorithms(@NonNull List<Set<SensitiveColumn>> columnsList,
            @NonNull Long organizationId) {
        List<MaskingAlgorithm> result = new ArrayList<>();
        Long defaultAlgorithmId = algorithmService.getDefaultAlgorithmIdByOrganizationId(organizationId);
        Map<Long, MaskingAlgorithm> id2Algorithm = getId2MaskingAlgorithmByOrganizationId(organizationId);
        for (Set<SensitiveColumn> columns : columnsList) {
            if (columns.isEmpty()) {
                result.add(null);
            } else if (columns.size() == 1) {
                Long algorithmId = columns.iterator().next().getMaskingAlgorithmId();
                result.add(id2Algorithm.get(algorithmId));
            } else {
                // If the masking algorithm cannot be uniquely determined, the system default is used.
                result.add(id2Algorithm.get(defaultAlgorithmId));
            }
        }
        return result;
    }

    @SkipAuthorize("odc internal usages")
    public List<Algorithm> getResultSetMaskingAlgorithmMaskers(@NonNull List<Set<SensitiveColumn>> columnsList) {
        List<Algorithm> results = new ArrayList<>();
        List<MaskingAlgorithm> maskingAlgorithms =
                getResultSetMaskingAlgorithms(columnsList, authenticationFacade.currentOrganizationId());
        for (MaskingAlgorithm a : maskingAlgorithms) {
            if (Objects.nonNull(a)) {
                Algorithm algorithmMasker = AlgorithmFactory.createAlgorithm(AlgorithmEnum.valueOf(a.getType().name()),
                        MaskingAlgorithmUtil.toAlgorithmParameters(a));
                results.add(algorithmMasker);
            } else {
                results.add(null);
            }
        }
        return results;
    }

    @SkipAuthorize("odc internal usages")
    public void maskRowsUsingAlgorithms(@NotNull SqlExecuteResult result, @NotEmpty List<Algorithm> algorithms) {
        List<String> columnLabels = result.getColumnLabels();
        List<List<Object>> rows = result.getRows();
        List<JdbcColumnMetaData> fieldMetaDataList = result.getResultSetMetaData().getFieldMetaDataList();
        int columnCount = rows.get(0).size();
        Verify.equals(columnCount, algorithms.size(), "algorithms.size");
        String dataType = "string";
        int totalCount = 0;
        int skippedCount = 0;
        int failedCount = 0;
        Map<String, Integer> failedColumn2FirstRow = new HashMap<>();
        for (int i = 0; i < columnCount; i++) {
            Algorithm algorithm = algorithms.get(i);
            if (Objects.isNull(algorithm)) {
                totalCount += rows.size();
                skippedCount += rows.size();
                continue;
            }
            if (algorithm.getType() == AlgorithmEnum.ROUNDING) {
                dataType = "double";
            }
            Data before = Data.of(null, MetadataFactory.createMetadata(null, dataType));
            for (int j = 0; j < rows.size(); j++) {
                List<Object> rowData = rows.get(j);
                totalCount++;
                if (rowData.get(i) == null) {
                    skippedCount++;
                    continue;
                }
                before.setValue(rowData.get(i).toString());
                Data masked;
                try {
                    masked = algorithm.mask(before);
                } catch (Exception e) {
                    // Eat exception
                    failedCount++;
                    failedColumn2FirstRow.putIfAbsent(columnLabels.get(i), j);
                    continue;
                }
                rowData.set(i, masked.getValue());
            }
            fieldMetaDataList.get(i).setMasked(true);
        }
        log.info("Data masking finished, total: {}, skipped: {}, failed: {}.", totalCount, skippedCount, failedCount);
        if (failedCount > 0) {
            String msg = failedColumn2FirstRow.entrySet().stream()
                    .map(e -> String.format("columnLabel: %s, columnIndex: %d", e.getKey(), e.getValue()))
                    .collect(Collectors.joining("; "));
            log.warn("Exception happened during data masking, position details: {}", msg);
        }
    }

    @SkipAuthorize("odc internal usages")
    public Map<SensitiveColumn, MaskingAlgorithm> listColumnsAndMaskingAlgorithm(Long databaseId,
            Collection<String> tableNames) {
        Map<SensitiveColumn, MaskingAlgorithm> result = new HashMap<>();
        List<SensitiveColumn> columns =
                columnService.listByDatabaseAndTable(Collections.singleton(databaseId), tableNames);
        Map<Long, MaskingAlgorithm> id2Algorithm =
                getId2MaskingAlgorithmByOrganizationId(authenticationFacade.currentOrganizationId());
        columns.forEach(column -> result.put(column, id2Algorithm.get(column.getMaskingAlgorithmId())));
        return result;
    }

    @SkipAuthorize("odc internal usages")
    public boolean isMaskingEnabled() {
        return maskingProperties.isMaskingEnabled()
                && columnService.existsInCurrentOrganization();
    }

    private Map<Long, MaskingAlgorithm> getId2MaskingAlgorithmByOrganizationId(@NonNull Long organizationId) {
        List<MaskingAlgorithm> algorithms = algorithmService.getMaskingAlgorithmsByOrganizationId(organizationId);
        if (CollectionUtils.isEmpty(algorithms)) {
            return new HashMap<>();
        }
        return algorithms.stream().collect(Collectors.toMap(MaskingAlgorithm::getId, e -> e, (e1, e2) -> e1));
    }

}
