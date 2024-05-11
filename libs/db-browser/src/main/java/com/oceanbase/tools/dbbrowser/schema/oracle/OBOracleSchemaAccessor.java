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
package com.oceanbase.tools.dbbrowser.schema.oracle;

import java.io.StringReader;
import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.RowMapper;

import com.oceanbase.tools.dbbrowser.model.DBColumnGroupElement;
import com.oceanbase.tools.dbbrowser.model.DBColumnTypeDisplay;
import com.oceanbase.tools.dbbrowser.model.DBDatabase;
import com.oceanbase.tools.dbbrowser.model.DBFunction;
import com.oceanbase.tools.dbbrowser.model.DBIndexAlgorithm;
import com.oceanbase.tools.dbbrowser.model.DBIndexType;
import com.oceanbase.tools.dbbrowser.model.DBObjectIdentity;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.model.DBPLObjectIdentity;
import com.oceanbase.tools.dbbrowser.model.DBPLParam;
import com.oceanbase.tools.dbbrowser.model.DBPackage;
import com.oceanbase.tools.dbbrowser.model.DBPackageBasicInfo;
import com.oceanbase.tools.dbbrowser.model.DBPackageDetail;
import com.oceanbase.tools.dbbrowser.model.DBProcedure;
import com.oceanbase.tools.dbbrowser.model.DBSequence;
import com.oceanbase.tools.dbbrowser.model.DBSynonym;
import com.oceanbase.tools.dbbrowser.model.DBSynonymType;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.DBTable.DBTableOptions;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn.CharUnit;
import com.oceanbase.tools.dbbrowser.model.DBTableConstraint;
import com.oceanbase.tools.dbbrowser.model.DBTableIndex;
import com.oceanbase.tools.dbbrowser.model.DBTrigger;
import com.oceanbase.tools.dbbrowser.model.DBType;
import com.oceanbase.tools.dbbrowser.model.DBTypeCode;
import com.oceanbase.tools.dbbrowser.model.DBVariable;
import com.oceanbase.tools.dbbrowser.model.DBView;
import com.oceanbase.tools.dbbrowser.model.OracleConstants;
import com.oceanbase.tools.dbbrowser.model.PLConstants;
import com.oceanbase.tools.dbbrowser.model.datatype.DataTypeUtil;
import com.oceanbase.tools.dbbrowser.parser.PLParser;
import com.oceanbase.tools.dbbrowser.parser.SqlParser;
import com.oceanbase.tools.dbbrowser.parser.result.ParseOraclePLResult;
import com.oceanbase.tools.dbbrowser.parser.result.ParseSqlResult;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessorSqlMappers;
import com.oceanbase.tools.dbbrowser.schema.constant.StatementsFiles;
import com.oceanbase.tools.dbbrowser.util.DBSchemaAccessorUtil;
import com.oceanbase.tools.dbbrowser.util.OracleDataDictTableNames;
import com.oceanbase.tools.dbbrowser.util.OracleSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.PLObjectErrMsgUtils;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;
import com.oceanbase.tools.dbbrowser.util.StringUtils;
import com.oceanbase.tools.sqlparser.OBOracleSQLParser;
import com.oceanbase.tools.sqlparser.SQLParser;
import com.oceanbase.tools.sqlparser.statement.createtable.CreateTable;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * 适用于的 DB 版本：[4.0.0, ~)
 * 
 * @author jingtian
 */
@Slf4j
public class OBOracleSchemaAccessor extends OracleSchemaAccessor {
    private static final Set<String> ESCAPE_USER_SET = new HashSet<>(4);

    static {
        ESCAPE_USER_SET.add("PUBLIC");
        ESCAPE_USER_SET.add("LBACSYS");
        ESCAPE_USER_SET.add("ORAAUDITOR");
        ESCAPE_USER_SET.add("__public");
    }

    public OBOracleSchemaAccessor(JdbcOperations jdbcOperations,
            OracleDataDictTableNames dataDictTableNames) {
        super(jdbcOperations, dataDictTableNames);
        this.sqlMapper = DBSchemaAccessorSqlMappers.get(StatementsFiles.OBORACLE_4_0_x);
    }

    @Override
    public List<String> showDatabases() {
        return super.showDatabases().stream().filter(user -> !ESCAPE_USER_SET.contains(user))
                .collect(Collectors.toList());
    }

    @Override
    public List<DBDatabase> listDatabases() {
        return super.listDatabases().stream().filter(user -> !ESCAPE_USER_SET.contains(user))
                .collect(Collectors.toList());
    }

    @Override
    public List<String> showCharset() {
        OracleSqlBuilder sb = new OracleSqlBuilder();
        sb.append("show character set");

        return jdbcOperations.query(sb.toString(), (rs, rowNum) -> rs.getString(1));
    }

    @Override
    public List<String> showCollation() {
        OracleSqlBuilder sb = new OracleSqlBuilder();
        sb.append("show collation");

        return jdbcOperations.query(sb.toString(), (rs, rowNum) -> rs.getString(1));
    }

    @Override
    public List<DBVariable> showVariables() {
        String sql = "show variables";

        return jdbcOperations.query(sql, (rs, rowNum) -> {
            DBVariable variable = new DBVariable();
            variable.setName(rs.getString(1));
            variable.setValue(rs.getString(2));
            return variable;
        });
    }

    @Override
    public List<DBVariable> showSessionVariables() {
        String sql = "show session variables";

        return jdbcOperations.query(sql, (rs, rowNum) -> {
            DBVariable variable = new DBVariable();
            variable.setName(rs.getString(1));
            variable.setValue(rs.getString(2));
            return variable;
        });
    }

