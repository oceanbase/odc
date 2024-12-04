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
package com.oceanbase.tools.sqlparser.statement.createtable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.common.BaseOptions;
import com.oceanbase.tools.sqlparser.statement.common.RelationFactor;
import com.oceanbase.tools.sqlparser.statement.common.mysql.LobStorageOption;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

/**
 * {@link TableOptions}
 *
 * @author yh263208
 * @date 2023-05-29 14:35
 * @since ODC_release_4.2.0
 * @see BaseStatement
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class TableOptions extends BaseOptions {

    private List<ColumnReference> sortKeys;
    private Integer parallel;
    private Boolean noParallel;
    private String tableMode;
    private String duplicateScope;
    private String locality;
    private Expression expireInfo;
    private Integer progressiveMergeNum;
    private Integer blockSize;
    private Integer tableId;
    private Integer replicaNum;
    private Boolean noCompress;
    private String compress;
    private Boolean useBloomFilter;
    private String primaryZone;
    private String tableGroup;
    private Boolean readOnly;
    private Boolean readWrite;
    private String engine;
    private Integer tabletSize;
    private Integer maxUsedPartId;
    private Boolean enableRowMovement;
    private Boolean disableRowMovement;
    private Integer pctFree;
    private Integer pctUsed;
    private Integer iniTrans;
    private Integer maxTrans;
    private List<String> storage;
    private String tableSpace;
    private String comment;
    private String compression;
    private Integer storageFormatVersion;
    private String rowFormat;
    private String charset;
    private String collation;
    private BigDecimal autoIncrement;
    private Integer delayKeyWrite;
    private Integer avgRowLength;
    private Integer checksum;
    private String autoIncrementMode;
    private Boolean enableExtendedRowId;
    private String location;
    private Map<String, Expression> format;
    private String pattern;
    private List<Expression> ttls;
    private String kvAttributes;
    private Integer defaultLobInRowThreshold;
    private Integer lobInRowThreshold;
    private Integer keyBlockSize;
    private Integer autoIncrementCacheSize;
    private String partitionType;
    private Map<String, String> externalProperties;
    private LobStorageOption lobStorageOption;
    private Boolean microIndexClustered;
    private String autoRefresh;
    private Integer maxRows;
    private Integer minRows;
    private String password;
    private String packKeys;
    private String connection;
    private String dataDirectory;
    private String indexDirectory;
    private String encryption;
    private String statsAutoRecalc;
    private String statsPersistent;
    private String statsSamplePages;
    private List<RelationFactor> union;
    private String insertMethod;

    public TableOptions(@NonNull ParserRuleContext context) {
        super(context);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (this.sortKeys != null) {
            builder.append(" SORTKEY(").append(this.sortKeys.stream()
                    .map(ColumnReference::toString).collect(Collectors.joining(",")))
                    .append(")");
        }
        if (this.parallel != null) {
            builder.append(" PARALLEL=").append(this.parallel);
        }
        if (Boolean.TRUE.equals(this.noParallel)) {
            builder.append(" NOPARALLEL");
        }
        if (this.tableMode != null) {
            builder.append(" TABLE_MODE=").append(this.tableMode);
        }
        if (this.duplicateScope != null) {
            builder.append(" DUPLICATE_SCOPE=").append(this.duplicateScope);
        }
        if (this.locality != null) {
            builder.append(" LOCALITY=").append(this.locality);
        }
        if (this.expireInfo != null) {
            builder.append(" EXPIRE_INFO=(").append(this.expireInfo).append(")");
        }
        if (this.progressiveMergeNum != null) {
            builder.append(" PROGRESSIVE_MERGE_NUM=").append(this.progressiveMergeNum);
        }
        if (this.blockSize != null) {
            builder.append(" BLOCK_SIZE=").append(this.blockSize);
        }
        if (this.tableId != null) {
            builder.append(" TABLE_ID=").append(this.tableId);
        }
        if (this.replicaNum != null) {
            builder.append(" REPLICA_NUM=").append(this.replicaNum);
        }
        if (Boolean.TRUE.equals(this.noCompress)) {
            builder.append(" NOCOMPRESS");
        }
        if (this.compress != null) {
            builder.append(" COMPRESS ").append(this.compress);
        }
        if (this.useBloomFilter != null) {
            builder.append(" USE_BLOOM_FILTER=").append(this.useBloomFilter ? "TRUE" : "FALSE");
        }
        if (this.primaryZone != null) {
            builder.append(" PRIMARY_ZONE=").append(this.primaryZone);
        }
        if (this.tableGroup != null) {
            builder.append(" TABLEGROUP=").append(this.tableGroup);
        }
        if (Boolean.TRUE.equals(this.readOnly)) {
            builder.append(" READ ONLY");
        }
        if (Boolean.TRUE.equals(this.readWrite)) {
            builder.append(" READ WRITE");
        }
        if (this.engine != null) {
            builder.append(" ENGINE=").append(this.engine);
        }
        if (this.tabletSize != null) {
            builder.append(" TABLET_SIZE=").append(this.tabletSize);
        }
        if (this.maxUsedPartId != null) {
            builder.append(" MAX_USED_PART_ID=").append(this.maxUsedPartId);
        }
        if (Boolean.TRUE.equals(this.enableRowMovement)) {
            builder.append(" ENABLE ROW MOVEMENT");
        }
        if (Boolean.TRUE.equals(this.disableRowMovement)) {
            builder.append(" DISABLE ROW MOVEMENT");
        }
        if (this.pctFree != null) {
            builder.append(" PCTFREE=").append(this.pctFree);
        }
        if (this.pctUsed != null) {
            builder.append(" PCTUSED ").append(this.pctUsed);
        }
        if (this.iniTrans != null) {
            builder.append(" INITRANS ").append(this.iniTrans);
        }
        if (this.maxTrans != null) {
            builder.append(" MAXTRANS ").append(this.maxTrans);
        }
        if (this.storage != null) {
            builder.append(" STORAGE(").append(String.join(" ", this.storage)).append(")");
        }
        if (this.tableSpace != null) {
            builder.append(" TABLESPACE ").append(this.tableSpace);
        }
        if (this.comment != null) {
            builder.append(" COMMENT ").append(this.comment);
        }
        if (this.compression != null) {
            builder.append(" COMPRESSION=").append(this.compression);
        }
        if (this.storageFormatVersion != null) {
            builder.append(" STORAGE_FORMAT_VERSION=").append(this.storageFormatVersion);
        }
        if (this.rowFormat != null) {
            builder.append(" ROW_FORMAT=").append(this.rowFormat);
        }
        if (this.charset != null) {
            builder.append(" CHARSET=").append(this.charset);
        }
        if (this.collation != null) {
            builder.append(" COLLATE=").append(this.collation);
        }
        if (this.autoIncrement != null) {
            builder.append(" AUTO_INCREMENT=").append(this.autoIncrement.toString());
        }
        if (this.delayKeyWrite != null) {
            builder.append(" DELAY_KEY_WRITE=").append(this.delayKeyWrite);
        }
        if (this.avgRowLength != null) {
            builder.append(" AVG_ROW_LENGTH=").append(this.avgRowLength);
        }
        if (this.checksum != null) {
            builder.append(" CHECKSUM=").append(this.checksum);
        }
        if (this.autoIncrementMode != null) {
            builder.append(" AUTO_INCREMENT_MODE=").append(this.autoIncrementMode);
        }
        if (this.enableExtendedRowId != null) {
            builder.append(" ENABLE_EXTENDED_ROWID=").append(this.enableExtendedRowId ? "TRUE" : "FALSE");
        }
        if (this.location != null) {
            builder.append(" LOCATION=").append(this.location);
        }
        if (this.format != null) {
            builder.append(" FORMAT=(")
                    .append(this.format.entrySet().stream()
                            .map(e -> e.getKey() + "=" + e.getValue().toString())
                            .collect(Collectors.joining(",")))
                    .append(")");
        }
        if (this.pattern != null) {
            builder.append(" PATTERN=").append(this.pattern);
        }
        if (CollectionUtils.isNotEmpty(this.ttls)) {
            builder.append(" TTL(").append(this.ttls.stream().map(Object::toString)
                    .collect(Collectors.joining(","))).append(")");
        }
        if (this.kvAttributes != null) {
            builder.append(" KV_ATTRIBUTES=").append(this.kvAttributes);
        }
        if (this.defaultLobInRowThreshold != null) {
            builder.append(" DEFAULT_LOB_INROW_THRESHOLD=").append(this.defaultLobInRowThreshold);
        }
        if (this.lobInRowThreshold != null) {
            builder.append(" LOB_INROW_THRESHOLD=").append(this.lobInRowThreshold);
        }
        if (this.keyBlockSize != null) {
            builder.append(" KEY_BLOCK_SIZE=").append(this.keyBlockSize);
        }
        if (this.autoIncrementCacheSize != null) {
            builder.append(" AUTO_INCREMENT_CACHE_SIZE=").append(this.autoIncrementCacheSize);
        }
        if (this.partitionType != null) {
            builder.append(" PARTITION_TYPE=").append(this.partitionType);
        }
        if (this.externalProperties != null) {
            builder.append(" PROPERTIES=(")
                    .append(this.externalProperties.entrySet().stream()
                            .map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining(",")))
                    .append(")");
        }
        if (this.lobStorageOption != null) {
            builder.append(" ").append(this.lobStorageOption);
        }
        if (this.microIndexClustered != null) {
            builder.append(" MICRO_INDEX_CLUSTERED=").append(
                    Boolean.TRUE.equals(this.microIndexClustered) ? "TRUE" : "FALSE");
        }
        if (this.autoRefresh != null) {
            builder.append(" AUTO_REFRESH=").append(this.autoRefresh);
        }
        if (this.maxRows != null) {
            builder.append(" MAX_ROWS=").append(this.maxRows);
        }
        if (this.minRows != null) {
            builder.append(" MIN_ROWS=").append(this.minRows);
        }
        if (this.password != null) {
            builder.append(" PASSWORD=").append(this.password);
        }
        if (this.packKeys != null) {
            builder.append(" PACK_KEYS=").append(this.packKeys);
        }
        if (this.connection != null) {
            builder.append(" CONNECTION=").append(this.connection);
        }
        if (this.dataDirectory != null) {
            builder.append(" DATA DIRECTORY=").append(this.dataDirectory);
        }
        if (this.indexDirectory != null) {
            builder.append(" INDEX DIRECTORY=").append(this.indexDirectory);
        }
        if (this.encryption != null) {
            builder.append(" ENCRYPTION=").append(this.encryption);
        }
        if (this.statsAutoRecalc != null) {
            builder.append(" STATS_AUTO_RECALC=").append(this.statsAutoRecalc);
        }
        if (this.statsPersistent != null) {
            builder.append(" STATS_PERSISTENT=").append(this.statsPersistent);
        }
        if (this.statsSamplePages != null) {
            builder.append(" STATS_SAMPLE_PAGES=").append(this.statsSamplePages);
        }
        if (this.union != null) {
            builder.append(" UNION=(").append(this.union.stream()
                    .map(RelationFactor::toString).collect(Collectors.joining(", "))).append(")");
        }
        if (this.insertMethod != null) {
            builder.append(" INSERT_METHOD=").append(this.insertMethod);
        }
        return builder.length() == 0 ? "" : builder.substring(1);
    }

}
