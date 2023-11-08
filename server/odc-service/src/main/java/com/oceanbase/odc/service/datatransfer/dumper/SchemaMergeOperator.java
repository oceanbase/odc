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
package com.oceanbase.odc.service.datatransfer.dumper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.oceanbase.odc.common.util.ListUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.common.util.TopoOrderComparator;
import com.oceanbase.odc.plugin.task.api.datatransfer.dumper.DumpDBObject;
import com.oceanbase.odc.plugin.task.api.datatransfer.dumper.DumperOutput;
import com.oceanbase.odc.plugin.task.api.datatransfer.dumper.SchemaFile;
import com.oceanbase.tools.dbbrowser.model.DBTableConstraint;
import com.oceanbase.tools.dbbrowser.parser.SqlParser;
import com.oceanbase.tools.dbbrowser.parser.result.ParseSqlResult;
import com.oceanbase.tools.loaddump.common.enums.ObjectType;
import com.oceanbase.tools.loaddump.common.enums.ServerMode;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link SchemaMergeOperator}
 *
 * @author liuyizhuo.lyz
 * @date 2023-01-30
 */

@Slf4j
public class SchemaMergeOperator {
    public static final String SCHEMA_FILE = "schema.sql";
    // @formatter:off
    /**
     * The DDL sequence in schema.sql is in accordance with {@code PROCESS_ORDER} specified.
     * schema.sql may be like:
     * -- --------------------------
     * -- TABLE structure for _TEST_T1
     * -- --------------------------
     * DROP TABLE IF EXISTS `_test_t1`;
     * CREATE TABLE `_test_t1` (
     *   `val` varchar(20) DEFAULT NULL
     * );
     * ...
     */
    // @formatter:on 
    private static final List<ObjectType> PROCESS_ORDER;
    private static final String COMMENT_PREFIX = "-- ";
    private static final char LINE_BREAKER = '\n';
    private static final String DOUBLE_LINE_BREAKER = "\n\n";
    private static final String COMMENT_SEPERATOR = "-- --------------------------\n";
    private static final String STRUCTURE_FOR = " structure for ";
    private static final String BLANK_SPACE = " ";
    private static final String UNDERLINE = "_";

    private final ServerMode mode;
    private final String schemaName;
    private final DumperOutput dumperOutput;

    private final Map<ObjectType, List<SchemaFileIdentifier>> objectMap;

    static {
        PROCESS_ORDER = new ArrayList<>(16);
        PROCESS_ORDER.add(ObjectType.TABLE);
        PROCESS_ORDER.add(ObjectType.TABLE_GROUP);
        PROCESS_ORDER.add(ObjectType.VIEW);
        PROCESS_ORDER.add(ObjectType.TRIGGER);
        PROCESS_ORDER.add(ObjectType.SEQUENCE);
        PROCESS_ORDER.add(ObjectType.SYNONYM);
        PROCESS_ORDER.add(ObjectType.PUBLIC_SYNONYM);
        PROCESS_ORDER.add(ObjectType.FUNCTION);
        PROCESS_ORDER.add(ObjectType.PROCEDURE);
        PROCESS_ORDER.add(ObjectType.TYPE);
        PROCESS_ORDER.add(ObjectType.TYPE_BODY);
        PROCESS_ORDER.add(ObjectType.PACKAGE);
        PROCESS_ORDER.add(ObjectType.PACKAGE_BODY);
    }

    public SchemaMergeOperator(DumperOutput dumperOutput, String schemaName, ServerMode mode) throws Exception {
        this.mode = mode;
        this.dumperOutput = dumperOutput;
        this.schemaName = mode.isMysqlMode() ? StringUtils.unquoteMySqlIdentifier(schemaName)
                : StringUtils.unquoteOracleIdentifier(schemaName);
        this.objectMap = getSchemaFileIdentifiers();
    }

    public void mergeSchemaFiles(File dest, Predicate<String> predicate) throws IOException {
        if (dest.exists()) {
            FileUtils.forceDelete(dest);
        }
        try (FileWriter fileWriter = new FileWriter(dest, true);
                BufferedWriter writer = new BufferedWriter(fileWriter)) {
            for (ObjectType type : PROCESS_ORDER) {
                if (!objectMap.containsKey(type)) {
                    continue;
                }
                if (type.equals(ObjectType.TABLE)) {
                    new TableProcessor(schemaName, mode).process(objectMap.get(type));
                }

                for (SchemaFileIdentifier identifier : objectMap.get(type)) {
                    if (predicate == null || predicate.test(identifier.getTarget().getFileName())) {
                        doMerge(identifier, writer);
                    }
                }
            }
        } catch (IOException ex) {
            FileUtils.deleteQuietly(dest);
            throw ex;
        }
    }