    @Override
    public List<DBVariable> showGlobalVariables() {
        String sql = "show global variables";

        return jdbcOperations.query(sql, (rs, rowNum) -> {
            DBVariable variable = new DBVariable();
            variable.setName(rs.getString(1));
            variable.setValue(rs.getString(2));
            return variable;
        });
    }

    @Override
    public List<DBTableIndex> listTableIndexes(String schemaName, String tableName) {
        List<DBTableIndex> indexList = super.listTableIndexes(schemaName, tableName);
        fillIndexInfo(indexList);
        fillIndexTypeAndAlgorithm(indexList);
        return indexList;
    }

    protected void fillIndexTypeAndAlgorithm(List<DBTableIndex> indexList) {
        for (DBTableIndex index : indexList) {
            if (index.getType() == DBIndexType.UNKNOWN) {
                if (index.isNonUnique()) {
                    index.setType(DBIndexType.NORMAL);
                } else {
                    index.setType(DBIndexType.UNIQUE);
                }
            }
            if (index.getAlgorithm() == DBIndexAlgorithm.UNKNOWN) {
                index.setAlgorithm(DBIndexAlgorithm.BTREE);
            }
        }
    }

    @Override
    public Map<String, List<DBTableIndex>> listTableIndexes(String schemaName) {
        Map<String, List<DBTableIndex>> tableName2Indexes = super.listTableIndexes(schemaName);
        List<DBTableIndex> indexList =
                tableName2Indexes.values().stream().flatMap(List::stream).collect(Collectors.toList());
        fillIndexInfo(indexList);
        fillIndexTypeAndAlgorithm(indexList);
        return tableName2Indexes;
    }

    @Override
    protected boolean isTableIndexAvailable(String status) {
        return "VALID".equals(status);
    }

    @Override
    protected boolean judgeIndexGlobalOrLocalFromDataDict() {
        return false;
    }

    protected void fillIndexInfo(List<DBTableIndex> indexList) {
        for (DBTableIndex index : indexList) {
            try {
                OracleSqlBuilder sb = new OracleSqlBuilder();
                sb.append("SELECT dbms_metadata.get_ddl('INDEX', ")
                        .value(index.getName())
                        .append(", ")
                        .value(index.getOwner())
                        .append(") DDL from dual");
                jdbcOperations.query(sb.toString(), (rs, num) -> {
                    String indexDdl = rs.getString("DDL");
                    ParseSqlResult result = SqlParser.parseOracle(indexDdl);
                    if (CollectionUtils.isEmpty(result.getIndexes())) {
                        DBSchemaAccessorUtil.fillWarning(index, index.type(), "parse index DDL failed");
                        index.setGlobal(true);
                    } else {
                        if (index.getType() != DBIndexType.UNIQUE) {
                            index.setDdl(indexDdl);
                        }
                        // we get one single create index statement for each table index
                        // so here we should only get one index object from this statement
                        index.setGlobal("GLOBAL".equalsIgnoreCase(result.getIndexes().get(0).getRange().name()));
                        index.setColumnGroups(result.getIndexes().get(0).getColumnGroups());
                    }
                    return indexDdl;
                });
            } catch (Exception ex) {
                DBSchemaAccessorUtil.fillWarning(index, index.type(),
                        "failed to call dbms_metadata.get_ddl to get index ddl, may index of the primary key");
                log.warn("failed to call dbms_metadata.get_ddl to get index ddl, schema={}, indexName={}",
                        index.getOwner(),
                        index.getName(), ex);
                index.setGlobal(true);
            }
        }
    }

    @Override
    protected RowMapper<DBTableColumn> listColumnsRowMapper() {
        final int[] hiddenColumnOrdinaryPosition = {-1};
        return (rs, romNum) -> {
            DBTableColumn tableColumn = new DBTableColumn();
            tableColumn.setSchemaName(rs.getString(OracleConstants.CONS_OWNER));
            tableColumn.setTableName(rs.getString(OracleConstants.COL_TABLE_NAME));
            tableColumn.setName(rs.getString(OracleConstants.COL_COLUMN_NAME));
            tableColumn
                    .setTypeName(DBSchemaAccessorUtil.normalizeTypeName(rs.getString(OracleConstants.COL_DATA_TYPE)));
            tableColumn.setFullTypeName(rs.getString(OracleConstants.COL_DATA_TYPE));
            tableColumn.setCharUsed(CharUnit.fromString(rs.getString(OracleConstants.COL_CHAR_USED)));
            tableColumn.setOrdinalPosition(rs.getInt(OracleConstants.COL_COLUMN_ID));
            tableColumn.setTypeModifiers(Arrays.asList(rs.getString(OracleConstants.COL_DATA_TYPE_MOD)));
            tableColumn.setMaxLength(
                    rs.getLong(tableColumn.getCharUsed() == CharUnit.CHAR ? OracleConstants.COL_CHAR_LENGTH
                            : OracleConstants.COL_DATA_LENGTH));
            tableColumn.setNullable("Y".equalsIgnoreCase(rs.getString(OracleConstants.COL_NULLABLE)));
            DBColumnTypeDisplay columnTypeDisplay = DBColumnTypeDisplay.fromName(tableColumn.getTypeName());
            if (columnTypeDisplay.displayScale()) {
                tableColumn.setScale(rs.getInt(OracleConstants.COL_DATA_SCALE));
            }
            if (columnTypeDisplay.displayPrecision()) {
                if (Objects.nonNull(rs.getObject(OracleConstants.COL_DATA_PRECISION))) {
                    tableColumn.setPrecision(rs.getLong(OracleConstants.COL_DATA_PRECISION));
                } else {
                    tableColumn.setPrecision(tableColumn.getMaxLength());
                }
            }
            if ("NUMBER".equalsIgnoreCase(tableColumn.getTypeName())) {
                if (Objects.isNull(rs.getObject(OracleConstants.COL_DATA_SCALE))) {
                    tableColumn.setScale(null);
                }
                if (Objects.isNull(rs.getObject(OracleConstants.COL_DATA_PRECISION))) {
                    tableColumn.setPrecision(null);
                }
            }
            if (!columnTypeDisplay.displayPrecision() && !columnTypeDisplay.displayScale()) {
                /**
                 * INTERVAL YEAR TO MONTH 类型在 Oracle 里 precision 是在 DATA_PRECISION 列的， 但是 OBOracle 是在 DATA_SCALE 列的
                 */
                if ("INTERVAL YEAR TO MONTH".equalsIgnoreCase(tableColumn.getTypeName())) {
                    tableColumn.setYearPrecision(rs.getInt(OracleConstants.COL_DATA_SCALE));
                } else if ("INTERVAL DAY TO SECOND".equalsIgnoreCase(tableColumn.getTypeName())) {
                    /**
                     * INTERVAL DAY TO SECOND 类型在 Oracle 里 day_precision 是在 DATA_PRECISION 列，seconds_precision 是在
                     * DATA_SCALE 列 但是 OBOracle 是把两个值拼起来放在 DATA_SCALE 列的 比如：INTERVAL DAY(2) TO SECOND(3)，OBOracle
                     * DATA_SCALE 的值就是 23
                     */
                    int packedScale = rs.getInt(OracleConstants.COL_DATA_SCALE);
                    tableColumn.setDayPrecision(packedScale / 10);
                    tableColumn.setSecondPrecision(packedScale % 10);
                } else if (tableColumn.getTypeName().startsWith("TIMESTAMP")) {
                    /**
                     * TIMESTAMP, TIMESTAMP WITH TIME ZONE, TIMESTAMP WITH LOCAL TIME 这几种类型的 precision 需要设置到
                     * secondPrecision 字段
                     */
                    tableColumn.setSecondPrecision(rs.getInt(OracleConstants.COL_DATA_SCALE));
                }
            }
            tableColumn.setHidden("YES".equalsIgnoreCase(rs.getString(OracleConstants.COL_HIDDEN_COLUMN)));
            /**
             * hidden column does not have ordinary position, we assign an negative position<br>
             * for front-end as a key to identify a column
             *
             */
            if (tableColumn.getHidden()) {
                tableColumn.setOrdinalPosition(hiddenColumnOrdinaryPosition[0]);
                hiddenColumnOrdinaryPosition[0]--;
            }
            tableColumn.setVirtual("YES".equalsIgnoreCase(rs.getString(OracleConstants.COL_VIRTUAL_COLUMN)));
            tableColumn.setDefaultValue("NULL".equals(rs.getString(OracleConstants.COL_DATA_DEFAULT)) ? null
                    : rs.getString(OracleConstants.COL_DATA_DEFAULT));
            if (tableColumn.getVirtual()) {
                tableColumn.setGenExpression(rs.getString(OracleConstants.COL_DATA_DEFAULT));
            }
            return tableColumn;
        };
    }

    @Override
    protected String getTableDDLOnly(String schemaName, String tableName) {
        OracleSqlBuilder sb = new OracleSqlBuilder();
        sb.append("SHOW CREATE TABLE ");
        sb.identifier(schemaName);
        sb.append(".");
        sb.identifier(tableName);

        AtomicReference<String> ddlRef = new AtomicReference<>();
        jdbcOperations.query(sb.toString(), t -> {
            // Create table ddl like this: CREATE [GLOBAL TEMPORARY|SHARDED|DUPLICATED] TABLE T...
            String ddl = t.getString(2);
            if (Objects.nonNull(ddl)) {
                // fix: Replace " TABLE " to " TABLE schemaName."
                ddlRef.set(StringUtils.replace(ddl, " TABLE ",
                        " TABLE " + StringUtils.quoteOracleIdentifier(schemaName) + ".", 1));
            }
        });
        return ddlRef.get();
    }

    @Override
    protected void obtainTableCharset(List<DBTableOptions> tableOptions) {
        String sql = "SHOW VARIABLES LIKE 'nls_characterset'";
        AtomicReference<String> charset = new AtomicReference<>();
        jdbcOperations.query(sql, t -> {
            charset.set(t.getString("VALUE"));
        });
        tableOptions.forEach(option -> {
            option.setCharsetName(charset.get());
        });
    }

    @Override
    protected void obtainTableCollation(List<DBTableOptions> tableOptions) {
        String sql = "SHOW VARIABLES LIKE 'nls_sort'";
        AtomicReference<String> collation = new AtomicReference<>();
        jdbcOperations.query(sql, t -> {
            collation.set(t.getString("VALUE"));
        });
        tableOptions.forEach(option -> {
            option.setCollationName(collation.get());
        });
    }

    @Override
    public DBFunction getFunction(String schemaName, String functionName) {
        OracleSqlBuilder sb = new OracleSqlBuilder();
        sb.append("select s.* , o.created, o.last_ddl_time, o.status from");
        sb.append(" (select * from ");
        sb.append(dataDictTableNames.OBJECTS());
        sb.append(" where object_type='FUNCTION') o right join ");
        sb.append(dataDictTableNames.SOURCE());
        sb.append(" s on s.name = o.object_name and s.owner = o.owner and s.type = o.object_type");
        sb.append(" where s.owner=");
        sb.value(schemaName);
        sb.append(" and s.name=");
        sb.value(functionName);
        sb.append(" and s.type = 'FUNCTION'");

        DBFunction function = new DBFunction();
        function.setFunName(functionName);

        jdbcOperations.query(sb.toString(), (rs) -> {
            function.setDefiner(rs.getString(1));
            function.setDdl(String.format("CREATE OR REPLACE %s;", rs.getClob(5).toString()));
            function.setStatus(rs.getString(9));
            function.setCreateTime(Timestamp.valueOf(rs.getString(7)));
            function.setModifyTime(Timestamp.valueOf(rs.getString(8)));
        });

        if (StringUtils.containsIgnoreCase(function.getStatus(), PLConstants.PL_OBJECT_STATUS_INVALID)) {
            function.setErrorMessage(PLObjectErrMsgUtils.getOraclePLObjErrMsg(jdbcOperations,
                    function.getDefiner(), DBObjectType.FUNCTION.name(), function.getFunName()));
        }

        return parseFunctionDDL(function);
    }