    private Map<ObjectType, List<SchemaFileIdentifier>> getSchemaFileIdentifiers() throws IOException {
        Map<ObjectType, List<SchemaFileIdentifier>> identifiers = new HashMap<>();
        for (DumpDBObject object : dumperOutput.getDumpDbObjects()) {
            if (CollectionUtils.isEmpty(object.getOutputFiles())) {
                continue;
            }
            ObjectType type = object.getObjectType();
            object.getOutputFiles().stream().filter(o -> o instanceof SchemaFile).forEach(o -> {
                SchemaFileIdentifier identifier =
                        new SchemaFileIdentifier(schemaName, o.getObjectName());
                identifier.setTarget((SchemaFile) o);
                identifiers.computeIfAbsent(type, t -> new ArrayList<>()).add(identifier);
            });
        }
        return identifiers;
    }

    private void doMerge(SchemaFileIdentifier identifier, Writer writer) throws IOException {
        URL url = identifier.getTarget().getUrl();
        String title = COMMENT_SEPERATOR
                + COMMENT_PREFIX
                + identifier.getTarget().getObjectType().toString() + STRUCTURE_FOR + identifier.getObjectName()
                + LINE_BREAKER
                + COMMENT_SEPERATOR;
        writer.write(title);
        try (InputStream in = url.openStream()) {
            IOUtils.copy(in, writer, StandardCharsets.UTF_8);
        }
        writer.write(DOUBLE_LINE_BREAKER);
        writer.flush();
    }

    @Getter
    @Setter
    @EqualsAndHashCode(of = {"schemaName", "objectName"})
    private static class SchemaFileIdentifier {
        private final String schemaName;
        private final String objectName;
        private Set<SchemaFileIdentifier> refTables;
        private SchemaFile target;
        private boolean deleted;

        public SchemaFileIdentifier(String schemaName, String objectName) {
            this.schemaName = schemaName;
            this.objectName = objectName;
        }
    }

    private static class TableProcessor {
        private final String schemaName;
        private final ServerMode mode;

        public TableProcessor(String schemaName, ServerMode mode) {
            this.schemaName = schemaName;
            this.mode = mode;
        }

        public void process(List<SchemaFileIdentifier> tables) throws IOException {
            if (CollectionUtils.isEmpty(tables)) {
                return;
            }
            TopoOrderComparator<SchemaFileIdentifier> comparator = new TopoOrderComparator<>();
            for (SchemaFileIdentifier tableIdentifier : tables) {
                SchemaFile file = tableIdentifier.getTarget();
                /** load */
                String content;
                try (InputStream stream = file.getUrl().openStream();
                        InputStreamReader reader = new InputStreamReader(stream)) {
                    content = IOUtils.toString(reader);
                }
                /** match */
                try {
                    findReferenceTables(tableIdentifier, content);
                } catch (Exception e) {
                    log.info("Failed to parse table reference, reason: {}", e.getMessage());
                }
                comparator.addAll(tableIdentifier, tableIdentifier.getRefTables());
            }
            /** sort */
            ListUtils.sortByTopoOrder(tables, (o1, o2) -> -comparator.compare(o1, o2));
        }

        private void findReferenceTables(SchemaFileIdentifier tableIdentifier, String content) {
            ParseSqlResult result;
            if (mode.isMysqlMode()) {
                result = SqlParser.parseMysql(content);
            } else {
                result = SqlParser.parseOracle(content);
            }
            if (CollectionUtils.isNotEmpty(result.getForeignConstraint())) {
                for (DBTableConstraint constraint : result.getForeignConstraint()) {
                    String schema = constraint.getReferenceSchemaName() == null ? schemaName
                            : constraint.getReferenceSchemaName();
                    String table = constraint.getReferenceTableName();
                    SchemaFileIdentifier refTable = new SchemaFileIdentifier(schema, table);
                    if (tableIdentifier.getRefTables() == null) {
                        tableIdentifier.setRefTables(new HashSet<>());
                    }
                    tableIdentifier.getRefTables().add(refTable);
                }
            }
        }
    }

}