    @Override
    protected DBFunction parseFunctionDDL(DBFunction function) {
        try {
            ParseOraclePLResult result = PLParser.parseObOracle(function.getDdl());
            List<DBFunction> functionList = result.getFunctionList();
            if (functionList.size() > 0) {
                List<DBPLParam> params = functionList.get(0).getParams();
                for (DBPLParam param : params) {
                    param.setExtendedType(DataTypeUtil.isExtType(param.getDataType()));
                    param.setDefaultValue(StringUtils.unquoteSqlIdentifier(param.getDefaultValue(), '\''));
                }
                // TODO: figure out why just choose the first element
                function.setParams(params);
            }

            function.setVariables(result.getVaribaleList());
            function.setTypes(result.getTypeList());
            function.setReturnType(result.getReturnType());
            if (DataTypeUtil.isExtType(result.getReturnType())) {
                function.setReturnExtendedType(true);
            }
        } catch (Exception e) {
            log.warn("Failed to parse function ddl={}, errorMessage={}", function.getDdl(), e.getMessage());
            function.setParseErrorMessage(e.getMessage());
        }

        return function;
    }

    @Override
    public List<DBColumnGroupElement> listTableColumnGroups(String schemaName, String tableName) {
        return listTableColumnGroups(getTableDDLOnly(schemaName, tableName));
    }

    private List<DBColumnGroupElement> listTableColumnGroups(String ddl) {
        SQLParser sqlParser = new OBOracleSQLParser();
        CreateTable stmt = (CreateTable) sqlParser.parse(new StringReader(ddl));
        return stmt.getColumnGroupElements() == null ? Collections.emptyList()
                : stmt.getColumnGroupElements().stream()
                        .map(DBColumnGroupElement::ofColumnGroupElement).collect(Collectors.toList());
    }

    @Override
    public DBView getView(String schemaName, String viewName) {
        OracleSqlBuilder sb = new OracleSqlBuilder();
        sb.append("select * from ");
        sb.append(dataDictTableNames.VIEWS());
        sb.append(" where owner =");
        sb.value(schemaName);
        sb.append(" and view_name =");
        sb.value(viewName);

        DBView view = new DBView();
        view.setViewName(viewName);
        view.setSchemaName(schemaName);
        AtomicReference<String> partDdl = new AtomicReference<>();
        jdbcOperations.query(sb.toString(), (rs) -> {
            view.setDefiner(rs.getString("OWNER"));
            partDdl.set(rs.getClob("TEXT").toString());
        });
        fullFillComment(view);
        boolean updatable = fillOracleUpdatableInfo(view);
        String ddl = String.format("CREATE VIEW %s AS %s",
                StringUtils.quoteOracleIdentifier(viewName), partDdl);
        if (!updatable && !ddl.toUpperCase().contains("WITH READ ONLY")) {
            view.setDdl(ddl + " WITH READ ONLY");
        } else {
            view.setDdl(ddl);
        }
        return fillColumnInfoByDesc(view);
    }

    /**
     * 目前all_views视图中READ_ONLY字段固定为NULL，通过SYS用户下ALL_VIRTUAL_TABLE_REAL_AGENT表查询视图是否可更新
     */
    private boolean fillOracleUpdatableInfo(DBView view) {
        OracleSqlBuilder sb = new OracleSqlBuilder();
        sb.append("select VIEW_IS_UPDATABLE from SYS.ALL_VIRTUAL_TABLE_REAL_AGENT "
                + "where table_type = 4 and table_name =");
        sb.value(view.getViewName());
        sb.append(" and database_id = ");
        sb.append("(select database_id from SYS.ALL_VIRTUAL_DATABASE_REAL_AGENT where database_name = ");
        sb.value(view.getDefiner());
        sb.append(")");

        jdbcOperations.query(sb.toString(), (rs) -> {
            if (rs.getBigDecimal("VIEW_IS_UPDATABLE").intValue() == 0) {
                view.setCheckOption("READ_ONLY");
                view.setUpdatable(false);
            } else {
                view.setCheckOption("NONE");
                view.setUpdatable(true);
            }
        });
        return view.isUpdatable();
    }

    private DBView fillColumnInfoByDesc(DBView view) {
        Map<String, List<DBTableColumn>> name2Cols = new HashMap<>();
        try {
            List<DBTableColumn> columns = listBasicTableColumns(view.getSchemaName(), view.getViewName());
            if (CollectionUtils.isNotEmpty(columns)) {
                name2Cols = columns.stream().collect(Collectors.groupingBy(DBTableColumn::getName));
            }
        } catch (Exception e) {
            log.warn("Failed to query view column comments, viewName={}, errorMessage={}", view.getViewName(),
                    e.getMessage());
        }
        OracleSqlBuilder sb = new OracleSqlBuilder();
        sb.append("desc ");
        sb.identifier(view.getViewName());

        Map<String, List<DBTableColumn>> finalName2Cols = name2Cols;
        List<DBTableColumn> columns = jdbcOperations.query(sb.toString(), (rs, rowNum) -> {
            DBTableColumn column = new DBTableColumn();
            column.setName(rs.getString("FIELD"));
            column.setTypeName(rs.getString("TYPE"));
            column.setNullable("YES".equalsIgnoreCase(rs.getString("NULL")));
            column.setDefaultValue(rs.getString("DEFAULT"));
            column.setOrdinalPosition(rowNum);
            column.setTableName(view.getViewName());
            List<DBTableColumn> cols = finalName2Cols.get(column.getName());
            if (CollectionUtils.isNotEmpty(cols)) {
                column.setComment(cols.get(0).getComment());
            }
            return column;
        });
        view.setColumns(columns);
        return view;
    }

    @Override
    public DBProcedure getProcedure(String schemaName, String procedureName) {
        OracleSqlBuilder sb = new OracleSqlBuilder();
        sb.append("select s.* , o.created, o.last_ddl_time, o.status from  (select * from ");
        sb.append(dataDictTableNames.OBJECTS());
        sb.append(" where object_type='PROCEDURE') o right join ");
        sb.append(dataDictTableNames.SOURCE());
        sb.append(" s on s.name = o.object_name and s.owner = o.owner and s.type = o.object_type where s.owner=");
        sb.value(schemaName);
        sb.append(" and s.name=");
        sb.value(procedureName);
        sb.append(" and s.type = 'PROCEDURE'");

        DBProcedure procedure = new DBProcedure();
        procedure.setProName(procedureName);
        jdbcOperations.query(sb.toString(), (rs) -> {
            procedure.setDefiner(rs.getString("OWNER"));
            procedure.setDdl(String.format("create or replace %s;", rs.getClob("TEXT").toString()));
            procedure.setStatus(rs.getString("STATUS"));
            procedure.setCreateTime(rs.getTimestamp("CREATED"));
            procedure.setModifyTime(rs.getTimestamp("LAST_DDL_TIME"));
        });

        if (StringUtils.containsIgnoreCase(procedure.getStatus(), PLConstants.PL_OBJECT_STATUS_INVALID)) {
            procedure.setErrorMessage(PLObjectErrMsgUtils.getOraclePLObjErrMsg(jdbcOperations,
                    procedure.getDefiner(), DBObjectType.PROCEDURE.name(), procedure.getProName()));
        }

        return parseProcedureDDL(procedure);
    }

    @Override
    protected DBProcedure parseProcedureDDL(DBProcedure procedure) {
        Validate.notBlank(procedure.getDdl(), "procedure.ddl");
        String ddl = procedure.getDdl();
        ParseOraclePLResult result;
        try {
            result = PLParser.parseOracle(ddl);
        } catch (Exception e) {
            log.warn("Failed to parse, ddl={}, errorMessage={}",
                    ddl, e.getMessage());
            procedure.setParseErrorMessage(e.getMessage());
            return procedure;
        }

        List<DBProcedure> procedureList = result.getProcedureList();
        if (procedureList.size() > 0) {
            List<DBPLParam> params = procedureList.get(0).getParams();
            for (DBPLParam param : params) {
                param.setExtendedType(DataTypeUtil.isExtType(param.getDataType()));
                param.setDefaultValue(StringUtils.unquoteSqlIdentifier(param.getDefaultValue(), '\''));
            }
            // TODO: figure out why just choose the first element
            procedure.setParams(params);
        }

        procedure.setVariables(result.getVaribaleList());
        procedure.setTypes((result.getTypeList()));
        return procedure;
    }

    @Override
    public DBPackage getPackage(String schemaName, String packageName) {
        OracleSqlBuilder sb = new OracleSqlBuilder();
        sb.append("select s.* , o.created, o.last_ddl_time, o.status from (select * from ");
        sb.append(dataDictTableNames.OBJECTS());
        sb.append(" where object_type='PACKAGE' or object_type='PACKAGE BODY') o right join ");
        sb.append(dataDictTableNames.SOURCE());
        sb.append(" s on s.name = o.object_name and s.owner = o.owner and s.type = o.object_type where s.owner=");
        sb.value(schemaName);
        sb.append(" and s.name=");
        sb.value(packageName);

        DBPackage dbPackage = new DBPackage();
        dbPackage.setPackageName(packageName);
        jdbcOperations.query(sb.toString(), (rs) -> {
            try {
                dbPackage.setStatus(rs.getString("STATUS"));

                DBPackageDetail packageDetail = new DBPackageDetail();
                DBPackageBasicInfo basicInfo = new DBPackageBasicInfo();
                basicInfo.setDdl("create or replace " + rs.getClob("TEXT").toString());
                basicInfo.setDefiner(rs.getString("OWNER"));
                basicInfo.setCreateTime(rs.getTimestamp("CREATED"));
                basicInfo.setModifyTime(rs.getTimestamp("LAST_DDL_TIME"));
                packageDetail.setBasicInfo(basicInfo);

                // parse variables、 types、procedures、functions
                try {
                    ParseOraclePLResult oraclePLResult = PLParser.parseOracle(basicInfo.getDdl());

                    packageDetail.setVariables(oraclePLResult.getVaribaleList());
                    packageDetail.setTypes(oraclePLResult.getTypeList());

                    List<DBFunction> functionList = oraclePLResult.getFunctionList();
                    for (DBFunction function : functionList) {
                        List<DBPLParam> params = function.getParams();
                        for (DBPLParam dbPLParam : params) {
                            dbPLParam.setExtendedType(DataTypeUtil.isExtType(dbPLParam.getDataType()));
                        }
                        if (DataTypeUtil.isExtType(function.getReturnType())) {
                            function.setReturnExtendedType(true);
                        }
                    }
                    packageDetail.setFunctions(functionList);

                    List<DBProcedure> procedureList = oraclePLResult.getProcedureList();
                    for (DBProcedure procedure : procedureList) {
                        List<DBPLParam> params = procedure.getParams();
                        for (DBPLParam dbPLParam : params) {
                            dbPLParam.setExtendedType(DataTypeUtil.isExtType(dbPLParam.getDataType()));
                        }
                    }
                    packageDetail.setProcedures(procedureList);
                } catch (Exception e) {
                    log.warn("Failed to parse ddl={}, errorMessage={}", basicInfo.getDdl(), e.getMessage());
                    packageDetail.setParseErrorMessage(e.getMessage());
                }

                if (DBObjectType.PACKAGE.name().equalsIgnoreCase(rs.getString("TYPE"))) {
                    // 包头
                    dbPackage.setPackageHead(packageDetail);
                } else {
                    // 包体
                    // can not get correct procedure or function names from all_auguments
                    // TODO: get correct name after observer implementation
                    dbPackage.setPackageBody(packageDetail);
                }
            } catch (Exception e) {
                log.warn("Failed to parse, packageName={}, errorMessage={}",
                        dbPackage.getPackageName(), e.getMessage());
            }
        });

        if (StringUtils.containsIgnoreCase(dbPackage.getStatus(), PLConstants.PL_OBJECT_STATUS_INVALID)) {
            dbPackage.setErrorMessage(PLObjectErrMsgUtils.getOraclePLObjErrMsg(jdbcOperations,
                    schemaName, DBObjectType.PACKAGE.name(), dbPackage.getPackageName()));
        }
        return dbPackage;
    }

    @Override
    protected List<DBPLObjectIdentity> fillTypeErrorMessage(List<DBPLObjectIdentity> types, String schemaName) {
        Map<String, String> errorText = PLObjectErrMsgUtils.acquireErrorMessage(jdbcOperations,
                schemaName, DBObjectType.TYPE.name(), null);
        for (DBPLObjectIdentity type : types) {
            String errorMessage = errorText.get(type.getName());
            if (StringUtils.isNotBlank(errorMessage)) {
                // status value from all_objects for type will always return valid
                // we need to set status this way until observer fix this issue
                type.setStatus(PLConstants.PL_OBJECT_STATUS_INVALID);
                type.setErrorMessage(errorMessage);
            }
        }

        return types;
    }

    @Override
    public DBTrigger getTrigger(String schemaName, String triggerName) {
        OracleSqlBuilder sb = new OracleSqlBuilder();
        sb.append("select s.OWNER")
                .append(",s.TRIGGER_NAME")
                .append(",s.TRIGGER_TYPE")
                .append(",s.TRIGGERING_EVENT")
                .append(",s.TABLE_OWNER")
                .append(",s.BASE_OBJECT_TYPE")
                .append(",s.TABLE_NAME")
                .append(",s.TABLE_NAME")
                .append(",s.COLUMN_NAME")
                .append(",s.REFERENCING_NAMES")
                .append(",s.WHEN_CLAUSE")
                .append(",s.STATUS as ENABLE_STATUS")
                .append(",s.DESCRIPTION")
                .append(",s.ACTION_TYPE")
                .append(",s.TRIGGER_BODY")
                .append(",s.CROSSEDITION")
                .append(",s.BEFORE_STATEMENT")
                .append(",s.BEFORE_ROW")
                .append(",s.AFTER_ROW")
                .append(",s.AFTER_STATEMENT")
                .append(",s.INSTEAD_OF_ROW")
                .append(",s.FIRE_ONCE")
                .append(",s.APPLY_SERVER_ONLY")
                .append(",o.STATUS")
                .append(" FROM (SELECT * FROM ").append(dataDictTableNames.OBJECTS())
                .append(" WHERE OBJECT_TYPE='TRIGGER') o")
                .append(" RIGHT JOIN ").append(dataDictTableNames.TRIGGERS())
                .append(" s ON o.OBJECT_NAME=s.TRIGGER_NAME AND o.OWNER=s.OWNER")
                .append(" WHERE s.OWNER=").value(schemaName).append(" AND s.TRIGGER_NAME=").value(triggerName);
        Map<String, String> map = jdbcOperations.queryForObject(sb.toString(), (rs, rowNum) -> {
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            Map<String, String> map1 = new HashMap<>();
            for (int i = 0; i < columnCount; i++) {
                String columnLabel = metaData.getColumnLabel(i + 1);
                if (columnLabel == null) {
                    throw new IllegalStateException("Column lable is null");
                }
                map1.putIfAbsent(columnLabel.toUpperCase(), rs.getString(i + 1));
            }
            return map1;
        });
        if (map == null) {
            throw new IllegalStateException("Failed to query trigger's meta info");
        }
        DBTrigger trigger = new DBTrigger();
        trigger.setBaseObjectType(map.get("BASE_OBJECT_TYPE"));
        trigger.setTriggerName(map.get("TRIGGER_NAME"));
        trigger.setOwner(map.get("OWNER"));
        trigger.setSchemaMode(map.get("TABLE_OWNER"));
        trigger.setSchemaName(map.get("TABLE_NAME"));
        trigger.setEnable("ENABLED".equalsIgnoreCase(map.get("ENABLE_STATUS")));
        trigger.setStatus(map.get("STATUS"));

        Validate.notNull(trigger.getTriggerName(), "TriggerName can not be null");
        Validate.notNull(trigger.getBaseObjectType(), "BaseObjectType can not be null");
        Validate.notNull(trigger.getOwner(), "Owner can not be null");
        Validate.notNull(trigger.getSchemaName(), "TableName can not be null");
        Validate.notNull(trigger.getSchemaMode(), "TableOwner can not be null");
        Validate.notNull(trigger.getStatus(), "Status can not be null");
        /**
         * the standard operation of getting ddl is using 'select dbms_metadata.get_ddl('TRIGGER', '%s')
         * from dual;' but this function will drop comments which defined in trigger's header (eg. create or
         * replace trigger xxx -- this is a comment ...-> create or replace trigger xxx ...) this is an
         * issue(aone issue id 33865677) so that this way is forbidden
         */
        String triggerBody = map.get("TRIGGER_BODY");
        if (triggerBody != null) {
            if (StringUtils.startsWithIgnoreCase(triggerBody, "trigger")) {
                trigger.setDdl(String.format("CREATE OR REPLACE %s", triggerBody));
            } else {
                trigger.setDdl(fixDdlFromTrigger(trigger, triggerBody,
                        map.get("TRIGGERING_EVENT"), map.get("TRIGGER_TYPE"),
                        map.get("REFERENCING_NAMES"), map.get("WHEN_CLAUSE")));
            }
        }
        if (StringUtils.containsIgnoreCase(trigger.getStatus(), PLConstants.PL_OBJECT_STATUS_INVALID)) {
            trigger.setErrorMessage(PLObjectErrMsgUtils.getOraclePLObjErrMsg(jdbcOperations,
                    trigger.getOwner(), DBObjectType.TRIGGER.name(), trigger.getTriggerName()));
        }
        return trigger;
    }

    /**
     * 从xxx_triggers视图中查询到的schema信息反推出触发器DDL，这种方式需要注意，引用新值和引用旧值在22x以前的版本中不可用,
     * 22x以前的版本不将references相关的信息引入到DDL中
     *
     * @param trigger 从xxx_triggers视图中查询到的触发器schema信息
     * @return 返回拼成的DDL
     */
    protected String fixDdlFromTrigger(DBTrigger trigger,
            @NonNull String triggerBody, @NonNull String triggerEvent,
            @NonNull String triggerType, String referenceNames,
            String whenClause) {
        SqlBuilder sqlBuilder = new OracleSqlBuilder();
        sqlBuilder.append("CREATE OR REPLACE TRIGGER ")
                .identifier(trigger.getOwner()).append(".")
                .identifier(trigger.getTriggerName())
                .append(" ");
        String triggerMode = null;
        SqlBuilder triggerLevel = null;
        if (StringUtils.startsWithIgnoreCase(triggerType, "before")
                || StringUtils.startsWithIgnoreCase(triggerType, "after")) {
            triggerLevel = new OracleSqlBuilder();
            String[] tmpStrList = triggerType.split(" ");
            triggerMode = tmpStrList[0];
            for (int i = 1; i < tmpStrList.length; i++) {
                triggerLevel.append(tmpStrList[i]).append(" ");
            }
        }
        if (triggerMode != null) {
            sqlBuilder.append(triggerMode).append("\n\t");
        }
        sqlBuilder.append(triggerEvent).append(" ")
                .append("ON ")
                .identifier(trigger.getTableOwner()).append(".")
                .identifier(trigger.getTableName())
                .append("\n\t");
        if (containsTriggerReferences()) {
            sqlBuilder.append(referenceNames).append("\n\t");
        }
        if (triggerLevel != null) {
            sqlBuilder.append("FOR ").append(triggerLevel.toString()).append("\n\t");
        }
        String status = trigger.getEnableState().substring(0, trigger.getEnableState().length() - 1);
        sqlBuilder.append(status).append("\n");
        if (StringUtils.isNotBlank(whenClause)) {
            sqlBuilder.append("\tWHEN (")
                    .append(whenClause).append(")\n");
        }
        return sqlBuilder.append(triggerBody).toString();
    }

    protected boolean containsTriggerReferences() {
        return true;
    }

    @Override
    public DBType getType(String schemaName, String typeName) {
        OracleSqlBuilder sb = new OracleSqlBuilder();
        sb.append("select a.OWNER,a.OBJECT_NAME,u.TYPE_NAME,a.CREATED,a.LAST_DDL_TIME,u.TYPECODE,u.TYPEID from ");
        sb.append(dataDictTableNames.OBJECTS());
        sb.append(" a right join ");
        sb.append(dataDictTableNames.TYPES());
        sb.append(" u on a.OBJECT_NAME=u.TYPE_NAME where a.OWNER=");
        sb.value(schemaName);
        sb.append(" and u.TYPE_NAME=");
        sb.value(typeName);

        DBType type = new DBType();
        jdbcOperations.query(sb.toString(), (rs) -> {
            type.setOwner(rs.getString("OWNER"));
            type.setTypeName(rs.getString("TYPE_NAME"));
            type.setCreateTime(rs.getTimestamp("CREATED"));
            type.setLastDdlTime(rs.getTimestamp("LAST_DDL_TIME"));
            type.setTypeId(rs.getBigDecimal("TYPEID").toString());
            type.setType(rs.getString("TYPECODE"));
        });

        OracleSqlBuilder sb2 = new OracleSqlBuilder();
        sb2.append("select UPPER_BOUND from ALL_COLL_TYPES where TYPE_NAME=");
        sb2.value(typeName);

        Integer upperBound = jdbcOperations.query(sb2.toString(), rs -> {
            if (!rs.next()) {
                return null;
            }
            return Integer.parseInt(rs.getBigDecimal(1).toString());
        });
        if (upperBound != null) {
            if (upperBound > 0) {
                // upper bound's value > 0 means VARRAY
                type.setType(DBTypeCode.VARRAY.name());
            } else {
                // -1 means table
                type.setType(DBTypeCode.TABLE.name());
            }
        }

        return parseTypeDDL(type);
    }

    @Override
    protected String queryTypeDdl(String querySql) {
        return jdbcOperations.query(querySql, rs -> {
            if (!rs.next()) {
                return null;
            }
            return rs.getClob(1).toString();
        });
    }

    @Override
    protected String queryTypeSpecDdl(String querySql) {
        return jdbcOperations.query(querySql, rs -> {
            if (!rs.next()) {
                return null;
            }
            return rs.getClob(1).toString();
        });
    }

    @Override
    protected String getSequenceDDL(DBSequence sequence) {
        Validate.notNull(sequence, "sequence");
        Validate.notBlank(sequence.getName(), "sequence.name");

        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("CREATE SEQUENCE \"").append(StringUtils.escapeUseDouble(sequence.getName(), '"'))
                .append("\"");
        if (sequence.getMinValue() != null) {
            sqlBuilder.append(" MINVALUE ").append(sequence.getMinValue());
        } else {
            sqlBuilder.append(" NOMINVALUE");
        }
        if (sequence.getMaxValue() != null) {
            sqlBuilder.append(" MAXVALUE ").append(sequence.getMaxValue());
        } else {
            sqlBuilder.append(" NOMAXVALUE");
        }
        if (sequence.getStartValue() != null) {
            sqlBuilder.append(" START WITH ").append(sequence.getStartValue());
        }
        if (sequence.getIncreament() != null) {
            sqlBuilder.append(" INCREMENT BY ").append(sequence.getIncreament());
        }
        if (sequence.getCached() != null) {
            if (sequence.getCached() && sequence.getCacheSize() != null) {
                sqlBuilder.append(" CACHE ").append(sequence.getCacheSize());
            } else {
                sqlBuilder.append(" NOCACHE");
            }
        }
        if (sequence.getOrderd() != null) {
            if (sequence.getOrderd()) {
                sqlBuilder.append(" ORDER");
            } else {
                sqlBuilder.append(" NOORDER");
            }
        }
        if (sequence.getCycled() != null) {
            if (sequence.getCycled()) {
                sqlBuilder.append(" CYCLE");
            } else {
                sqlBuilder.append(" NOCYCLE");
            }
        }

        sqlBuilder.append(";");
        return sqlBuilder.toString();
    }

    @Override
    public List<DBObjectIdentity> listSynonyms(String schemaName, DBSynonymType synonymType) {
        OracleSqlBuilder sb = new OracleSqlBuilder();

        sb.append("select s.OWNER as schema_name, s.SYNONYM_NAME as name, o.OBJECT_TYPE as type from ")
                .append(dataDictTableNames.SYNONYMS())
                .append(" s left join (select * from ")
                .append(dataDictTableNames.OBJECTS())
                .append(" where OBJECT_TYPE='SYNONYM') o on s.SYNONYM_NAME=o.OBJECT_NAME and s.OWNER=o.OWNER where s.OWNER=")
                .value(getSynonymOwnerSymbol(synonymType, schemaName))
                .append(" order by name asc");

        return jdbcOperations.query(sb.toString(), new BeanPropertyRowMapper<>(DBObjectIdentity.class));
    }

    @Override
    protected String getSynonymDDL(DBSynonym synonym) {
        OracleSqlBuilder ddl = new OracleSqlBuilder();
        ddl.append("CREATE OR REPLACE ");
        if (synonym.getSynonymType() == DBSynonymType.PUBLIC) {
            ddl.append("PUBLIC ");
        }
        ddl.append("SYNONYM ")
                .identifier(synonym.getSynonymName())
                .append(" FOR ");
        if (StringUtils.isNotBlank(synonym.getTableOwner())) {
            ddl.identifier(synonym.getTableOwner())
                    .append(".");
        }
        ddl.identifier(StringUtils.isBlank(synonym.getDbLink()) ? synonym.getTableName()
                : synonym.getTableName() + "@" + synonym.getDbLink())
                .append(";");
        return ddl.toString();
    }

    @Override
    protected String getSynonymOwnerSymbol(DBSynonymType synonymType, String schemaName) {
        if (synonymType.equals(DBSynonymType.PUBLIC)) {
            return "__public";
        } else if (synonymType.equals(DBSynonymType.COMMON)) {
            return schemaName;
        } else {
            throw new UnsupportedOperationException("Not supported Synonym type");
        }
    }

    @Override
    public Map<String, DBTable> getTables(@NonNull String schemaName, List<String> tableNames) {
        // TODO: Only query the table information of tableNames passed upstream
        Map<String, DBTable> returnVal = new HashMap<>();
        tableNames = showTables(schemaName);
        if (tableNames.isEmpty()) {
            return returnVal;
        }
        Map<String, List<DBTableColumn>> tableName2Columns = listTableColumns(schemaName, Collections.emptyList());
        Map<String, List<DBTableIndex>> tableName2Indexes = listTableIndexes(schemaName);
        Map<String, List<DBTableConstraint>> tableName2Constraints = listTableConstraints(schemaName);
        Map<String, DBTableOptions> tableName2Options = listTableOptions(schemaName);
        for (String tableName : tableNames) {
            if (!tableName2Columns.containsKey(tableName)) {
                continue;
            }
            DBTable table = new DBTable();
            table.setSchemaName(schemaName);
            table.setOwner(schemaName);
            table.setName(tableName);
            List<DBTableColumn> columns = tableName2Columns.getOrDefault(tableName, new ArrayList<>());
            List<DBTableIndex> indexes = tableName2Indexes.getOrDefault(tableName, new ArrayList<>());
            table.setColumns(columns);
            table.setIndexes(indexes);
            table.setConstraints(tableName2Constraints.getOrDefault(tableName, new ArrayList<>()));
            table.setTableOptions(tableName2Options.getOrDefault(tableName, new DBTableOptions()));
            table.setPartition(getPartition(schemaName, tableName));
            table.setDDL(getTableDDL(schemaName, tableName, columns, indexes));
            table.setColumnGroups(listTableColumnGroups(getTableDDLOnly(schemaName, tableName)));
            returnVal.put(tableName, table);
        }
        return returnVal;
    }
}
